package br.com.tcc.iot.proactiveengine.application.ports.output;

/**
 * Porta de Saída responsável por disparar ações físicas no ambiente IoT.
 * Abstrai a comunicação com atuadores reais (lâmpadas, fechaduras inteligentes).
 */
public interface ActionTriggerPort {

    /**
     * Aciona a iluminação de rotas de circulação acessíveis (ex: rodapés).
     * Utilizado na Rotina de "Deslocamento Noturno Seguro" para guiar o cadeirante durante deslocamentos noturnos,
     * reduzindo o risco de colisões com obstáculos.
     */
    void turnOnPathLights();

    /**
     * Executa rotinas de segurança residencial, como o trancamento automático de portas
     * e o desligamento de luzes principais.
     */
    void triggerSecurityAlert();
}
