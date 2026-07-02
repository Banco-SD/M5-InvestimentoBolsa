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
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Motor de casamento de ordens SIMPLIFICADO: não mantém livro de ofertas
 * (order book) nem casa ordens de compra/venda de usuários diferentes entre
 * si. Em vez disso, executa a ordem diretamente contra o preço atual de
 * mercado (obtido via CotacaoService/brapi.dev) - o que é razoável para o
 * escopo do projeto, já que o "vendedor" real é o mercado como um todo.
 *
 * Ponto central do desafio de Sistemas Distribuídos: se a execução falhar
 * por qualquer motivo (preço-limite não atingido, erro de comunicação com
 * o Wallet Service, exceção inesperada), este consumer SEMPRE compensa a
 * reserva feita anteriormente pelo OrdemService, para o dinheiro/posição
 * nunca ficar "preso" no limbo.
 */
@Component
public class MatchingEngineConsumer {

    private static final Logger log = LoggerFactory.getLogger(MatchingEngineConsumer.class);

    private final OrdemRepository ordemRepository;
    private final CotacaoService cotacaoService;
    private final WalletClient walletClient;
    private final CarteiraService carteiraService;
    private final OrdemService ordemService;

    public MatchingEngineConsumer(
            OrdemRepository ordemRepository,
            CotacaoService cotacaoService,
            WalletClient walletClient,
            CarteiraService carteiraService,
            OrdemService ordemService
    ) {
        this.ordemRepository = ordemRepository;
        this.cotacaoService = cotacaoService;
        this.walletClient = walletClient;
        this.carteiraService = carteiraService;
        this.ordemService = ordemService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDENS)
    @Transactional
    public void processarOrdem(OrdemMensagemDTO mensagem) {
        Optional<Ordem> ordemOpt = ordemRepository.findById(mensagem.getOrdemId());
        if (ordemOpt.isEmpty()) {
            log.warn("Ordem {} não encontrada - mensagem descartada", mensagem.getOrdemId());
            return;
        }

        Ordem ordem = ordemOpt.get();

        // Idempotência: se por algum motivo a mensagem for reprocessada
        // (redelivery do RabbitMQ) e a ordem já não estiver mais aguardando
        // processamento, não faz nada de novo.
        if (ordem.getStatus() != StatusOrdem.ENVIADA_PARA_PROCESSAMENTO) {
            log.info("Ordem {} já está em status {} - ignorando reprocessamento", ordem.getId(), ordem.getStatus());
            return;
        }

        try {
            CotacaoResponseDTO cotacao = cotacaoService.buscarCotacao(ordem.getTicker());
            BigDecimal precoMercado = cotacao.getPreco();

            if (!precoAtendeCondicao(ordem, precoMercado)) {
                compensarEFinalizar(ordem, "Preço-limite não atingido no momento do processamento (mercado: "
                        + precoMercado + ", limite: " + ordem.getPrecoLimite() + ")");
                return;
            }

            executarOrdem(ordem, precoMercado);

        } catch (Exception ex) {
            log.error("Falha ao processar ordem {}: {}", ordem.getId(), ex.getMessage(), ex);
            compensarEFinalizar(ordem, "Falha inesperada ao processar a ordem: " + ex.getMessage());
        }
    }

    private boolean precoAtendeCondicao(Ordem ordem, BigDecimal precoMercado) {
        if (ordem.getTipoPreco() == TipoPreco.MERCADO) {
            return true;
        }
        // LIMITADA: compra só executa se o mercado está <= limite; venda só se >= limite
        return ordem.getTipoOrdem() == TipoOrdem.COMPRA
                ? precoMercado.compareTo(ordem.getPrecoLimite()) <= 0
                : precoMercado.compareTo(ordem.getPrecoLimite()) >= 0;
    }

    private void executarOrdem(Ordem ordem, BigDecimal precoExecucao) {
        if (ordem.getTipoOrdem() == TipoOrdem.COMPRA) {
            walletClient.confirmar(ordem.getIdReservaSaldo());
            carteiraService.registrarCompra(ordem.getUsuarioId(), ordem.getTicker(), ordem.getQuantidade(), precoExecucao);
        } else {
            carteiraService.confirmarVenda(ordem.getUsuarioId(), ordem.getTicker(), ordem.getQuantidade());
            BigDecimal valorVenda = precoExecucao.multiply(BigDecimal.valueOf(ordem.getQuantidade()));
            walletClient.creditar(ordem.getUsuarioId(), valorVenda, ordem.getId());
        }

        ordem.setPrecoExecucao(precoExecucao);
        ordem.setStatus(StatusOrdem.EXECUTADA);
        ordemRepository.save(ordem);
        log.info("Ordem {} executada com sucesso ao preço {}", ordem.getId(), precoExecucao);
    }

    /** *** COMPENSAÇÃO ASSÍNCRONA *** - devolve saldo/posição e marca a ordem como falha. */
    private void compensarEFinalizar(Ordem ordem, String motivo) {
        ordemService.compensarReserva(ordem);
        ordem.setStatus(StatusOrdem.FALHA_COMPENSADA);
        ordem.setMotivo(motivo);
        ordemRepository.save(ordem);
        log.info("Ordem {} compensada: {}", ordem.getId(), motivo);
    }
}
