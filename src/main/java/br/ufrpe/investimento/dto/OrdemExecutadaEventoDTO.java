package br.ufrpe.investimento.dto;

import br.ufrpe.investimento.model.TipoOrdem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento publicado na fila "fila.ordem.executada" toda vez que uma ordem é
 * EXECUTADA com sucesso. Consumido por outros módulos (ex.: extrato/evolução
 * patrimonial), que NÃO devem precisar fazer um GET de volta pra este módulo
 * pra completar a informação - por isso o payload já vem com tudo que uma
 * série histórica (Pandas, Redis, etc.) normalmente precisa:
 *
 * - usuarioId: de quem é a ordem (dono do extrato a atualizar)
 * - dataExecucao: timestamp exato da execução, em ISO-8601
 *   (ex.: "2026-07-02T10:30:00") - LocalDateTime já serializa assim por
 *   padrão com o Jackson2JsonMessageConverter/JavaTimeModule
 * - precoExecucao: o preço REAL de execução (nunca o precoLimite, que fica
 *   nulo em ordens a MERCADO e é só um teto/piso em ordens LIMITADA - o que
 *   o consumidor quer pra custo histórico é sempre precoExecucao)
 * - valorTotal: quantidade * precoExecucao, já calculado, pra não obrigar
 *   quem consome a refazer essa conta
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrdemExecutadaEventoDTO {
    private UUID ordemId;
    private UUID usuarioId;
    private String ticker;
    private TipoOrdem tipoOrdem;
    private Integer quantidade;
    private BigDecimal precoExecucao;
    private BigDecimal valorTotal;
    private LocalDateTime dataExecucao;
}