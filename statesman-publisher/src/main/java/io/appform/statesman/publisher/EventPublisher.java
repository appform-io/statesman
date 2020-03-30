package io.appform.statesman.publisher;

import io.appform.statesman.publisher.model.Event;
import io.appform.statesman.publisher.model.EventType;

import java.util.List;

/**
 * @author shashank.g
 */
public interface EventPublisher {

    void publish(final Event event, final EventType type);

    void publish(final List<Event> events, final EventType type);

    void publish(final Event event, final String topic);

    void publish(final List<Event> events, final String topic);
}
