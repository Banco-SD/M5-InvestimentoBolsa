package br.ufrpe.investimento.service;

import br.ufrpe.investimento.exception.ServicoExternoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
public class WalletClientHttp implements WalletClient {

    private final RestTemplate restTemplate;
    private final String walletBaseUrl;

    public WalletClientHttp(RestTemplate restTemplate, @Value("${wallet.service.url}") String walletBaseUrl) {
        this.restTemplate = restTemplate;
        this.walletBaseUrl = walletBaseUrl;
    }

    @Override
    public ReservaSaldo reservar(UUID usuarioId, BigDecimal valor, UUID referenciaOrdemId) {
        try {
            Map<String, Object> corpo = Map.of(
                    "usuarioId", usuarioId,
                    "valor", valor,
                    "referenciaId", referenciaOrdemId
            );
            @SuppressWarnings("unchecked")
            Map<String, Object> resposta = restTemplate.postForObject(
                    walletBaseUrl + "/carteira/reservar", corpo, Map.class
            );

            UUID idReserva = UUID.fromString(resposta.get("idReserva").toString());
            return new ReservaSaldo(true, idReserva, null);

        } catch (HttpClientErrorException ex) {
            // Convenção assumida: o Wallet Service retorna 402 (Payment Required) ou
            // 409 (Conflict) quando o saldo é insuficiente. PAYMENT_REQUIRED não tem
            // uma subclasse dedicada no Spring (só Conflict, NotFound, etc. têm),
            // por isso checamos o status manualmente em vez de usar catch por tipo.
            if (ex.getStatusCode() == HttpStatus.PAYMENT_REQUIRED || ex.getStatusCode() == HttpStatus.CONFLICT) {
                return new ReservaSaldo(false, null, "Saldo insuficiente");
            }
            throw new ServicoExternoException("Wallet Service retornou erro inesperado ao reservar saldo: " + ex.getStatusCode(), ex);
        } catch (RestClientException ex) {
            throw new ServicoExternoException("Não foi possível comunicar com o Wallet Service para reservar saldo", ex);
        }
    }

    @Override
    public void confirmar(UUID idReserva) {
        try {
            restTemplate.postForLocation(walletBaseUrl + "/carteira/confirmar/" + idReserva, null);
        } catch (RestClientException ex) {
            // Se isto falhar, o dinheiro fica "reservado" (não perdido) no Wallet Service.
            // Uma rotina de reconciliação/retry seria o próximo passo num sistema de produção.
            throw new ServicoExternoException("Não foi possível confirmar a reserva de saldo " + idReserva, ex);
        }
    }

    @Override
    public void liberar(UUID idReserva) {
        try {
            restTemplate.postForLocation(walletBaseUrl + "/carteira/liberar/" + idReserva, null);
        } catch (RestClientException ex) {
            // Falha na PRÓPRIA compensação é o pior caso da saga: aqui relançamos
            // como erro de servidor pra ficar bem visível nos logs, já que
            // significa dinheiro reservado que ainda não foi devolvido.
            throw new ServicoExternoException("Falha ao compensar (liberar) a reserva de saldo " + idReserva, ex);
        }
    }

    @Override
    public void creditar(UUID usuarioId, BigDecimal valor, UUID referenciaOrdemId) {
        try {
            Map<String, Object> corpo = Map.of(
                    "usuarioId", usuarioId,
                    "valor", valor,
                    "referenciaId", referenciaOrdemId
            );
            restTemplate.postForLocation(walletBaseUrl + "/carteira/creditar", corpo);
        } catch (RestClientException ex) {
            throw new ServicoExternoException("Não foi possível creditar o valor da venda para o usuário " + usuarioId, ex);
        }
    }
}
