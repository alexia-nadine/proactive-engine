package br.com.tcc.iot.proactiveengine.application.ports.output;

public interface ActionTriggerPort {

    // Ação para ajudar na mobilidade no escuro
    void turnOnPathLights();

    // Ação para prevenção de evasão/risco
    void triggerSecurityAlert();
}
