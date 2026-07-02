package br.ufrpe.investimento.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa um ativo negociável na B3 (ação, FII, ETF, BDR...).
 * O ticker é a própria chave primária - não faz sentido gerar UUID pra algo
 * que já tem um identificador natural único (ex.: "PETR4").
 *
 * ultimoPrecoConhecido funciona como cache local da cotação externa (brapi.dev),
 * evitando bater na API a cada requisição e nos protegendo se ela ficar fora do ar.
 */
@Getter
@Setter
@Entity
@Table(name = "ativo")
public class Ativo {

    @Id
    @NotBlank(message = "Ticker é obrigatório")
    @Column(length = 20)
    private String ticker;

    @NotBlank(message = "Nome é obrigatório")
    @Column(nullable = false, length = 150)
    private String nome;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal ultimoPrecoConhecido = BigDecimal.ZERO;

    private LocalDateTime dataAtualizacaoPreco;

    @Column(nullable = false)
    private boolean negociavel = true;

    @Version
    private Long versao;
}
