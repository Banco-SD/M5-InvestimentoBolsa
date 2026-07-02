package br.ufrpe.investimento.service;

import br.ufrpe.investimento.dto.CotacaoResponseDTO;
import br.ufrpe.investimento.dto.OrdemResponseDTO;
import br.ufrpe.investimento.exception.OrdemInvalidaException;
import br.ufrpe.investimento.exception.OrdemNaoEncontradaException;
import br.ufrpe.investimento.exception.PosicaoInsuficienteException;
import br.ufrpe.investimento.model.Ordem;
import br.ufrpe.investimento.model.StatusOrdem;
import br.ufrpe.investimento.model.TipoOrdem;
import br.ufrpe.investimento.model.TipoPreco;
import br.ufrpe.investimento.repository.OrdemRepository;
import br.ufrpe.investimento.request.CriarOrdemRequest;
import org.springframework.amqp.AmqpException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Orquestra o fluxo (SAGA) de criação de uma ordem:
 *
 * 1. Valida o ativo/cotação.
 * 2. COMPRA -> reserva saldo no Wallet Service. VENDA -> reserva a
 *    quantidade na posição local (CarteiraService).
 * 3. Publica a ordem na fila do RabbitMQ pro Matching Engine processar.
 * 4. Se o passo 3 falhar (broker fora do ar), COMPENSA imediatamente o
 *    passo 2 (libera o saldo/posição reservada) - é exatamente o cenário
 *    "se a compra falhar no meio do caminho" do desafio.
 *
 * A parte "assíncrona" da saga (o que acontece quando o Matching Engine
 * processa a ordem, minutos ou segundos depois) fica no MatchingEngineConsumer.
 */
@Service
public class OrdemService {

    private final OrdemRepository ordemRepository;
    private final CotacaoService cotacaoService;
    private final WalletClient walletClient;
    private final CarteiraService carteiraService;
    private final OrdemPublisherService ordemPublisherService;

    public OrdemService(
            OrdemRepository ordemRepository,
            CotacaoService cotacaoService,
            WalletClient walletClient,
            CarteiraService carteiraService,
            OrdemPublisherService ordemPublisherService
    ) {
        this.ordemRepository = ordemRepository;
        this.cotacaoService = cotacaoService;
        this.walletClient = walletClient;
        this.carteiraService = carteiraService;
        this.ordemPublisherService = ordemPublisherService;
    }

    @Transactional
    public OrdemResponseDTO criarOrdem(UUID usuarioId, CriarOrdemRequest request) {
        validarRequest(request);

        String ticker = request.getTicker().trim().toUpperCase();
        CotacaoResponseDTO cotacao = cotacaoService.buscarCotacao(ticker);

        Ordem ordem = new Ordem();
        ordem.setUsuarioId(usuarioId);
        ordem.setTicker(ticker);
        ordem.setTipoOrdem(request.getTipoOrdem());
        ordem.setTipoPreco(request.getTipoPreco());
        ordem.setQuantidade(request.getQuantidade());
        ordem.setPrecoLimite(request.getPrecoLimite());
        ordem.setStatus(StatusOrdem.RECEBIDA);
        ordem = ordemRepository.save(ordem);

        if (request.getTipoOrdem() == TipoOrdem.COMPRA) {
            processarReservaCompra(ordem, cotacao);
        } else {
            processarReservaVenda(ordem);
        }

        // Se a reserva foi rejeitada, a ordem já está REJEITADA - não publica na fila.
        if (ordem.getStatus() == StatusOrdem.REJEITADA) {
            return OrdemResponseDTO.de(ordem);
        }

        try {
            ordemPublisherService.publicar(ordem);
            ordem.setStatus(StatusOrdem.ENVIADA_PARA_PROCESSAMENTO);
        } catch (AmqpException ex) {
            // *** COMPENSAÇÃO ***: a fila falhou depois que já reservamos saldo/posição.
            compensarReserva(ordem);
            ordem.setStatus(StatusOrdem.FALHA_COMPENSADA);
            ordem.setMotivo("Falha ao enviar ordem para processamento; reserva devolvida automaticamente");
        }

        return OrdemResponseDTO.de(ordemRepository.save(ordem));
    }

