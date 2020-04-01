package io.appform.statesman.engine.observer;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public abstract class ObservableEventBusSubscriber {
    private final ObservableEventBusSubscriber next;

    protected ObservableEventBusSubscriber(ObservableEventBusSubscriber next) {
        this.next = next;
    }

    public final void handle(ObservableEvent event) {
        try {
            handleEvent(event);
        } catch (Exception e) {
            log.error("Event handler error", e);
        }
        if(null != next) {
            next.handle(event);
        }
    }

    protected abstract void handleEvent(ObservableEvent event);
}
