package io.appform.statesman.engine.observer;

import io.appform.statesman.engine.observer.events.StateTransitionEvent;

/**
 *
 */
public interface ObservableEventVisitor<T> {

    T visit(StateTransitionEvent stateTransitionEvent);
}
