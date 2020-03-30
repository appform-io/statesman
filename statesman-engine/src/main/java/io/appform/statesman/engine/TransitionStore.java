package io.appform.statesman.engine;

import io.appform.statesman.model.StateTransition;

import java.util.List;
import java.util.Optional;

/**
 *
 */
public interface TransitionStore {

    Optional<StateTransition> create(String workflowTemplateId, StateTransition stateTransition);

    List<StateTransition> getTransitionFor(String workflowTemplateId, String fromState);

    List<StateTransition> getAllTransitions(String workflowTemplateId);

    List<StateTransition> update(String workflowTemplateId, StateTransition stateTransition);
}
