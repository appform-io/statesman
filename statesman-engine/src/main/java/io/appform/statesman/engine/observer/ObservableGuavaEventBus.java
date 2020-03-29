package io.appform.statesman.engine.observer;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.List;

/**
 *
 */
@Singleton
public class ObservableGuavaEventBus implements ObservableEventBus {
    private final EventBus syncEventBus;
    private final Provider<List<ObservableEventBusSubscriber>> subscribers;

    @Inject
    public ObservableGuavaEventBus(final Provider<List<ObservableEventBusSubscriber>> subscribers) {
        this.subscribers = subscribers;
        this.syncEventBus = new EventBus("events");
    }

    @Override
    public void publish(ObservableEvent event) {
        syncEventBus.post(event);
    }

    @Subscribe
    public void handleEvent(ObservableEvent event) {
        subscribers.get()
                .forEach(subscriber -> subscriber.handle(event));
    }
}
