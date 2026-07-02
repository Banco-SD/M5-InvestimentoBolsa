package br.ufrpe.investimento.dto;

import br.ufrpe.investimento.model.Ordem;
import br.ufrpe.investimento.model.StatusOrdem;
import br.ufrpe.investimento.model.TipoOrdem;
import br.ufrpe.investimento.model.TipoPreco;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class OrdemResponseDTO {

    private UUID id;
    private String ticker;
    private TipoOrdem tipoOrdem;
    private TipoPreco tipoPreco;
    private Integer quantidade;
    private BigDecimal precoLimite;
    private BigDecimal precoExecucao;
    private StatusOrdem status;
    private String motivo;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;

    public static OrdemResponseDTO de(Ordem ordem) {
        return new OrdemResponseDTO(
                ordem.getId(),
                ordem.getTicker(),
                ordem.getTipoOrdem(),
                ordem.getTipoPreco(),
                ordem.getQuantidade(),
                ordem.getPrecoLimite(),
                ordem.getPrecoExecucao(),
                ordem.getStatus(),
                ordem.getMotivo(),
                ordem.getDataCriacao(),
                ordem.getDataAtualizacao()
        );
    }
}
