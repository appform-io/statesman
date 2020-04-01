package io.appform.statesman.engine.events;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Value;

/**
 *
 */
@Value
@Builder
public class StateChangeEvent {
    String workflowId; //This should be set as grouping key SHASHANK
    String workflowTemplateId;
    String oldState;
    String newState;
    boolean terminal;
    JsonNode data;
    JsonNode update;
    String appliedAction;
}
