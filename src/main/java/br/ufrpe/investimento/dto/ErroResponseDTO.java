package br.ufrpe.investimento.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class ErroResponseDTO {
    private LocalDateTime timestamp;
    private int status;
    private String erro;
    private String mensagem;
    private List<String> detalhes;
}
