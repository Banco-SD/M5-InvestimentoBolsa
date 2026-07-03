package br.ufrpe.investimento.service;

import br.ufrpe.investimento.config.RabbitMQConfig;
import br.ufrpe.investimento.dto.OrdemExecutadaEventoDTO;
import br.ufrpe.investimento.model.Ordem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Service
public class OrdemExecutadaEventoPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrdemExecutadaEventoPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public OrdemExecutadaEventoPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarExecucao(Ordem ordem) {
        BigDecimal valorTotal = ordem.getPrecoExecucao().multiply(BigDecimal.valueOf(ordem.getQuantidade()));

        OrdemExecutadaEventoDTO evento = new OrdemExecutadaEventoDTO(
                ordem.getId(),
                ordem.getUsuarioId(),
                ordem.getTicker(),
                ordem.getTipoOrdem(),
                ordem.getQuantidade(),
                ordem.getPrecoExecucao(),
                valorTotal,
                LocalDateTime.now()
        );

        try {
            rabbitTemplate.convertAndSend(
                    "",
                    RabbitMQConfig.QUEUE_ORDEM_EXECUTADA,
                    evento
            );
        } catch (AmqpException ex) {
            log.error("Falha ao publicar evento de execução da ordem {} - extrato pode ficar desatualizado", ordem.getId(), ex);
        }
    }
}