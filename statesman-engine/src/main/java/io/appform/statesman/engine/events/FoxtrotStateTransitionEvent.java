package io.appform.statesman.engine.events;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Value;

/**
 *
 */
@Value
@Builder
public class FoxtrotStateTransitionEvent {
    String workflowId;
    String workflowTemplateId;
    String oldState;
    String newState;
    boolean terminal;
    JsonNode data;
    JsonNode update;
    String appliedAction;
}
