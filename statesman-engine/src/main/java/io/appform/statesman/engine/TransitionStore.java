package io.appform.statesman.engine;

import io.appform.statesman.model.StateTransition;

import java.util.List;
import java.util.Optional;

/**
 *
 */
public interface TransitionStore {
    Optional<StateTransition> save(String workflowTemplateId, String fromState, StateTransition stateTransition);
    List<StateTransition> getTransitionFor(String workflowTemplateId, String fromState);
    List<StateTransition> allTransitions(String workflowTemplateId);
}
