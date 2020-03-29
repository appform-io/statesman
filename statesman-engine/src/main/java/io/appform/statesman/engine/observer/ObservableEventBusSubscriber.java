package io.appform.statesman.engine.observer;

/**
 *
 */
public abstract class ObservableEventBusSubscriber {
    private final ObservableEventBusSubscriber next;

    protected ObservableEventBusSubscriber(ObservableEventBusSubscriber next) {
        this.next = next;
    }

    public final void handle(ObservableEvent event) {
        handleEvent(event);
        if(null != next) {
            next.handle(event);
        }
    }

    protected abstract void handleEvent(ObservableEvent event);
}
