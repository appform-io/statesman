package io.appform.statesman.engine.observer;

import io.appform.statesman.engine.observer.events.StateTransitionEvent;
import io.appform.statesman.engine.observer.events.WorkflowInitEvent;

/**
 *
 */
public interface ObservableEventVisitor<T> {

    T visit(StateTransitionEvent stateTransitionEvent);

    T visit(WorkflowInitEvent workflowInitEvent);
}
