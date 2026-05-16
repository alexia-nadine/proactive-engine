package br.com.tcc.iot.proactiveengine.application.services;

import br.com.tcc.iot.proactiveengine.application.ports.output.ActionTriggerPort;
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
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ProactiveDecisionServiceTest {

    @Mock
    private ActionTriggerPort actionTriggerPort;

    private ProactiveDecisionService decisionService;

    @BeforeEach
    void setUp() {
        ProactiveRulesProperties thresholds = new ProactiveRulesProperties(
                LocalTime.of(21, 0),  // nightStartTime
                LocalTime.of(5, 0),   // nightEndTime
                20,                   // minSafeLuminosity
                20                    // maxBathroomDurationMinutes
        );

        // Instanciamos o motor injetando as regras e o mock da porta de saída
        decisionService = new ProactiveDecisionService(thresholds, actionTriggerPort);
    }

    //ROTINA 1: BOA NOITE AUToNOMA

    @Test
    @DisplayName("Gatilho Ideal: Deve trancar a casa quando o utilizador deitar de madrugada")
    void deveDispararRotina1_QuandoUsuarioPCDDeitarDeMadrugadaComPortaDestrancada() {
        ContextEventPayload payload = new ContextEventPayload(
                LocalTime.of(23, 0), UserPosture.LYING_DOWN, RoomLocation.BEDROOM,
                DoorStatus.UNLOCKED, BedPressureStatus.OCCUPIED, true, 50
        );

        decisionService.evaluate(payload);

        // Verifica se a porta de saída foi chamada exatamente 1 vez com a instrução de trancar
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

        // Verifica que o motor ignorou a ação
        Mockito.verify(actionTriggerPort, never()).triggerSecurityAlert();
    }


    @Test
    @DisplayName("Falso Positivo: Não deve agir se deitar no sofá da sala")
    void naoDeveDispararRotina1_QuandoUsuarioDeitarNaSala() {
        ContextEventPayload payload = new ContextEventPayload(
                LocalTime.of(22, 0), UserPosture.LYING_DOWN, RoomLocation.LIVING_ROOM,
                DoorStatus.UNLOCKED, BedPressureStatus.UNOCCUPIED, true, 100
        );

        decisionService.evaluate(payload);

        Mockito.verify(actionTriggerPort, never()).triggerSecurityAlert();
    }

    @Test
    @DisplayName("Eficiência: Não deve tentar trancar uma casa já trancada")
    void naoDeveDispararRotina1_SePortaJaEstiverTrancada() {
        ContextEventPayload payload = new ContextEventPayload(
                LocalTime.of(22, 30), UserPosture.LYING_DOWN, RoomLocation.BEDROOM,
                DoorStatus.LOCKED, BedPressureStatus.OCCUPIED, true, 50
        );

        decisionService.evaluate(payload);

        Mockito.verify(actionTriggerPort, never()).triggerSecurityAlert();
    }

    // ==========================================
    // TESTES DA ROTINA 2: DESLOCAMENTO SEGURO
    // ==========================================

    @Test
    @DisplayName("Gatilho Ideal: Deve acender caminho de luz ao levantar no escuro de madrugada")
    void deveDispararRotina2_QuandoLevantarNoEscuroNaMadrugada() {
        ContextEventPayload payload = new ContextEventPayload(
                LocalTime.of(3, 0), UserPosture.SITTING, RoomLocation.BEDROOM,
                DoorStatus.LOCKED, BedPressureStatus.UNOCCUPIED, true, 10 // 10 lux < 20 lux
        );

        decisionService.evaluate(payload);

        Mockito.verify(actionTriggerPort, times(1)).turnOnPathLights();
    }

    @Test
    @DisplayName("Falso Positivo: Não deve acender luzes de rodapé se o teto já estiver aceso")
    void naoDeveDispararRotina2_SeLevantarDeMadrugadaComLuzAcesa() {
        ContextEventPayload payload = new ContextEventPayload(
                LocalTime.of(3, 0), UserPosture.STANDING, RoomLocation.BEDROOM,
                DoorStatus.LOCKED, BedPressureStatus.UNOCCUPIED, true, 150 // 150 lux > 20 lux
        );

        decisionService.evaluate(payload);

        Mockito.verify(actionTriggerPort, never()).turnOnPathLights();
    }

    @Test
    @DisplayName("Falso Positivo: Não deve agir se o utilizador apenas se mexer na cama")
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

        Mockito.verify(actionTriggerPort, never()).turnOnPathLights();
    }

    @Test
    @DisplayName("Fronteira: Deve disparar Rotina 1 exatamente no minuto de início do limiar noturno - 21:00")
    void deveDispararRotina1_ExatamenteAs21Horas() {
        ContextEventPayload payload = new ContextEventPayload(
                LocalTime.of(21, 0), UserPosture.LYING_DOWN, RoomLocation.BEDROOM,
                DoorStatus.UNLOCKED, BedPressureStatus.OCCUPIED, true, 50
        );
        decisionService.evaluate(payload);
        Mockito.verify(actionTriggerPort, times(1)).triggerSecurityAlert();
    }

    @Test
    @DisplayName("Fronteira: Não deve disparar proatividade exatamente um minuto após o fim da noite - 05:01")
    void naoDeveDispararRotina2_AsCincoEUmDaManha() {
        ContextEventPayload payload = new ContextEventPayload(
                LocalTime.of(5, 1), UserPosture.SITTING, RoomLocation.BEDROOM,
                DoorStatus.LOCKED, BedPressureStatus.UNOCCUPIED, true, 10
        );
        decisionService.evaluate(payload);
        Mockito.verify(actionTriggerPort, never()).turnOnPathLights();
    }

    @Test
    @DisplayName("Fronteira: Não deve acender a luz se a luminosidade for exatamente igual ao limiar seguro (20 lux)")
    void naoDeveDispararRotina2_ComExatos20Lux() {
        ContextEventPayload payload = new ContextEventPayload(
                LocalTime.of(3, 0), UserPosture.SITTING, RoomLocation.BEDROOM,
                DoorStatus.LOCKED, BedPressureStatus.UNOCCUPIED, true, 20 // Exatamente 20
        );
        decisionService.evaluate(payload);
        Mockito.verify(actionTriggerPort, never()).turnOnPathLights();
    }

}
