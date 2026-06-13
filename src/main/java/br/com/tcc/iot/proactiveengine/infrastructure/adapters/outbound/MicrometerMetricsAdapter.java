package br.com.tcc.iot.proactiveengine.infrastructure.adapters.outbound;

import br.com.tcc.iot.proactiveengine.application.ports.output.MetricsPort;
import br.com.tcc.iot.proactiveengine.application.services.ProactiveDecisionService;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MicrometerMetricsAdapter implements MetricsPort {

    private static final Logger log = LoggerFactory.getLogger(MicrometerMetricsAdapter.class);

    private final MeterRegistry meterRegistry;

    public MicrometerMetricsAdapter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }


    @Override
    public void incrementProactiveAction(String routine, String action, String room, String posture, String physicalRelief, String cognitiveRelief) {
        meterRegistry.counter("proactive_actions_total",
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
    public void incrementIgnoredDaytimeEvent() {
        meterRegistry.counter("proactive_events_ignored",
                "reason", "daytime_safe_window"
        ).increment();
        log.debug("[MÉTRICA ENVIADA] +1 Evento Ignorado (Janela Diurna)");
    }

    @Override
    public void incrementFalsePositiveEvent() {
        meterRegistry.counter("proactive_events_ignored",
                "reason", "false_positive"
        ).increment();
        log.info("[MÉTRICA ENVIADA] +1 Evento Ignorado (Falso Positivo/Alívio de Pressão)");
    }
}
