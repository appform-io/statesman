package io.appform.statesman.engine.observer.observers;

import io.appform.statesman.engine.observer.ObservableEvent;
import io.appform.statesman.engine.observer.ObservableEventBusSubscriber;

/**
 *
 */
public class FoxtrotEventSender extends ObservableEventBusSubscriber {

    public FoxtrotEventSender() {
        super(null);
    }

    @Override
    protected void handleEvent(ObservableEvent event) {
        //TODO
    }
}
