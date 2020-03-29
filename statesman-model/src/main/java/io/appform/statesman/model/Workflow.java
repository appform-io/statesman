package io.appform.statesman.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;

/**
 *
 */
@Value
public class Workflow {
    String id;
    String name;
    DataObject dataObject;
    JsonNode attributes;
}