    @Transactional
    public void cancelarOrdem(UUID usuarioId, UUID ordemId) {
        Ordem ordem = buscarEntidadeDoUsuario(usuarioId, ordemId);

        if (ordem.getStatus() != StatusOrdem.RECEBIDA && ordem.getStatus() != StatusOrdem.RESERVA_CONFIRMADA) {
            throw new OrdemInvalidaException("Ordem não pode mais ser cancelada (status atual: " + ordem.getStatus() + ")");
        }

        compensarReserva(ordem);
        ordem.setStatus(StatusOrdem.CANCELADA);
        ordem.setMotivo("Cancelada pelo usuário");
        ordemRepository.save(ordem);
    }

    public OrdemResponseDTO buscarPorId(UUID usuarioId, UUID ordemId) {
        return OrdemResponseDTO.de(buscarEntidadeDoUsuario(usuarioId, ordemId));
    }

    public List<OrdemResponseDTO> listarPorUsuario(UUID usuarioId) {
        return ordemRepository.findByUsuarioIdOrderByDataCriacaoDesc(usuarioId).stream()
                .map(OrdemResponseDTO::de)
                .toList();
    }

    private void processarReservaCompra(Ordem ordem, CotacaoResponseDTO cotacao) {
        BigDecimal precoEstimado = ordem.getTipoPreco() == TipoPreco.LIMITADA ? ordem.getPrecoLimite() : cotacao.getPreco();
        BigDecimal valorEstimado = precoEstimado.multiply(BigDecimal.valueOf(ordem.getQuantidade()));

        WalletClient.ReservaSaldo reserva = walletClient.reservar(ordem.getUsuarioId(), valorEstimado, ordem.getId());

        if (!reserva.sucesso()) {
            ordem.setStatus(StatusOrdem.REJEITADA);
            ordem.setMotivo(reserva.motivoFalha() != null ? reserva.motivoFalha() : "Saldo insuficiente");
            return;
        }

        ordem.setIdReservaSaldo(reserva.idReserva());
        ordem.setValorReservado(valorEstimado);
        ordem.setStatus(StatusOrdem.RESERVA_CONFIRMADA);
    }

    private void processarReservaVenda(Ordem ordem) {
        try {
            carteiraService.reservarParaVenda(ordem.getUsuarioId(), ordem.getTicker(), ordem.getQuantidade());
            ordem.setStatus(StatusOrdem.RESERVA_CONFIRMADA);
        } catch (PosicaoInsuficienteException ex) {
            ordem.setStatus(StatusOrdem.REJEITADA);
            ordem.setMotivo(ex.getMessage());
        }
    }

    /** Compensação genérica: desfaz a reserva de saldo (compra) ou posição (venda) desta ordem. */
    void compensarReserva(Ordem ordem) {
        if (ordem.getTipoOrdem() == TipoOrdem.COMPRA && ordem.getIdReservaSaldo() != null) {
            walletClient.liberar(ordem.getIdReservaSaldo());
        } else if (ordem.getTipoOrdem() == TipoOrdem.VENDA) {
            carteiraService.liberarReservaDeVenda(ordem.getUsuarioId(), ordem.getTicker(), ordem.getQuantidade());
        }
    }

    private void validarRequest(CriarOrdemRequest request) {
        if (request.getTipoPreco() == TipoPreco.LIMITADA && request.getPrecoLimite() == null) {
            throw new OrdemInvalidaException("Preço-limite é obrigatório para ordens do tipo LIMITADA");
        }
    }

    Ordem buscarEntidadeDoUsuario(UUID usuarioId, UUID ordemId) {
        Ordem ordem = ordemRepository.findById(ordemId)
                .orElseThrow(() -> new OrdemNaoEncontradaException("Ordem não encontrada"));

        if (!ordem.getUsuarioId().equals(usuarioId)) {
            throw new OrdemNaoEncontradaException("Ordem não encontrada");
        }
        return ordem;
    }
}
