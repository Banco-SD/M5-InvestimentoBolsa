package br.ufrpe.investimento.exception;

public class AtivoNaoEncontradoException extends RuntimeException {
    public AtivoNaoEncontradoException(String message) {
        super(message);
    }
}
