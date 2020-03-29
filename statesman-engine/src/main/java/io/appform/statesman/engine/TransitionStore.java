package io.appform.statesman.engine;

import io.appform.statesman.model.StateTransition;

import java.util.List;
import java.util.Optional;

/**
 *
 */
public interface TransitionStore {
    Optional<List<StateTransition>> getTransitionFor(String workflowTmplId, String fromState);
}
