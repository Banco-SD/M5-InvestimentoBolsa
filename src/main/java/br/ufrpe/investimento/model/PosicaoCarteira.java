package br.ufrpe.investimento.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Posição consolidada de um usuário em um ativo (quanto ele possui).
 *
 * quantidade = total que o usuário possui.
 * quantidadeReservada = parte já "comprometida" com ordens de venda pendentes,
 * ainda não executadas. quantidadeDisponivel (calculado, não persistido) é
 * quantidade - quantidadeReservada, e é isso que deve ser checado antes de
 * aceitar uma nova ordem de venda (evita vender a mesma ação duas vezes
 * enquanto a primeira venda ainda está na fila).
 */
@Getter
@Setter
@Entity
@Table(name = "posicao_carteira", uniqueConstraints = {
        @UniqueConstraint(name = "uk_posicao_usuario_ativo", columnNames = {"usuarioId", "ticker"})
})
public class PosicaoCarteira {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(nullable = false)
    private UUID usuarioId;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String ticker;

    @NotNull
    @Column(nullable = false)
    private Integer quantidade = 0;

    @NotNull
    @Column(nullable = false)
    private Integer quantidadeReservada = 0;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal precoMedio = BigDecimal.ZERO;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime dataAtualizacao;

    @Version
    private Long versao;

    @Transient
    public int getQuantidadeDisponivel() {
        return quantidade - quantidadeReservada;
    }
}
