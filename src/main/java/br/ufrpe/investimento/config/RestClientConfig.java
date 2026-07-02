package br.ufrpe.investimento.config;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    /**
     * Usado para chamar tanto o Wallet Service quanto a brapi.dev.
     * Timeouts curtos são importantes aqui: se a API externa travar, não
     * queremos que a criação de uma ordem fique pendurada indefinidamente.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }
}
