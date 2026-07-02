package br.ufrpe.investimento.exception;

public class OrdemNaoEncontradaException extends RuntimeException {
    public OrdemNaoEncontradaException(String mensagem) {
        super(mensagem);
    }
}
