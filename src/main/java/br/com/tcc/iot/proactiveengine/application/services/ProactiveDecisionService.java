package br.com.tcc.iot.proactiveengine.application.services;

import br.com.tcc.iot.proactiveengine.application.ports.input.EvaluateRoutineUseCase;
import br.com.tcc.iot.proactiveengine.application.ports.output.ActionTriggerPort;
import br.com.tcc.iot.proactiveengine.domain.ContextEventPayload;
import br.com.tcc.iot.proactiveengine.domain.enums.BedPressureStatus;
import br.com.tcc.iot.proactiveengine.infrastructure.config.ProactiveRulesProperties;

import java.time.LocalTime;

public class ProactiveDecisionService implements EvaluateRoutineUseCase {
    private final ProactiveRulesProperties thresholds;
    private final ActionTriggerPort actionTriggerPort;

    public ProactiveDecisionService(ProactiveRulesProperties thresholds, ActionTriggerPort actionTriggerPort) {
        this.thresholds = thresholds;
        this.actionTriggerPort = actionTriggerPort;
    }

    @Override
    public void evaluate(ContextEventPayload payload) {
        if (isNightTime(payload.getTimeOfDay())) {

            // Rotina 2: Deslocamento Noturno Seguro (Prevenção de Quedas)
            if (payload.getBedPressureStatus() == BedPressureStatus.UNOCCUPIED
                    && payload.getLuminosityLux() < thresholds.minSafeLuminosity()) {

                System.out.println("AÇÃO PROATIVA DISPARADA: Acendendo caminho de luz. Motivo: Risco visual de queda.");

            }

            // Rotina 1: Boa Noite Autônoma
            if (payload.getBedPressureStatus() == BedPressureStatus.OCCUPIED) {
                System.out.println("AÇÃO PROATIVA DISPARADA: Trancando portas e apagando luzes. Motivo: Repouso iniciado.");
            }
        }
    }

    private boolean isNightTime(LocalTime eventTime) {
        if (thresholds.nightStartTime().isAfter(thresholds.nightEndTime())) {
            return !eventTime.isBefore(thresholds.nightStartTime()) || !eventTime.isAfter(thresholds.nightEndTime());
        }
        return !eventTime.isBefore(thresholds.nightStartTime()) && !eventTime.isAfter(thresholds.nightEndTime());
    }
}
