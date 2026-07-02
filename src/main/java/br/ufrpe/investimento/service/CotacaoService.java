package br.ufrpe.investimento.service;

import br.ufrpe.investimento.dto.CotacaoResponseDTO;

public interface CotacaoService {

    /**
     * Retorna a cotação atual do ativo. Implementações devem cachear
     * localmente por um curto período para não estourar o limite de
     * requisições da API externa.
     */
    CotacaoResponseDTO buscarCotacao(String ticker);
}
