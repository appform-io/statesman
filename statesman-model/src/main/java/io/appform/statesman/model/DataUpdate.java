package io.appform.statesman.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.appform.statesman.model.dataaction.DataAction;
import lombok.Value;

/**
 *
 */
@Value
public class DataUpdate {
    String workflowId;
    JsonNode data;
    DataAction dataAction;
}
