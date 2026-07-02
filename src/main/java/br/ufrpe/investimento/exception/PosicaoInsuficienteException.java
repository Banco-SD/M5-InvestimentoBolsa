package br.ufrpe.investimento.exception;

public class PosicaoInsuficienteException extends RuntimeException {
    public PosicaoInsuficienteException(String message) {
        super(message);
    }
}
