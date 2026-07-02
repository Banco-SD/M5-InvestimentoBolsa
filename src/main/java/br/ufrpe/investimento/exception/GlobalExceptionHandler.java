package br.ufrpe.investimento.exception;

import br.ufrpe.investimento.dto.ErroResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({AtivoNaoEncontradoException.class, OrdemNaoEncontradaException.class})
    public ResponseEntity<ErroResponseDTO> tratarNaoEncontrado(RuntimeException ex) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({SaldoInsuficienteException.class, PosicaoInsuficienteException.class, OrdemInvalidaException.class})
    public ResponseEntity<ErroResponseDTO> tratarRegraNegocio(RuntimeException ex) {
        return construir(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(AtivoIndisponivelException.class)
    public ResponseEntity<ErroResponseDTO> tratarAtivoIndisponivel(AtivoIndisponivelException ex) {
        return construir(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(ServicoExternoException.class)
    public ResponseEntity<ErroResponseDTO> tratarServicoExterno(ServicoExternoException ex) {
        return construir(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponseDTO> tratarValidacao(MethodArgumentNotValidException ex) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        ErroResponseDTO erro = new ErroResponseDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Dados inválidos",
                "Um ou mais campos não passaram na validação",
                detalhes
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponseDTO> tratarGenerico(Exception ex) {
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor");
    }

    private ResponseEntity<ErroResponseDTO> construir(HttpStatus status, String mensagem) {
        ErroResponseDTO erro = new ErroResponseDTO(
                LocalDateTime.now(), status.value(), status.getReasonPhrase(), mensagem, null
        );
        return ResponseEntity.status(status).body(erro);
    }
}
