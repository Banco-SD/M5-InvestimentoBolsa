package br.ufrpe.investimento.request;

import br.ufrpe.investimento.model.TipoOrdem;
import br.ufrpe.investimento.model.TipoPreco;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CriarOrdemRequest {

    @NotBlank(message = "Ticker é obrigatório")
    private String ticker;

    @NotNull(message = "Tipo da ordem (COMPRA/VENDA) é obrigatório")
    private TipoOrdem tipoOrdem;

    @NotNull(message = "Tipo de preço (MERCADO/LIMITADA) é obrigatório")
    private TipoPreco tipoPreco;

    @NotNull(message = "Quantidade é obrigatória")
    @Positive(message = "Quantidade deve ser maior que zero")
    private Integer quantidade;

    /**
     * Obrigatório apenas quando tipoPreco = LIMITADA.
     * Validado manualmente no service (validação cruzada entre 2 campos
     * é mais simples ali do que com anotações do Bean Validation).
     */
    @Positive(message = "Preço-limite deve ser maior que zero")
    private BigDecimal precoLimite;
}
