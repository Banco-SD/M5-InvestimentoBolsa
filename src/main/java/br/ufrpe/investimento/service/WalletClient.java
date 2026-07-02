package br.ufrpe.investimento.service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Contrato de comunicação com o Wallet Service (módulo de saldo/conta
 * corrente), que é EXTERNO a este módulo.
 *
 * IMPORTANTE: a URL base e os endpoints abaixo (POST /carteira/reservar,
 * /carteira/confirmar/{id}, /carteira/liberar/{id}, /carteira/creditar) são
 * uma proposta de contrato REST - alinhe com quem estiver construindo o
 * Wallet Service e ajuste o WalletClientHttp se os endpoints reais forem
 * diferentes.
 */
public interface WalletClient {

    /**
     * Tenta reservar (bloquear) um valor do saldo do usuário para uma ordem
     * de compra. Retorna o ID da reserva se bem-sucedido.
     */
    ReservaSaldo reservar(UUID usuarioId, BigDecimal valor, UUID referenciaOrdemId);

    /**
     * Efetiva definitivamente uma reserva (debita o saldo de fato), chamado
     * quando a ordem de compra é executada com sucesso.
     */
    void confirmar(UUID idReserva);

    /**
     * Compensação: libera uma reserva sem debitar nada, devolvendo o saldo
     * "reservado" para "disponível". Chamado quando algo falha depois da
     * reserva (fila fora do ar, ordem rejeitada pelo matching engine, etc.).
     */
    void liberar(UUID idReserva);

    /**
     * Credita o valor de uma venda executada na conta do usuário.
     */
    void creditar(UUID usuarioId, BigDecimal valor, UUID referenciaOrdemId);

    record ReservaSaldo(boolean sucesso, UUID idReserva, String motivoFalha) {
    }
}
