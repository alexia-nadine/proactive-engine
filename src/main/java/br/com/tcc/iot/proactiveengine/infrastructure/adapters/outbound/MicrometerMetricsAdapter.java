package br.com.tcc.iot.proactiveengine.infrastructure.adapters.outbound;

import br.com.tcc.iot.proactiveengine.application.ports.output.MetricsPort;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MicrometerMetricsAdapter implements MetricsPort {

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
    }

    @Override
    public void incrementIgnoredDaytimeEvent() {
        meterRegistry.counter("proactive_events_ignored",
                "reason", "daytime_safe_window"
        ).increment();
    }
}
