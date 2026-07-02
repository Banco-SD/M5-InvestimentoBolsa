package br.ufrpe.investimento.service;

import br.ufrpe.investimento.dto.PosicaoCarteiraResponseDTO;
import br.ufrpe.investimento.exception.PosicaoInsuficienteException;
import br.ufrpe.investimento.model.PosicaoCarteira;
import br.ufrpe.investimento.repository.PosicaoCarteiraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class CarteiraService {

    private final PosicaoCarteiraRepository posicaoCarteiraRepository;

    public CarteiraService(PosicaoCarteiraRepository posicaoCarteiraRepository) {
        this.posicaoCarteiraRepository = posicaoCarteiraRepository;
    }

    public List<PosicaoCarteiraResponseDTO> listarPosicoes(UUID usuarioId) {
        return posicaoCarteiraRepository.findByUsuarioId(usuarioId).stream()
                .map(PosicaoCarteiraResponseDTO::de)
                .toList();
    }

    /**
     * Reserva (bloqueia) uma quantidade da posição para uma ordem de venda
     * pendente. Usa lock pessimista pra evitar que duas vendas simultâneas
     * do mesmo ativo "vejam" saldo disponível que na verdade já foi
     * comprometido uma pela outra.
     */
    @Transactional
    public void reservarParaVenda(UUID usuarioId, String ticker, int quantidade) {
        PosicaoCarteira posicao = posicaoCarteiraRepository.buscarComLock(usuarioId, ticker)
                .orElseThrow(() -> new PosicaoInsuficienteException(
                        "Você não possui nenhuma posição em " + ticker));

        if (posicao.getQuantidadeDisponivel() < quantidade) {
            throw new PosicaoInsuficienteException(
                    "Quantidade disponível de " + ticker + " (" + posicao.getQuantidadeDisponivel()
                            + ") é menor que a quantidade da ordem (" + quantidade + ")");
        }

        posicao.setQuantidadeReservada(posicao.getQuantidadeReservada() + quantidade);
        posicaoCarteiraRepository.save(posicao);
    }

    /** Compensação: devolve uma quantidade reservada para venda que não pôde ser concluída. */
    @Transactional
    public void liberarReservaDeVenda(UUID usuarioId, String ticker, int quantidade) {
        posicaoCarteiraRepository.buscarComLock(usuarioId, ticker).ifPresent(posicao -> {
            posicao.setQuantidadeReservada(Math.max(0, posicao.getQuantidadeReservada() - quantidade));
            posicaoCarteiraRepository.save(posicao);
        });
    }

    /** Venda executada: remove definitivamente a quantidade (total e reservada). */
    @Transactional
    public void confirmarVenda(UUID usuarioId, String ticker, int quantidade) {
        PosicaoCarteira posicao = posicaoCarteiraRepository.buscarComLock(usuarioId, ticker)
                .orElseThrow(() -> new PosicaoInsuficienteException("Posição em " + ticker + " não encontrada"));

        posicao.setQuantidade(posicao.getQuantidade() - quantidade);
        posicao.setQuantidadeReservada(Math.max(0, posicao.getQuantidadeReservada() - quantidade));
        posicaoCarteiraRepository.save(posicao);
    }

    /** Compra executada: soma a quantidade e recalcula o preço médio (média ponderada). */
    @Transactional
    public void registrarCompra(UUID usuarioId, String ticker, int quantidade, BigDecimal precoExecucao) {
        PosicaoCarteira posicao = posicaoCarteiraRepository.buscarComLock(usuarioId, ticker).orElseGet(() -> {
            PosicaoCarteira nova = new PosicaoCarteira();
            nova.setUsuarioId(usuarioId);
            nova.setTicker(ticker);
            return nova;
        });

        BigDecimal valorAntigo = posicao.getPrecoMedio().multiply(BigDecimal.valueOf(posicao.getQuantidade()));
        BigDecimal valorNovo = precoExecucao.multiply(BigDecimal.valueOf(quantidade));
        int quantidadeTotal = posicao.getQuantidade() + quantidade;

        BigDecimal precoMedioAtualizado = quantidadeTotal == 0
                ? BigDecimal.ZERO
                : valorAntigo.add(valorNovo).divide(BigDecimal.valueOf(quantidadeTotal), 4, java.math.RoundingMode.HALF_UP);

        posicao.setQuantidade(quantidadeTotal);
        posicao.setPrecoMedio(precoMedioAtualizado);
        posicaoCarteiraRepository.save(posicao);
    }
}
