package br.com.tcc.iot.proactiveengine.infrastructure.config;

import br.com.tcc.iot.proactiveengine.application.ports.input.ProactiveRulesConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalTime;

/**
 * Implementação concreta do contrato de configuração de regras do motor de decisão.
 * <p>
 * Esta classe atua como adaptador de infraestrutura, recebendo os valores externos
 * definidos em arquivos de configuração da aplicação e expondo a camada de aplicação
 * por meio do contrato {@link ProactiveRulesConfig}.
 * </p>
 * <p>
 * Por ser um {@code record}, esta implementação é imutável, enxuta e orientada apenas
 * ao transporte de dados, o que reforça a separação entre configuração técnica e regra de negócio.
 * </p>
 */
@ConfigurationProperties(prefix = "proactive.rules.thresholds")
public record ProactiveRulesProperties(
        LocalTime nightStartTime,
        LocalTime nightEndTime,
        Integer minSafeLuminosity,
        Integer bedAbsenceDelaySeconds
) implements ProactiveRulesConfig {
}
