package io.appform.statesman.server.publisher;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author shashank.g
 */
public interface DataPublisher {
    void publish(final JsonNode data);
}
