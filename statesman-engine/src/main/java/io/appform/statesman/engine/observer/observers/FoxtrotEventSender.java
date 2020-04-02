package io.appform.statesman.engine.observer.observers;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.appform.statesman.engine.Constants;
import io.appform.statesman.engine.events.EngineEventType;
import io.appform.statesman.engine.events.FoxtrotStateTransitionEvent;
import io.appform.statesman.engine.observer.ObservableEvent;
import io.appform.statesman.engine.observer.ObservableEventBusSubscriber;
import io.appform.statesman.engine.observer.ObservableEventVisitor;
import io.appform.statesman.engine.observer.events.StateTransitionEvent;
import io.appform.statesman.model.exception.StatesmanError;
import io.appform.statesman.publisher.EventPublisher;
import io.appform.statesman.publisher.model.Event;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 *
 */
@Slf4j
@Singleton
public class FoxtrotEventSender extends ObservableEventBusSubscriber {


    private static final EventTranslator EVENT_TRANSLATOR = new EventTranslator();

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

                return stateTransitionEvent.accept(EVENT_TRANSLATOR);
            }
        });

        //publish
        publish(eventList);
    }

    private void publish(final List<Event> eventList) {
        try {
            if (null != eventList && !eventList.isEmpty()) {
                publisher.publish(Constants.FOXTROT_REPORTING_TOPIC, eventList);
            }
        }
        catch (final Exception e) {
            log.error("unable to send event", e);
            throw StatesmanError.propagate(e);
        }
    }

    private static final class EventTranslator implements ObservableEventVisitor<List<Event>> {

        @Override
        public List<Event> visit(StateTransitionEvent stateTransitionEvent) {
            return Collections.singletonList(
                    Event.builder()
                            .id(UUID.randomUUID().toString())
                            .topic(Constants.FOXTROT_REPORTING_TOPIC)
                            .app(Constants.FOXTROT_APP_NAME)
                            .eventType(EngineEventType.STATE_CHANGED.name())
                            .groupingKey(stateTransitionEvent.getWorkflow().getId())
                            .partitionKey(stateTransitionEvent.getWorkflow().getId())
                            .time(new Date())
                            .eventSchemaVersion("v1")
                            .eventData(FoxtrotStateTransitionEvent.builder()
                                               .workflowId(stateTransitionEvent.getWorkflow().getId())
                                               .workflowTemplateId(stateTransitionEvent.getWorkflow().getTemplateId())
                                               .oldState(stateTransitionEvent.getOldState().getName())
                                               .newState(stateTransitionEvent.getWorkflow().getDataObject().getCurrentState().getName())
                                               .terminal(stateTransitionEvent.getWorkflow().getDataObject().getCurrentState().isTerminal())
                                               .data(stateTransitionEvent.getWorkflow().getDataObject().getData())
                                               .update(stateTransitionEvent.getUpdate().getData())
                                               .appliedAction(stateTransitionEvent.getTransition().getAction())
                                               .elapseTime(System.currentTimeMillis() - stateTransitionEvent.getWorkflow().getCreated().getTime())
                                               .build())
                            .build());
        }
    }
}
