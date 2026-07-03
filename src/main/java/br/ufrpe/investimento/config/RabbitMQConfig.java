package br.ufrpe.investimento.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    public static final String EXCHANGE_ORDENS = "investimento.ordens.exchange";
    public static final String QUEUE_ORDENS = "investimento.ordens.processamento";
    public static final String ROUTING_KEY_ORDENS = "ordens.processar";

    // Dead-letter: se o Matching Engine falhar repetidamente ao processar
    // uma mensagem (ex.: exceção antes mesmo de conseguir compensar), ela
    // cai aqui em vez de ser perdida ou reprocessada pra sempre.
    private static final String DLQ_ORDENS = "investimento.ordens.processamento.dlq";
    private static final String DLX_ORDENS = "investimento.ordens.dlx";
    public static final String QUEUE_ORDEM_EXECUTADA = "fila.ordem.executada";

    @Bean
    public Queue queueOrdemExecutada() {
        return QueueBuilder.durable(QUEUE_ORDEM_EXECUTADA).build();
    }

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
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    // Container factory usada pelos @RabbitListener. Sem isso, uma exceção
    // não tratada no listener fica sendo reenfileirada pra sempre em vez de
    // cair na DLQ configurada acima.
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}