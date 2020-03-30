package io.appform.statesman.engine.observer.observers;

import com.google.inject.Inject;
import io.appform.statesman.engine.observer.ObservableEvent;
import io.appform.statesman.engine.observer.ObservableEventBusSubscriber;

import javax.inject.Singleton;

/**
 *
 */
@Singleton
public class FoxtrotEventSender extends ObservableEventBusSubscriber {

    @Inject
    public FoxtrotEventSender() {
        super(null);
    }

    @Override
    protected void handleEvent(ObservableEvent event) {
        //TODO
    }
}
