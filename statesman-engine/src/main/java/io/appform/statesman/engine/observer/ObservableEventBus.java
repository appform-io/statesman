package io.appform.statesman.engine.observer;

/**
 *
 */
public interface ObservableEventBus {
    void publish(final ObservableEvent event);
}
