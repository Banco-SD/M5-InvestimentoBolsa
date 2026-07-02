package br.ufrpe.investimento.service;

import br.ufrpe.investimento.dto.CotacaoResponseDTO;
import br.ufrpe.investimento.exception.AtivoIndisponivelException;
import br.ufrpe.investimento.model.Ativo;
import br.ufrpe.investimento.repository.AtivoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Busca cotações na brapi.dev (https://brapi.dev/api/quote/{ticker}).
 * 4 tickers de teste (PETR4, VALE3, MGLU3, ITUB4) funcionam sem token;
 * qualquer outro exige token (conta gratuita: 15.000 requisições/mês).
 *
 * Cacheamos o preço na tabela `ativo` por CACHE_TTL_SEGUNDOS: além de economizar
 * cota da API, protege o fluxo de ordens caso a brapi fique momentaneamente fora
 * do ar (usamos o último preço conhecido em vez de falhar a ordem toda).
 */
@Service
public class BrapiCotacaoService implements CotacaoService {

    private static final String BRAPI_BASE_URL = "https://brapi.dev/api/quote/";
    private static final long CACHE_TTL_SEGUNDOS = 30;

    private final RestTemplate restTemplate;
    private final AtivoRepository ativoRepository;

    @Value("${brapi.token:}")
    private String brapiToken;

    public BrapiCotacaoService(RestTemplate restTemplate, AtivoRepository ativoRepository) {
        this.restTemplate = restTemplate;
        this.ativoRepository = ativoRepository;
    }

    @Override
    @Transactional
    public CotacaoResponseDTO buscarCotacao(String ticker) {
        String tickerNormalizado = ticker.trim().toUpperCase();

        Ativo ativo = ativoRepository.findById(tickerNormalizado).orElse(null);

        boolean cacheValido = ativo != null
                && ativo.getDataAtualizacaoPreco() != null
                && ativo.getDataAtualizacaoPreco().isAfter(LocalDateTime.now().minus(CACHE_TTL_SEGUNDOS, ChronoUnit.SECONDS));

        if (cacheValido) {
            return paraDTO(ativo);
        }

        try {
            Map<String, Object> corpo = chamarBrapi(tickerNormalizado);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> resultados = (List<Map<String, Object>>) corpo.get("results");

            if (resultados == null || resultados.isEmpty()) {
                throw new AtivoIndisponivelException("Ativo '" + tickerNormalizado + "' não encontrado na B3");
            }

            Map<String, Object> resultado = resultados.get(0);
            BigDecimal preco = new BigDecimal(resultado.get("regularMarketPrice").toString());
            BigDecimal variacao = resultado.get("regularMarketChangePercent") != null
                    ? new BigDecimal(resultado.get("regularMarketChangePercent").toString())
                    : BigDecimal.ZERO;
            String nome = (String) resultado.getOrDefault("shortName", tickerNormalizado);

            if (ativo == null) {
                ativo = new Ativo();
                ativo.setTicker(tickerNormalizado);
            }
            ativo.setNome(nome);
            ativo.setUltimoPrecoConhecido(preco);
            ativo.setDataAtualizacaoPreco(LocalDateTime.now());
            ativoRepository.save(ativo);

            return new CotacaoResponseDTO(tickerNormalizado, nome, preco, variacao, ativo.getDataAtualizacaoPreco());

        } catch (RestClientException ex) {
            // A API externa falhou: se temos um preço em cache (mesmo vencido),
            // usamos ele em vez de derrubar a criação da ordem por completo.
            if (ativo != null && ativo.getDataAtualizacaoPreco() != null) {
                return paraDTO(ativo);
            }
            throw new AtivoIndisponivelException("Não foi possível obter a cotação de " + tickerNormalizado + " agora");
        }
    }

    private Map<String, Object> chamarBrapi(String ticker) {
        HttpHeaders headers = new HttpHeaders();
        if (brapiToken != null && !brapiToken.isBlank()) {
            headers.set("Authorization", "Bearer " + brapiToken);
        }
        HttpEntity<Void> requisicao = new HttpEntity<>(headers);

        ResponseEntity<Map> resposta = restTemplate.exchange(
                BRAPI_BASE_URL + ticker, HttpMethod.GET, requisicao, Map.class
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> corpo = resposta.getBody();
        if (corpo == null) {
            throw new AtivoIndisponivelException("Resposta vazia da API de cotações para " + ticker);
        }
        return corpo;
    }

    private CotacaoResponseDTO paraDTO(Ativo ativo) {
        return new CotacaoResponseDTO(
                ativo.getTicker(), ativo.getNome(), ativo.getUltimoPrecoConhecido(),
                BigDecimal.ZERO, ativo.getDataAtualizacaoPreco()
        );
    }
}
