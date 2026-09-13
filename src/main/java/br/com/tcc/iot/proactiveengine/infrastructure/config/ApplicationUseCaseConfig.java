package br.com.tcc.iot.proactiveengine.infrastructure.config;

import br.com.tcc.iot.proactiveengine.application.ports.input.EvaluateRoutineUseCase;
import br.com.tcc.iot.proactiveengine.application.ports.input.ProactiveRulesConfig;
import br.com.tcc.iot.proactiveengine.application.ports.output.ActionTriggerPort;
import br.com.tcc.iot.proactiveengine.application.ports.output.MetricsPort;
import br.com.tcc.iot.proactiveengine.application.services.ProactiveDecisionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ApplicationUseCaseConfig {

    @Bean
    public EvaluateRoutineUseCase evaluateRoutineUseCase(
            ProactiveRulesConfig proactiveRulesConfig,
            ActionTriggerPort actionTriggerPort,
            MetricsPort metricsPort,
            Clock clock
    ) {
        return new ProactiveDecisionService(proactiveRulesConfig, actionTriggerPort, metricsPort, clock);
    }
}
