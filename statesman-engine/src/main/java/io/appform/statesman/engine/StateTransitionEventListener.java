package io.appform.statesman.engine;

import io.appform.statesman.model.*;

/**
 *
 */
public interface StateTransitionEventListener {
    void preStateUpdate(
            Workflow workflow,
            WorkflowTemplate template,
            DataUpdate dataUpdate,
            StateTransition selectedTransition);

    void postStateUpdate(
            Workflow workflow,
            WorkflowTemplate template,
            DataUpdate dataUpdate,
            StateTransition selectedTransition);
}
