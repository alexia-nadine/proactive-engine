package br.com.tcc.iot.proactiveengine.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@SuppressWarnings("removal")
public class RabbitMQConfig {

    public static final String QUEUE_NAME = "iot.sensor.events";
    public static final String EXCHANGE_NAME = "iot.exchange";
    public static final String ROUTING_KEY = "sensor.routing.key";


    @Bean
    public Queue sensorQueue() {
        return new Queue(QUEUE_NAME, true); // true = A fila sobrevive se o servidor for reiniciado
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding binding(Queue queue, DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }

    // Traduz o JSON bruto para o nosso Record ContextEventPayload de forma automática
    @Bean
    public Jackson2JsonMessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
