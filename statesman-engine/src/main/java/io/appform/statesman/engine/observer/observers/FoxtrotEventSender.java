package io.appform.statesman.engine.observer.observers;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.appform.statesman.engine.observer.ObservableEvent;
import io.appform.statesman.engine.observer.ObservableEventBusSubscriber;
import io.appform.statesman.engine.observer.ObservableEventVisitor;
import io.appform.statesman.engine.observer.events.StateTransitionEvent;
import io.appform.statesman.model.exception.StatesmanError;
import io.appform.statesman.publisher.EventPublisher;
import io.appform.statesman.publisher.model.Event;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 *
 */
@Slf4j
@Singleton
public class FoxtrotEventSender extends ObservableEventBusSubscriber {

    private static final String REPORTING = "statesman-reporting";
    private static final String APP_NAME = "reporting";
    private EventPublisher publisher;

    @Inject
    public FoxtrotEventSender(@Named("eventPublisher") final EventPublisher publisher) {
        super(null);
        this.publisher = publisher;
    }

    @Override
    protected void handleEvent(ObservableEvent event) {
        final List<Event> eventList = event.accept(new ObservableEventVisitor<List<Event>>() {
            @Override
            public List<Event> visit(StateTransitionEvent stateTransitionEvent) {

                //TODO: put the app level event here
                //this is test event
                return Lists.newArrayList(Event.builder()
                        .topic(REPORTING)
                        .groupingKey(UUID.randomUUID().toString())
                        .app(APP_NAME)
                        .time(new Date())
                        .partitionKey(UUID.randomUUID().toString())
                        .eventType("test")
                        .eventData(event)
                        .build());
            }
        });

        //publish
        publish(eventList);
    }

    private void publish(final List<Event> eventList) {
        try {
            if (null != eventList) {
                publisher.publish(REPORTING, eventList);
            }
        } catch (final Exception e) {
            log.error("unable to send event", e);
            throw StatesmanError.propagate(e);
        }
    }
}
