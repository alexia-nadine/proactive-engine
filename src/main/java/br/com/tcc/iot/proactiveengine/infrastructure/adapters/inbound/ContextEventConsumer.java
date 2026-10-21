package br.com.tcc.iot.proactiveengine.infrastructure.adapters.inbound;

import br.com.tcc.iot.proactiveengine.application.ports.input.EvaluateRoutineUseCase;
import br.com.tcc.iot.proactiveengine.application.ports.output.MetricsPort;
import br.com.tcc.iot.proactiveengine.domain.ContextEventPayload;
import br.com.tcc.iot.proactiveengine.infrastructure.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ContextEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ContextEventConsumer.class);

    private final EvaluateRoutineUseCase evaluateRoutineUseCase;

    private final MetricsPort metricsPort;

    private final AtomicInteger throughputCounter = new AtomicInteger(0);

    public ContextEventConsumer(EvaluateRoutineUseCase evaluateRoutineUseCase, MetricsPort metricsPort) {
        this.evaluateRoutineUseCase = evaluateRoutineUseCase;
        this.metricsPort = metricsPort;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void receiveSensorEvent(ContextEventPayload payload) {
        final long startTime = System.currentTimeMillis();
        log.debug("[RabbitMQ] Evento de contexto recebido da simulação IoT.");

        try {
            if (payload == null || payload.timeOfDay() == null || payload.userPosture() == null
                    || payload.roomLocation() == null || payload.doorStatus() == null
                    || payload.bedPressureStatus() == null || payload.presenceDetected() == null
                    || payload.luminosityLux() == null) {
                log.warn("[RabbitMQ] Payload de contexto inválido ou incompleto. Evento descartado.");
                metricsPort.incrementRabbitMqConsumptionError();
                return;
            }


            evaluateRoutineUseCase.evaluate(payload);

            //incrementa o contador de prova real
            throughputCounter.incrementAndGet();

            metricsPort.incrementRabbitMqConsumedEvent();

            long latencyMs = System.currentTimeMillis() - startTime;
            metricsPort.recordProcessingLatencyInMilliseconds(latencyMs);

            //prova Real de latencia -> Loga apenas 1% dos pacotes (Amostragem)
            //isso evita poluição no console, mas permite bater os números com o Grafana
            if (Math.random() < 0.01) {
                log.info("[PROVA REAL - LATÊNCIA] Amostra de processamento: {} ms", latencyMs);
            }

        } catch (Exception ex) {
            metricsPort.incrementRabbitMqConsumptionError();
            log.error("[RabbitMQ] Falha ao processar evento de contexto.", ex);
            throw ex;
        }

    }

    /**
     * 4. Prova Real de Vazão: O Spring executa este método a cada 1 segundo.
     * Ele pega a quantidade de mensagens consumidas, imprime no log e zera o contador.
     */
    @Scheduled(fixedRate = 1000)
    public void logThroughputReal() {
        int messagesProcessedInLastSecond = throughputCounter.getAndSet(0);

        if (messagesProcessedInLastSecond > 0) {
            log.warn("[PROVA REAL - VAZÃO] Throughput auditado internamente: {} msgs/sec", messagesProcessedInLastSecond);
        }
    }
}
