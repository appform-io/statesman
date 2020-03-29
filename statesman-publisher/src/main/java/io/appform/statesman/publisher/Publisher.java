package io.appform.statesman.publisher;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author shashank.g
 */
public interface Publisher {
    void publish(final JsonNode data);
}
