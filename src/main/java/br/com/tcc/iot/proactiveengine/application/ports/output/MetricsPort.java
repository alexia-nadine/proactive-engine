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
    void incrementDaytimeEventIgnored();

    default void incrementIgnoredDaytimeEvent() {
        incrementDaytimeEventIgnored();
    }

    /**
     * Registra falso positivo associado ao alívio de pressão da cama.
     */
    void incrementFalsePositiveEvent();

    default void incrementBedPressureReliefFilteredEvent() {
        incrementFalsePositiveEvent();
    }

    /**
     * Incrementa o contador de eventos consumidos com sucesso no RabbitMQ.
     */
    void incrementRabbitMqConsumedEvent();

    /**
     * Registra falhas de consumo de eventos no RabbitMQ.
     */
    void incrementRabbitMqConsumptionError();

    /**
     * Registra a latência de processamento em milissegundos.
     */
    void recordProcessingLatencyInMilliseconds(long latencyInMilliseconds);

    /**
     * Incrementa o contador de interações manuais evitadas por tipo.
     */
    void incrementAvoidedManualInteraction(String interactionType);

    /**
     * Registra cenários noturnos em que a decisão não gerou ação.
     */
    void incrementDecisionNoAction();

    /**
     * Registra a ocorrência de decisão proativa efetiva.
     */
    void incrementProactiveDecision();
}
