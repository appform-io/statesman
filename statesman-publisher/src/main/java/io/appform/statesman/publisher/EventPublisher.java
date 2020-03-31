package io.appform.statesman.publisher;

import io.appform.statesman.publisher.model.Event;

import java.util.List;

/**
 * @author shashank.g
 */
public interface EventPublisher {

    void start() throws Exception;

    void stop() throws Exception;

    void publish(final Event event) throws Exception;

    void publish(final String topic, final List<Event> events) throws Exception;
}
