package br.com.tcc.iot.proactiveengine.application.ports.input;

import java.time.LocalTime;

/**
 * Contrato de configuração de regras utilizadas pelo motor de decisão proativo.
 * <p>
 * Esta interface representa os parâmetros de negócio que orientam a avaliação
 * de contexto do ambiente IoT, como janela noturna, luminosidade mínima segura
 * e tempo de confirmação para ausência de pressão na cama.
 * </p>
 */
public interface ProactiveRulesConfig {

    /**
     * Define o horário inicial da janela noturna de monitoramento.
     * <p>
     * A partir deste horário, o motor passa a considerar eventos como candidatos
     * à aplicação das rotinas proativas de segurança e acessibilidade.
     * </p>
     *
     * @return horário de início do período noturno.
     */
    LocalTime nightStartTime();

    /**
     * Define o horário final da janela noturna de monitoramento.
     * <p>
     * Antes ou após este limite, conforme a regra configurada, os eventos podem
     * ser tratados como fora do escopo de atuação do motor.
     * </p>
     *
     * @return horário de término do período noturno.
     */
    LocalTime nightEndTime();

    /**
     * Define a luminosidade mínima segura para deslocamento seguro.
     * <p>
     * Caso a luminosidade atual esteja abaixo deste limiar, o sistema pode acionar
     * a iluminação de rota para reduzir risco de colisão.
     * </p>
     *
     * @return valor mínimo aceitável de luminosidade, em lux.
     */
    Integer minSafeLuminosity();

    /**
     * Define o tempo mínimo de ausência de pressão na cama para caracterizar uma saída real do usuário.
     * <p>
     * Após este tempo, se não houver pressão na cama, o motor pode considerar
     * que o usuário saiu da cama e acionar rotinas proativas.
     * Esse intervalo ajuda a evitar falsos positivos devido a movimentos momentâneos ou ajustes de posição.
     * </p>
     *
     * @return tempo de confirmação da ausência de pressão na cama em segundos.
     */
    Integer bedAbsenceDelaySeconds();
}
