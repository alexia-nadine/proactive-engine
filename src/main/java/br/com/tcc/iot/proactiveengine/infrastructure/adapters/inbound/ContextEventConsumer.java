package br.com.tcc.iot.proactiveengine.infrastructure.adapters.inbound;

import br.com.tcc.iot.proactiveengine.application.ports.input.EvaluateRoutineUseCase;
import br.com.tcc.iot.proactiveengine.domain.ContextEventPayload;
import br.com.tcc.iot.proactiveengine.infrastructure.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ContextEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ContextEventConsumer.class);

    private final EvaluateRoutineUseCase evaluateRoutineUseCase;

    public ContextEventConsumer(EvaluateRoutineUseCase evaluateRoutineUseCase) {
        this.evaluateRoutineUseCase = evaluateRoutineUseCase;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void receiveSensorEvent(ContextEventPayload payload) {
        log.info("[RabbitMQ] Evento de contexto recebido da simulação IoT.");

        // O Adaptador apenas repassa o objeto limpo para o motor de regras decidir
        evaluateRoutineUseCase.evaluate(payload);
    }
}
