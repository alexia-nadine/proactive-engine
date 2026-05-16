package br.com.tcc.iot.proactiveengine.domain;

import br.com.tcc.iot.proactiveengine.domain.enums.BedPressureStatus;
import br.com.tcc.iot.proactiveengine.domain.enums.DoorStatus;
import br.com.tcc.iot.proactiveengine.domain.enums.UserPosture;

import java.time.LocalTime;

public record ContextEventPayload(
        LocalTime timeOfDay,
        UserPosture userPosture,
        String roomLocation,
        DoorStatus doorStatus,
        BedPressureStatus bedPressureStatus,
        Boolean presenceDetected,
        Integer luminosityLux
) {}