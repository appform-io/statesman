package io.appform.statesman.publisher;

import io.appform.statesman.publisher.model.Event;

import java.util.List;

/**
 * @author shashank.g
 */
public interface EventPublisher {

    void publish(final Event event);

    void publish(final String topic, final List<Event> events);
}
