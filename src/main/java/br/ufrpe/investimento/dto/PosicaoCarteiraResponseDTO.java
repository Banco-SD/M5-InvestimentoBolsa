package br.ufrpe.investimento.dto;

import br.ufrpe.investimento.model.PosicaoCarteira;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PosicaoCarteiraResponseDTO {

    private String ticker;
    private int quantidade;
    private int quantidadeReservada;
    private int quantidadeDisponivel;
    private BigDecimal precoMedio;
    private LocalDateTime dataAtualizacao;

    public static PosicaoCarteiraResponseDTO de(PosicaoCarteira posicao) {
        return new PosicaoCarteiraResponseDTO(
                posicao.getTicker(),
                posicao.getQuantidade(),
                posicao.getQuantidadeReservada(),
                posicao.getQuantidadeDisponivel(),
                posicao.getPrecoMedio(),
                posicao.getDataAtualizacao()
        );
    }
}
