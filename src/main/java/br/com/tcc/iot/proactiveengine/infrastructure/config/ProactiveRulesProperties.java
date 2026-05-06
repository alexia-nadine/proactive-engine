package br.com.tcc.iot.proactiveengine.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.LocalTime;

@ConfigurationProperties(prefix = "proactive.rules.thresholds")
public record ProactiveRulesProperties (
        LocalTime nightStartTime,
        LocalTime nightEndTime,
        Integer minSafeLuminosity,
        Integer maxBathroomDurationMinutes
) {
}
