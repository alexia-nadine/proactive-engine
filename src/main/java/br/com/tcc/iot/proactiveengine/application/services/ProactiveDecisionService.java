package br.com.tcc.iot.proactiveengine.application.services;

import br.com.tcc.iot.proactiveengine.application.ports.input.EvaluateRoutineUseCase;
import br.com.tcc.iot.proactiveengine.application.ports.output.ActionTriggerPort;
import br.com.tcc.iot.proactiveengine.domain.ContextEventPayload;
import br.com.tcc.iot.proactiveengine.domain.enums.BedPressureStatus;
import br.com.tcc.iot.proactiveengine.domain.enums.DoorStatus;
import br.com.tcc.iot.proactiveengine.domain.enums.RoomLocation;
import br.com.tcc.iot.proactiveengine.domain.enums.UserPosture;
import br.com.tcc.iot.proactiveengine.infrastructure.config.ProactiveRulesProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Service
public class ProactiveDecisionService implements EvaluateRoutineUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProactiveDecisionService.class);

    private final ProactiveRulesProperties thresholds;
    private final ActionTriggerPort actionTriggerPort;

    public ProactiveDecisionService(ProactiveRulesProperties thresholds, ActionTriggerPort actionTriggerPort) {
        this.thresholds = thresholds;
        this.actionTriggerPort = actionTriggerPort;
    }

    @Override
    public void evaluate(ContextEventPayload payload) {
        if (!isNightTime(payload.timeOfDay())) {
            log.debug("Monitorização: Evento ignorado. Fora do limiar noturno definido.");
            return;
        }

        evaluateSleepRoutine(payload);
        evaluateNightMovementRoutine(payload);
    }

    /**
     * ROTINA 1: Boa Noite Autônoma
     * Avalia o risco de evasão noturna quando o usuario com mobilidade reduzida já se encontra deitado.
     * Matriz: Deitado + No Quarto + Cama Ocupada
     */
    private void evaluateSleepRoutine(ContextEventPayload payload) {
        boolean isUserSleeping = payload.userPosture() == UserPosture.LYING_DOWN &&
                payload.roomLocation() == RoomLocation.BEDROOM &&
                payload.bedPressureStatus() == BedPressureStatus.OCCUPIED;

        // Guard Clause: Se não estiver dormindo, interrompe a avaliação desta rotina
        if (!isUserSleeping) {
            return;
        }

        // Avaliação de vulnerabilidade e tomada de decisão
        if (payload.doorStatus() == DoorStatus.UNLOCKED) {
            actionTriggerPort.triggerSecurityAlert();
            log.info("ROTINA 1 DISPARADA: Trancando portas e apagando luzes. Motivo: Repouso iniciado com portas destrancadas.");
        } else {
            log.debug("ROTINA 1 IGNORADA: A porta já se encontra trancada de forma segura.");
        }
    }

    /**
     * ROTINA 2: Deslocamento Noturno Seguro
     * Avalia o risco de queda por ausência de luminosidade caso o usuario se levante de madrugada.
     * Matriz: Cama Vazia + Movimento Detetado
     */
    private void evaluateNightMovementRoutine(ContextEventPayload payload) {
        boolean isUserMoving = payload.bedPressureStatus() == BedPressureStatus.UNOCCUPIED &&
                Boolean.TRUE.equals(payload.presenceDetected());

        // Guard Clause: Se não houver movimentação fora da cama, interrompe a avaliação
        if (!isUserMoving) {
            return;
        }

        // Avaliação de risco visual e tomada de decisão
        if (payload.luminosityLux() < thresholds.minSafeLuminosity()) {
            actionTriggerPort.turnOnPathLights();
            log.info("ROTINA 2 DISPARADA: Acendendo caminho de luz. Motivo: Risco visual na transferência ou movimentação.");
        } else {
            log.debug("ROTINA 2 IGNORADA: O ambiente já possui luminosidade igual ou superior ao limiar seguro.");
        }
    }

    private boolean isNightTime(LocalTime eventTime) {
        // Lógica segura para calcular madrugadas (ex: 21:00 às 05:00 cruza a meia-noite)
        if (thresholds.nightStartTime().isAfter(thresholds.nightEndTime())) {
            return !eventTime.isBefore(thresholds.nightStartTime()) || !eventTime.isAfter(thresholds.nightEndTime());
        }
        return !eventTime.isBefore(thresholds.nightStartTime()) && !eventTime.isAfter(thresholds.nightEndTime());
    }
}
