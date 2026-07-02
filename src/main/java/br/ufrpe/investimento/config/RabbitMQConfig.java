package br.ufrpe.investimento.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_ORDENS = "investimento.ordens.exchange";
    public static final String QUEUE_ORDENS = "investimento.ordens.processamento";
    public static final String ROUTING_KEY_ORDENS = "ordens.processar";

    // Dead-letter: se o Matching Engine falhar repetidamente ao processar
    // uma mensagem (ex.: exceção antes mesmo de conseguir compensar), ela
    // cai aqui em vez de ser perdida ou reprocessada pra sempre.
    private static final String DLQ_ORDENS = "investimento.ordens.processamento.dlq";
    private static final String DLX_ORDENS = "investimento.ordens.dlx";

    @Bean
    public TopicExchange exchangeOrdens() {
        return new TopicExchange(EXCHANGE_ORDENS);
    }

    @Bean
    public Queue queueOrdens() {
        return QueueBuilder.durable(QUEUE_ORDENS)
                .withArgument("x-dead-letter-exchange", DLX_ORDENS)
                .withArgument("x-dead-letter-routing-key", DLQ_ORDENS)
                .build();
    }

    @Bean
    public Binding bindingOrdens(Queue queueOrdens, TopicExchange exchangeOrdens) {
        return BindingBuilder.bind(queueOrdens).to(exchangeOrdens).with(ROUTING_KEY_ORDENS);
    }

    @Bean
    public DirectExchange dlxOrdens() {
        return new DirectExchange(DLX_ORDENS);
    }

    @Bean
    public Queue dlqOrdens() {
        return QueueBuilder.durable(DLQ_ORDENS).build();
    }

    @Bean
    public Binding bindingDlq(Queue dlqOrdens, DirectExchange dlxOrdens) {
        return BindingBuilder.bind(dlqOrdens).to(dlxOrdens).with(DLQ_ORDENS);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
