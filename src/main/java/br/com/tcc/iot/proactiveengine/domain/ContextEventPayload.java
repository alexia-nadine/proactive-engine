package br.com.tcc.iot.proactiveengine.domain;

import br.com.tcc.iot.proactiveengine.domain.enums.BedPressureStatus;
import br.com.tcc.iot.proactiveengine.domain.enums.DoorStatus;
import br.com.tcc.iot.proactiveengine.domain.enums.UserPosture;

import java.time.LocalTime;

public class ContextEventPayload {

    LocalTime timeOfDay;
    UserPosture userPosture;
    String roomLocation;
    DoorStatus doorStatus;
    BedPressureStatus bedPressureStatus;
    Boolean presenceDetected;
    Integer luminosityLux;

    public Integer getLuminosityLux() {
        return luminosityLux;
    }

    public Boolean getPresenceDetected() {
        return presenceDetected;
    }

    public BedPressureStatus getBedPressureStatus() {
        return bedPressureStatus;
    }

    public DoorStatus getDoorStatus() {
        return doorStatus;
    }

    public String getRoomLocation() {
        return roomLocation;
    }

    public UserPosture getUserPosture() {
        return userPosture;
    }

    public LocalTime getTimeOfDay() {
        return timeOfDay;
    }
}
