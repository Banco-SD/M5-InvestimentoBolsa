package br.ufrpe.investimento.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ordem de compra ou venda de um ativo. É o objeto central deste módulo:
 * nasce RECEBIDA, passa por reserva de saldo/posição, é publicada na fila
 * do RabbitMQ e finalmente processada pelo Matching Engine (EXECUTADA,
 * REJEITADA ou FALHA_COMPENSADA).
 *
 * usuarioId NÃO tem @ManyToOne pra Usuario porque o usuário pertence a outro
 * módulo (Autenticação) - aqui guardamos só a referência (UUID), sem FK
 * entre bancos/módulos diferentes.
 */
@Getter
@Setter
@Entity
@Table(name = "ordem", indexes = {
        @Index(name = "idx_ordem_usuario", columnList = "usuarioId"),
        @Index(name = "idx_ordem_status", columnList = "status")
})
public class Ordem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull(message = "Usuário é obrigatório")
    @Column(nullable = false)
    private UUID usuarioId;

    @NotBlank(message = "Ticker é obrigatório")
    @Column(nullable = false, length = 20)
    private String ticker;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoOrdem tipoOrdem;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoPreco tipoPreco;

    @NotNull(message = "Quantidade é obrigatória")
    @Positive(message = "Quantidade deve ser maior que zero")
    @Column(nullable = false)
    private Integer quantidade;

    /** Preço-limite informado pelo usuário. Obrigatório apenas se tipoPreco = LIMITADA. */
    @Column(precision = 19, scale = 4)
    private BigDecimal precoLimite;

    /** Preço pelo qual a ordem foi de fato executada (preenchido pelo Matching Engine). */
    @Column(precision = 19, scale = 4)
    private BigDecimal precoExecucao;

    /** Valor estimado (compra) reservado no Wallet Service, para eventual compensação. */
    @Column(precision = 19, scale = 4)
    private BigDecimal valorReservado;

    /** ID da reserva de saldo no Wallet Service (só para ordens de COMPRA). */
    private UUID idReservaSaldo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusOrdem status = StatusOrdem.RECEBIDA;

    /** Motivo legível de rejeição/falha (ex.: "Saldo insuficiente", "Preço-limite não atingido"). */
    @Column(length = 255)
    private String motivo;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime dataAtualizacao;

    // Lock otimista: essencial aqui, já que a mesma ordem é lida e escrita
    // tanto pela requisição HTTP quanto pelo consumer do RabbitMQ.
    @Version
    private Long versao;
}
