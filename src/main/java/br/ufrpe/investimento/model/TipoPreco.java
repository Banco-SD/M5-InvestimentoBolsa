package br.ufrpe.investimento.model;

public enum TipoPreco {
    /** Executa imediatamente ao preço de mercado atual */
    MERCADO,
    /** Só executa se o preço de mercado atingir o precoLimite informado */
    LIMITADA
}
