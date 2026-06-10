package br.com.tcc.iot.proactiveengine.application.services;

import br.com.tcc.iot.proactiveengine.application.ports.output.ActionTriggerPort;
import br.com.tcc.iot.proactiveengine.application.ports.output.MetricsPort;
import br.com.tcc.iot.proactiveengine.domain.ContextEventPayload;
import br.com.tcc.iot.proactiveengine.domain.enums.BedPressureStatus;
import br.com.tcc.iot.proactiveengine.domain.enums.DoorStatus;
import br.com.tcc.iot.proactiveengine.domain.enums.RoomLocation;
import br.com.tcc.iot.proactiveengine.domain.enums.UserPosture;
import br.com.tcc.iot.proactiveengine.infrastructure.config.ProactiveRulesProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProactiveDecisionServiceTest {

    @Mock
    private ActionTriggerPort actionTriggerPort;

    @Mock
    private MetricsPort metricsPort;

    @Mock
    private ProactiveRulesProperties thresholds;

    @Mock
    private Clock clock;

    private ProactiveDecisionService decisionService;

    private Instant currentTime;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        decisionService = new ProactiveDecisionService(thresholds, actionTriggerPort, metricsPort, clock);

        lenient().when(thresholds.nightStartTime()).thenReturn(LocalTime.of(22, 0));
        lenient().when(thresholds.nightEndTime()).thenReturn(LocalTime.of(6, 0));
        lenient().when(thresholds.bedAbsenceDelaySeconds()).thenReturn(30);
        lenient().when(thresholds.minSafeLuminosity()).thenReturn(150);

        currentTime = Instant.parse("2026-01-01T03:00:00Z");
        lenient().when(clock.instant()).thenAnswer(invocation -> currentTime);
    }

    private void avancarTempo(long segundos) {
        currentTime = currentTime.plusSeconds(segundos);
    }

    //ROTINA 1: BOA NOITE AUTONOMA

    @Test
    @DisplayName("Gatilho Ideal: Deve trancar a casa quando o utilizador deitar de madrugada")
    void deveDispararRotina1_QuandoUsuarioPCDDeitarDeMadrugadaComPortaDestrancada() {
        ContextEventPayload payload = new ContextEventPayload(
                LocalTime.of(23, 0), UserPosture.LYING_DOWN, RoomLocation.BEDROOM,
                DoorStatus.UNLOCKED, BedPressureStatus.OCCUPIED, true, 50
        );

        decisionService.evaluate(payload);
        Mockito.verify(actionTriggerPort, times(1)).triggerSecurityAlert();
    }

    @Test
    @DisplayName("Falso Positivo: Não deve agir num cochilo da tarde")
    void naoDeveDispararRotina1_QuandoUsuarioDeitarDeTarde() {
        ContextEventPayload payload = new ContextEventPayload(
                LocalTime.of(15, 0), UserPosture.LYING_DOWN, RoomLocation.BEDROOM,
                DoorStatus.UNLOCKED, BedPressureStatus.OCCUPIED, true, 200
        );

        decisionService.evaluate(payload);
        Mockito.verify(actionTriggerPort, never()).triggerSecurityAlert();
    }

    @Test
    @DisplayName("Falso Positivo: Não deve agir se deitar no sofá da sala")
    void naoDeveDispararRotina1_QuandoUsuarioDeitarNaSala() {
        ContextEventPayload payload = new ContextEventPayload(
                LocalTime.of(22, 0), UserPosture.LYING_DOWN, RoomLocation.LIVING_ROOM,
                DoorStatus.UNLOCKED, BedPressureStatus.UNOCCUPIED, true, 160
        );

        decisionService.evaluate(payload);
        Mockito.verify(actionTriggerPort, never()).triggerSecurityAlert();
    }

    @Test
    @DisplayName("Eficiência: Não deve tentar trancar uma casa já trancada")
    void naoDeveDispararRotina1_SePortaJaEstiverTrancada() {
        ContextEventPayload payload = new ContextEventPayload(
                LocalTime.of(22, 30), UserPosture.LYING_DOWN, RoomLocation.BEDROOM,
                DoorStatus.LOCKED, BedPressureStatus.OCCUPIED, true, 160
        );

        decisionService.evaluate(payload);
        Mockito.verify(actionTriggerPort, never()).triggerSecurityAlert();
    }

    @Test
    @DisplayName("Fronteira: Deve disparar Rotina 1 exatamente no minuto de início do limiar noturno - 22:00")
    void deveDispararRotina1_ExatamenteAs22Horas() {
        ContextEventPayload payload = new ContextEventPayload(
                LocalTime.of(22, 0), UserPosture.LYING_DOWN, RoomLocation.BEDROOM,
                DoorStatus.UNLOCKED, BedPressureStatus.OCCUPIED, true, 160
        );
        decisionService.evaluate(payload);
        Mockito.verify(actionTriggerPort, times(1)).triggerSecurityAlert();
    }

    // TESTES DA ROTINA 2: DESLOCAMENTO NOTURNO SEGURO

    @Test
    void shouldTriggerRoutine2_WhenBedEmptySurpassesDelay() {
        when(thresholds.bedAbsenceDelaySeconds()).thenReturn(90);

        ContextEventPayload event = new ContextEventPayload(LocalTime.of(3, 0), UserPosture.SITTING, RoomLocation.BEDROOM, DoorStatus.LOCKED, BedPressureStatus.UNOCCUPIED, true, 10);

        decisionService.evaluate(event);
        verify(actionTriggerPort, never()).turnOnPathLights();

        avancarTempo(91);

        decisionService.evaluate(event);

        verify(actionTriggerPort, times(1)).turnOnPathLights();
    }

    @Test
    @DisplayName("Gatilho Ideal: Deve acender caminho de luz após 30s de ausência da cama com baixa luminosidade")
    void deveDispararRotina2_QuandoLevantarNoEscuroNaMadrugada() {
        ContextEventPayload payload = new ContextEventPayload(
                LocalTime.of(3, 0), UserPosture.SITTING, RoomLocation.BEDROOM,
                DoorStatus.LOCKED, BedPressureStatus.UNOCCUPIED, true, 10
        );
        decisionService.evaluate(payload);

        avancarTempo(31);

        decisionService.evaluate(payload);

        Mockito.verify(actionTriggerPort, times(1)).turnOnPathLights();
    }

    @Test
    @DisplayName("Filtro Clínico: Deve resetar o contador se o usuário voltar para a cama antes do delay")
    void deveResetarContadorSeUsuarioVoltarParaCama() {
        ContextEventPayload payloadAusencia = new ContextEventPayload(LocalTime.of(3, 0), UserPosture.SITTING, RoomLocation.BEDROOM, DoorStatus.LOCKED, BedPressureStatus.UNOCCUPIED, true, 10);
        decisionService.evaluate(payloadAusencia);

        avancarTempo(15);

        ContextEventPayload payloadRetorno = new ContextEventPayload(LocalTime.of(3, 0), UserPosture.LYING_DOWN, RoomLocation.BEDROOM, DoorStatus.LOCKED, BedPressureStatus.OCCUPIED, true, 10);
        decisionService.evaluate(payloadRetorno);

        avancarTempo(5);
        decisionService.evaluate(payloadAusencia);

        avancarTempo(16);
        decisionService.evaluate(payloadAusencia);

        Mockito.verify(actionTriggerPort, never()).turnOnPathLights();

        avancarTempo(15);
        decisionService.evaluate(payloadAusencia);
        Mockito.verify(actionTriggerPort, times(1)).turnOnPathLights();
    }

    @Test
    @DisplayName("Falso Positivo: Não deve acender luzes de rodapé se o teto já estiver aceso")
    void naoDeveDispararRotina2_SeLevantarDeMadrugadaComLuzAcesa() {
        ContextEventPayload payload = new ContextEventPayload(
                LocalTime.of(3, 0), UserPosture.STANDING, RoomLocation.BEDROOM,
                DoorStatus.LOCKED, BedPressureStatus.UNOCCUPIED, true, 150
        );
        decisionService.evaluate(payload);

        avancarTempo(35);
        decisionService.evaluate(payload);

        Mockito.verify(actionTriggerPort, never()).turnOnPathLights();
    }

    @Test
    @DisplayName("Falso Positivo: Não deve agir se o utilizador apenas se mexer na cama - ainda ocupada")
    void naoDeveDispararRotina2_SeHouverMovimentoMasCamaEstiverOcupada() {
        ContextEventPayload payload = new ContextEventPayload(
                LocalTime.of(2, 0), UserPosture.LYING_DOWN, RoomLocation.BEDROOM,
                DoorStatus.LOCKED, BedPressureStatus.OCCUPIED, true, 5
        );
        decisionService.evaluate(payload);
        Mockito.verify(actionTriggerPort, never()).turnOnPathLights();
    }

    @Test
    @DisplayName("Falso Positivo: Não deve agir num deslocamento normal de manhã")
    void naoDeveDispararRotina2_QuandoLevantarDeManha() {
        ContextEventPayload payload = new ContextEventPayload(
                LocalTime.of(10, 0), UserPosture.SITTING, RoomLocation.BEDROOM,
                DoorStatus.UNLOCKED, BedPressureStatus.UNOCCUPIED, true, 15
        );
        decisionService.evaluate(payload);
        avancarTempo(35);
        decisionService.evaluate(payload);
        Mockito.verify(actionTriggerPort, never()).turnOnPathLights();
    }

    @Test
    @DisplayName("Fronteira: Não deve disparar proatividade exatamente um minuto após o fim da noite - 05:01")
    void naoDeveDispararRotina2_AsSeisEUmDaManha() {
        ContextEventPayload payload = new ContextEventPayload(
                LocalTime.of(6, 1), UserPosture.SITTING, RoomLocation.BEDROOM,
                DoorStatus.LOCKED, BedPressureStatus.UNOCCUPIED, true, 10
        );
        decisionService.evaluate(payload);
        avancarTempo(35);
        decisionService.evaluate(payload);
        Mockito.verify(actionTriggerPort, never()).turnOnPathLights();
    }

    @Test
    @DisplayName("Fronteira: Não deve acender a luz se a luminosidade for exatamente igual ao limiar seguro (150 lux)")
    void naoDeveDispararRotina2_ComExatos150Lux() {
        ContextEventPayload payload = new ContextEventPayload(
                LocalTime.of(3, 0), UserPosture.SITTING, RoomLocation.BEDROOM,
                DoorStatus.LOCKED, BedPressureStatus.UNOCCUPIED, true, 150
        );
        decisionService.evaluate(payload);
        avancarTempo(35);
        decisionService.evaluate(payload);
        Mockito.verify(actionTriggerPort, never()).turnOnPathLights();
    }

}
