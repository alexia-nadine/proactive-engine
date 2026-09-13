package br.com.tcc.iot.proactiveengine.infrastructure.adapters.outbound;

import br.com.tcc.iot.proactiveengine.application.ports.output.ActionTriggerPort;
import org.springframework.stereotype.Component;

@Component
public class ConsoleActionTriggerAdapter implements ActionTriggerPort {
    @Override
    public void turnOnPathLights() {
        // Simulando a integração de hardware
        System.out.println("=====================================================");
        System.out.println("[INFRA ADAPTER] Comando enviado: LIGAR LUZES DE ROTA.");
        System.out.println("[INFRA ADAPTER] Motivo: Prevenção de colisão (AAL).");
        System.out.println("=====================================================");
    }

    @Override
    public void triggerSecurityAlert() {
        // Simulando o envio de um push notification ou alarme sonoro
        System.out.println("=====================================================");
        System.out.println("[INFRA ADAPTER] Comando enviado: TRACAR PORTAS.");
        System.out.println("[INFRA ADAPTER] Motivo: Risco de evasão noturna.");
        System.out.println("=====================================================");
    }
}
