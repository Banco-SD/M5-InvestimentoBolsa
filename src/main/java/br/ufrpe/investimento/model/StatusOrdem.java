package br.ufrpe.investimento.model;

public enum StatusOrdem {
    /** Ordem acabou de ser criada, ainda não passou pela validação de saldo/posição */
    RECEBIDA,
    /** Saldo (compra) ou posição (venda) foi reservado com sucesso */
    RESERVA_CONFIRMADA,
    /** Publicada com sucesso na fila do RabbitMQ, aguardando o Matching Engine */
    ENVIADA_PARA_PROCESSAMENTO,
    /** Executada com sucesso pelo Matching Engine */
    EXECUTADA,
    /** Rejeitada antes de chegar na fila (ex.: saldo/posição insuficiente) */
    REJEITADA,
    /**
     * Reserva foi feita, mas algo falhou depois (fila fora do ar, erro no
     * matching engine, preço-limite não atingido, etc.) e a compensação
     * (devolução do saldo/posição reservada) já foi executada.
     */
    FALHA_COMPENSADA,
    /** Cancelada pelo usuário antes de ser processada */
    CANCELADA
}
