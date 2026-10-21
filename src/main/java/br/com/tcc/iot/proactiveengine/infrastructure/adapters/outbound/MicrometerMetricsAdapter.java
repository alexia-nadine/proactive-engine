package br.com.tcc.iot.proactiveengine.infrastructure.adapters.outbound;

import br.com.tcc.iot.proactiveengine.application.ports.output.MetricsPort;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class MicrometerMetricsAdapter implements MetricsPort {

    private static final Logger log = LoggerFactory.getLogger(MicrometerMetricsAdapter.class);

    private final MeterRegistry meterRegistry;

    public MicrometerMetricsAdapter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void incrementProactiveAction(String routine, String action, String room, String posture, String physicalRelief, String cognitiveRelief) {
        meterRegistry.counter("proactive_actions",
                "routine", routine,
                "action", action,
                "room", room,
                "posture", posture,
                "physical_relief", physicalRelief,
                "cognitive_relief", cognitiveRelief
        ).increment();

        log.info("[MÉTRICA ENVIADA] +1 Ação Proativa | Rotina: '{}' | Alívio Físico: '{}' | Alívio Cognitivo: '{}'",
                routine, physicalRelief, cognitiveRelief);
    }

    @Override
    public void incrementDaytimeEventIgnored() {
        meterRegistry.counter("daytime_events_ignored",
                "reason", "daytime_safe_window"
        ).increment();
        log.debug("[MÉTRICA ENVIADA] +1 Evento Ignorado (Janela Diurna)");
    }

    @Override
    public void incrementFalsePositiveEvent() {
        meterRegistry.counter("false_positive_events",
                "reason", "bed_pressure_relief_filter"
        ).increment();
        log.info("[MÉTRICA ENVIADA] +1 Evento Ignorado (Falso Positivo/Alívio de Pressão)");
    }

    @Override
    public void incrementRabbitMqConsumedEvent() {
        meterRegistry.counter("rabbitmq_context_events").increment();
    }

    @Override
    public void incrementRabbitMqConsumptionError() {
        meterRegistry.counter("rabbitmq_context_events_errors").increment();
    }

    @Override
    public void recordProcessingLatencyInMilliseconds(long latencyInMilliseconds) {
        meterRegistry.timer("context_event_processing_duration")
                .record(latencyInMilliseconds, TimeUnit.MILLISECONDS);
    }

    @Override
    public void incrementAvoidedManualInteraction(String interactionType) {
        meterRegistry.counter("manual_interactions_avoided",
                "interaction_type", interactionType
        ).increment();
    }

    @Override
    public void incrementDecisionNoAction() {
        meterRegistry.counter("decision_no_action").increment();
    }

    @Override
    public void incrementProactiveDecision() {
        meterRegistry.counter("proactive_decisions").increment();
    }
}
