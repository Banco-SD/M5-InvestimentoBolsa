package br.ufrpe.investimento.dto;

import br.ufrpe.investimento.model.TipoOrdem;
import br.ufrpe.investimento.model.TipoPreco;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload publicado na fila do RabbitMQ e consumido pelo Matching Engine.
 * Propositalmente pequeno (só o ID e o essencial) - o consumer sempre relê
 * a Ordem do banco antes de processar, pra pegar o estado mais atual e não
 * confiar em dados possivelmente desatualizados que ficaram na mensagem.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrdemMensagemDTO {
    private UUID ordemId;
    private UUID usuarioId;
    private String ticker;
    private TipoOrdem tipoOrdem;
    private TipoPreco tipoPreco;
    private Integer quantidade;
    private BigDecimal precoLimite;
}
