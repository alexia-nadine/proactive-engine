package br.com.tcc.iot.proactiveengine.application.ports.input;

import br.com.tcc.iot.proactiveengine.domain.ContextEventPayload;

public interface EvaluateRoutineUseCase {

    void evaluate(ContextEventPayload payload);
}
