package br.ufrpe.investimento.exception;

/** Lançada quando não foi possível obter a cotação do ativo (API externa fora do ar, ticker inválido, etc.). */
public class AtivoIndisponivelException extends RuntimeException {
    public AtivoIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
