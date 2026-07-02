package br.ufrpe.investimento.service;

import br.ufrpe.investimento.config.RabbitMQConfig;
import br.ufrpe.investimento.dto.OrdemMensagemDTO;
import br.ufrpe.investimento.model.Ordem;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrdemPublisherService {

    private final RabbitTemplate rabbitTemplate;

    public OrdemPublisherService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publica a ordem na fila de processamento.
     * Lança AmqpException se o broker estiver inacessível - quem chama
     * (OrdemService) é responsável por compensar a reserva de saldo/posição
     * nesse caso.
     */
    public void publicar(Ordem ordem) {
        OrdemMensagemDTO mensagem = new OrdemMensagemDTO(
                ordem.getId(), ordem.getUsuarioId(), ordem.getTicker(),
                ordem.getTipoOrdem(), ordem.getTipoPreco(), ordem.getQuantidade(), ordem.getPrecoLimite()
        );

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_ORDENS, RabbitMQConfig.ROUTING_KEY_ORDENS, mensagem
            );
        } catch (AmqpException ex) {
            throw new AmqpException("Falha ao publicar ordem " + ordem.getId() + " na fila de processamento", ex);
        }
    }
}
