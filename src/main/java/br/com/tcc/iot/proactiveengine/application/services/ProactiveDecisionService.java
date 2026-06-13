package br.com.tcc.iot.proactiveengine.application.services;

import br.com.tcc.iot.proactiveengine.application.ports.input.EvaluateRoutineUseCase;
import br.com.tcc.iot.proactiveengine.application.ports.output.ActionTriggerPort;
import br.com.tcc.iot.proactiveengine.application.ports.output.MetricsPort;
import br.com.tcc.iot.proactiveengine.domain.ContextEventPayload;
import br.com.tcc.iot.proactiveengine.domain.enums.BedPressureStatus;
import br.com.tcc.iot.proactiveengine.domain.enums.DoorStatus;
import br.com.tcc.iot.proactiveengine.domain.enums.RoomLocation;
import br.com.tcc.iot.proactiveengine.domain.enums.UserPosture;
import br.com.tcc.iot.proactiveengine.infrastructure.config.ProactiveRulesProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementação técnica do caso de uso {@link EvaluateRoutineUseCase}.
 * <p>
 * <b>Detalhe de Implementação:</b> O rastreamento temporal para filtro de falsos
 * positivos (latência da cama) é gerenciado via estado em memória (Stateful)
 * utilizando um {@code ConcurrentHashMap}.
 * </p>
 */
@Service
public class ProactiveDecisionService implements EvaluateRoutineUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProactiveDecisionService.class);

    private final ProactiveRulesProperties thresholds;
    private final ActionTriggerPort actionTriggerPort;
    private final MetricsPort metricsPort;
    private final Clock clock;


    private final ConcurrentHashMap<String, Instant> bedAbsenceTracker = new ConcurrentHashMap<>();
    private static final String PERSONA_ID = "USUARIO_MOBILIDADE_REDUZIDA";
    private boolean routine2AlreadyTriggered = false;

    public ProactiveDecisionService(ProactiveRulesProperties thresholds, ActionTriggerPort actionTriggerPort, MetricsPort metricsPort, Clock clock) {
        this.thresholds = thresholds;
        this.actionTriggerPort = actionTriggerPort;
        this.metricsPort = metricsPort;
        this.clock = clock;
    }


    /**
     * Metodo de entrada do usecase. Atua como um *Gateway* para roteamento de regras
     * baseado em janelas temporais de ciclo circadiano.
     *
     * @param payload Objeto de domínio contendo o contexto do ambiente IoT no momento do evento.
     *
     */
    @Override
    public void evaluate(ContextEventPayload payload) {
        // Log de diagnóstico crítico
        log.info("Diagnóstico: Postura={}, Porta={}, Lux={}, Hora={}",
                payload.userPosture(), payload.doorStatus(), payload.luminosityLux(), payload.timeOfDay());
        if (!isNightTime(payload.timeOfDay())) {
            log.info("Monitorização: Evento ignorado. Fora do limiar noturno definido.");
            metricsPort.incrementIgnoredDaytimeEvent();
            resetNightTrackers();
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

        if (payload.doorStatus() == DoorStatus.UNLOCKED) {
            actionTriggerPort.triggerSecurityAlert();
            log.info("ROTINA 1 DISPARADA: Trancando portas e apagando luzes.");

            metricsPort.incrementProactiveAction(
                    "Boa Noite Autonoma", "Desligar luzes e trancar portas",
                    payload.roomLocation().name(), payload.userPosture().name(),
                    "Deslocamento Cama-Porta", "Segurança Residencial"
            );
        } else {
            log.debug("ROTINA 1 IGNORADA: Portas já estão trancadas. Nenhuma ação necessária.");
        }
    }

    /**
     * ROTINA 2: Deslocamento Noturno Seguro
     * Avalia o risco de queda por ausência de luminosidade caso o usuario se levante de madrugada.
     * Matriz: Cama Vazia + Movimento Detetado
     */
    private void evaluateNightMovementRoutine(ContextEventPayload payload) {
        // sensor detectou ausência de pressão na cama -> Pode ser alívio de pressão OU saída real;
        if (payload.bedPressureStatus() == BedPressureStatus.UNOCCUPIED && Boolean.TRUE.equals(payload.presenceDetected())) {

            // Usa o relógio injetado (Clock) em vez de depender da máquina física diretamente
            bedAbsenceTracker.putIfAbsent(PERSONA_ID, Instant.now(clock));
            Instant absenceStartTime = bedAbsenceTracker.get(PERSONA_ID);
            long secondsAbsent = Duration.between(absenceStartTime, Instant.now(clock)).toSeconds();

            if (secondsAbsent >= thresholds.bedAbsenceDelaySeconds()) {
                if (!routine2AlreadyTriggered && payload.luminosityLux() < thresholds.minSafeLuminosity()) {
                    actionTriggerPort.turnOnPathLights();
                    log.info("ROTINA 2 DISPARADA: Acendendo caminho de luz. Motivo: Saída consolidada após {}s.", secondsAbsent);

                    metricsPort.incrementProactiveAction(
                            "Deslocamento Noturno Seguro", "Iluminar rota",
                            payload.roomLocation().name(), payload.userPosture().name(),
                            "Acionamento de Interruptores", "Prevenção de Colisão"
                    );
                    routine2AlreadyTriggered = true;
                }
            }
        } else if (payload.bedPressureStatus() == BedPressureStatus.OCCUPIED) {
            if (bedAbsenceTracker.containsKey(PERSONA_ID)) {
                log.info("FILTRO CLÍNICO ATIVADO: Falso positivo mitigado (Alívio de Pressão). Resetando contadores.");

                metricsPort.incrementFalsePositiveEvent();

                resetNightTrackers();
            }
        }
    }

    /**
     * Avalia o ciclo circadiano, gerenciando janelas de sono que cruzam a meia-noite (ex: 22h às 06h).
     *
     * @param eventTime Horário exato extraído do payload do evento IoT.
     * @return true caso o evento ocorra dentro da janela predefinida como noturna.
     */
    private boolean isNightTime(LocalTime eventTime) {
        if (thresholds.nightStartTime().isAfter(thresholds.nightEndTime())) {
            return !eventTime.isBefore(thresholds.nightStartTime()) || !eventTime.isAfter(thresholds.nightEndTime());
        }
        return !eventTime.isBefore(thresholds.nightStartTime()) && !eventTime.isAfter(thresholds.nightEndTime());
    }


    private void resetNightTrackers() {
        bedAbsenceTracker.remove(PERSONA_ID);
        routine2AlreadyTriggered = false;
    }
}
