package br.com.tcc.iot.proactiveengine.application.services;

import br.com.tcc.iot.proactiveengine.application.ports.input.EvaluateRoutineUseCase;
import br.com.tcc.iot.proactiveengine.application.ports.output.ActionTriggerPort;
import br.com.tcc.iot.proactiveengine.domain.ContextEventPayload;
import br.com.tcc.iot.proactiveengine.domain.enums.BedPressureStatus;
import br.com.tcc.iot.proactiveengine.domain.enums.DoorStatus;
import br.com.tcc.iot.proactiveengine.domain.enums.UserPosture;
import br.com.tcc.iot.proactiveengine.infrastructure.config.ProactiveRulesProperties;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Service
public class ProactiveDecisionService implements EvaluateRoutineUseCase {
    private final ProactiveRulesProperties thresholds;
    private final ActionTriggerPort actionTriggerPort;

    public ProactiveDecisionService(ProactiveRulesProperties thresholds, ActionTriggerPort actionTriggerPort) {
        this.thresholds = thresholds;
        this.actionTriggerPort = actionTriggerPort;
    }

    @Override
    public void evaluate(ContextEventPayload payload) {
        // O sistema só monitoriza as rotinas de proatividade de segurança no período noturno
        if (isNightTime(payload.getTimeOfDay())) {

            // ==========================================
            // ROTINA 1: BOA NOITE AUTÓNOMA
            // Matriz: Deitado + No Quarto + Cama Ocupada
            // ==========================================
            boolean isUserSleeping = payload.getUserPosture() == UserPosture.LYING_DOWN &&
                    "BEDROOM".equals(payload.getRoomLocation()) && payload.getBedPressureStatus() == BedPressureStatus.OCCUPIED;

            if (isUserSleeping) {
                // Só dispara a ação se a porta estiver efetivamente vulnerável (destrancada)
                if (payload.getDoorStatus() == DoorStatus.UNLOCKED) {
                    actionTriggerPort.triggerSecurityAlert();
                    System.out.println("ROTINA 1 - AÇÃO PROATIVA DISPARADA: Trancando portas e apagando luzes. Motivo: Repouso iniciado.");
                } else {
                    System.out.println("ROTINA 1 - Ignorado: A porta já se encontra trancada. Poupando processamento.");
                }
            }

            // ==========================================
            // ROTINA 2: DESLOCAMENTO NOTURNO SEGURO
            // Matriz: Cama Vazia + Movimento Detetado
            // ==========================================
            boolean isUserMoving = payload.getBedPressureStatus() == BedPressureStatus.UNOCCUPIED &&
                    Boolean.TRUE.equals(payload.getPresenceDetected());

            if (isUserMoving) {
                // Só acende a luz guia se o ambiente estiver escuro (abaixo do limiar seguro)
                if (payload.getLuminosityLux() < thresholds.minSafeLuminosity()) {
                    actionTriggerPort.turnOnPathLights();
                    System.out.println("ROTINA 2 - AÇÃO PROATIVA DISPARADA: Acendendo caminho de luz. Motivo: Risco visual na transferência do cadeirante.");
                } else {
                    System.out.println("ROTINA 2 - Ignorado: O ambiente já possui luminosidade igual ou superior ao limiar seguro.");
                }
            }

        } else {
            System.out.println("Monitorização: Evento ignorado pois está fora do limiar noturno.");
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
