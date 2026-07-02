package br.ufrpe.investimento.exception;

public class AtivoNaoEncontradoException extends RuntimeException {
    public AtivoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
