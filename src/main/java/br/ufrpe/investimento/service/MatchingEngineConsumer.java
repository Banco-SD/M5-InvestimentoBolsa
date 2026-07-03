package br.ufrpe.investimento.service;

import br.ufrpe.investimento.config.RabbitMQConfig;
import br.ufrpe.investimento.dto.CotacaoResponseDTO;
import br.ufrpe.investimento.dto.OrdemMensagemDTO;
import br.ufrpe.investimento.model.Ordem;
import br.ufrpe.investimento.model.StatusOrdem;
import br.ufrpe.investimento.model.TipoOrdem;
import br.ufrpe.investimento.model.TipoPreco;
import br.ufrpe.investimento.repository.OrdemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Consome a QUEUE_ORDENS e executa o matching contra a cotação atual.
 * Idempotente: se a ordem já não estiver mais ENVIADA_PARA_PROCESSAMENTO
 * (ex.: mensagem duplicada, ou já cancelada), ignora.
 */
@Service
public class MatchingEngineConsumer {

    private static final Logger log = LoggerFactory.getLogger(MatchingEngineConsumer.class);

    private final OrdemRepository ordemRepository;
    private final CotacaoService cotacaoService;
    private final OrdemService ordemService;
    private final OrdemExecutadaEventoPublisher ordemExecutadaEventoPublisher;

    public MatchingEngineConsumer(
            OrdemRepository ordemRepository,
            CotacaoService cotacaoService,
            OrdemService ordemService,
            OrdemExecutadaEventoPublisher ordemExecutadaEventoPublisher
    ) {
        this.ordemRepository = ordemRepository;
        this.cotacaoService = cotacaoService;
        this.ordemService = ordemService;
        this.ordemExecutadaEventoPublisher = ordemExecutadaEventoPublisher;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDENS)
    @Transactional
    public void processar(OrdemMensagemDTO mensagem) {
        Ordem ordem = ordemRepository.findById(mensagem.getOrdemId())
                .orElseThrow(() -> {
                    log.error("Ordem {} não encontrada - descartando mensagem (sem reserva pra compensar)", mensagem.getOrdemId());
                    return new AmqpRejectAndDontRequeueException("Ordem não encontrada: " + mensagem.getOrdemId());
                });

        if (ordem.getStatus() != StatusOrdem.ENVIADA_PARA_PROCESSAMENTO) {
            log.warn("Ordem {} com status {} - ignorando (mensagem duplicada ou já processada)",
                    ordem.getId(), ordem.getStatus());
            return;
        }

        CotacaoResponseDTO cotacao = cotacaoService.buscarCotacao(ordem.getTicker());
        BigDecimal precoExecucao = determinarPrecoExecucao(ordem, cotacao);

        if (precoExecucao == null) {
            ordemService.compensarReserva(ordem);
            ordem.setStatus(StatusOrdem.FALHA_COMPENSADA);
            ordem.setMotivo("Preço-limite não atingido; reserva devolvida automaticamente");
            ordemRepository.save(ordem);
            return;
        }

        ordem.setPrecoExecucao(precoExecucao);
        ordem.setStatus(StatusOrdem.EXECUTADA);
        ordemRepository.save(ordem);

        ordemExecutadaEventoPublisher.publicarExecucao(ordem);
    }

    private BigDecimal determinarPrecoExecucao(Ordem ordem, CotacaoResponseDTO cotacao) {
        BigDecimal precoMercado = cotacao.getPreco();

        if (ordem.getTipoPreco() == TipoPreco.MERCADO) {
            return precoMercado;
        }

        BigDecimal precoLimite = ordem.getPrecoLimite();
        boolean atingiu = ordem.getTipoOrdem() == TipoOrdem.COMPRA
                ? precoMercado.compareTo(precoLimite) <= 0   // só compra se mercado <= limite
                : precoMercado.compareTo(precoLimite) >= 0;  // só vende se mercado >= limite

        return atingiu ? precoMercado : null;
    }
}