package br.com.tcc.iot.proactiveengine.application.ports.input;

import br.com.tcc.iot.proactiveengine.domain.ContextEventPayload;

/**
 * Porta de Entrada que define o Caso de Uso principal do Motor de Decisão Proativo.
 * <p>
 * O objetivo deste contrato é atuar como a inteligência do sistema IoT, analisando
 * eventos contextuais em tempo real (postura, localização, luminosidade) para mitigar
 * a carga cognitiva e o esforço físico de pessoas com mobilidade reduzida.
 * </p>
 */
public interface EvaluateRoutineUseCase {

    /**
     * Avalia o contexto atual do ambiente e decide, de forma autônoma, se alguma
     * rotina proativa de segurança ou acessibilidade deve ser engatilhada.
     * <p>
     * As regras avaliadas contemplam filtros temporais (falsos positivos) e o
     * alinhamento com o ciclo circadiano do usuário (janelas de repouso).
     * </p>
     *
     * @param payload Objeto de domínio contendo o retrato do ambiente IoT no momento do evento
     * (pressão da cama, luminosidade, portas).
     */
    void evaluate(ContextEventPayload payload);
}
