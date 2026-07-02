package br.ufrpe.investimento.exception;

public class ServicoExternoException extends RuntimeException {
    public ServicoExternoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    public ServicoExternoException(String mensagem) {
        super(mensagem);
    }
}
