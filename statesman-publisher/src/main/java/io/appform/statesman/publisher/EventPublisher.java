package io.appform.statesman.publisher;

import io.appform.statesman.publisher.model.Event;

/**
 * @author shashank.g
 */
public interface EventPublisher {

    void publish(final Event event);

    void publish(final Event event, final String topic);
}
