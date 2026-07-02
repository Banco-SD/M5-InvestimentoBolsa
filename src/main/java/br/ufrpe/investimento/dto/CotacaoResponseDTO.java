package br.ufrpe.investimento.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CotacaoResponseDTO {
    private String ticker;
    private String nome;
    private BigDecimal preco;
    private BigDecimal variacaoPercentual;
    private LocalDateTime atualizadoEm;
}
