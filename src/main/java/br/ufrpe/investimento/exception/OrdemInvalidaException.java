package br.ufrpe.investimento.exception;

public class OrdemInvalidaException extends RuntimeException {
    public OrdemInvalidaException(String mensagem) {
        super(mensagem);
    }
}
