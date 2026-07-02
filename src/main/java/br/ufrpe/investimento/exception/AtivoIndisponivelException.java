package br.ufrpe.investimento.exception;

public class AtivoIndisponivelException extends RuntimeException {
    public AtivoIndisponivelException(String message) {
        super(message);
    }
}
