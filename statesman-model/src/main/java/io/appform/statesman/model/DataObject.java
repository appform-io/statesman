package io.appform.statesman.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;

import java.util.Date;

/**
 *
 */
@Value
public class DataObject {
    JsonNode data;
    State currentState;
    Date created;
    Date updated;
}
