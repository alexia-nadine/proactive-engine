package br.com.tcc.iot.proactiveengine.application.ports.output;

/**
 * Porta de Saída responsável pela observabilidade e coleta de métricas de negócio.
 * Desacopla o domínio de ferramentas específicas de monitoramento (Prometheus/Micrometer).
 */
public interface MetricsPort {

    /**
     * Registra o incremento de uma ação proativa bem-sucedida, categorizando os alívios gerados.
     */
    void incrementProactiveAction(String routine, String action, String room, String posture, String physicalRelief, String cognitiveRelief);

    /**
     * Registra eventos recebidos fora da janela de atuação (eventos diurnos descartados).
     */
    void incrementIgnoredDaytimeEvent();
}
