package io.appform.statesman.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 *
 */
@Data
@Builder
public class DataObject {
    private JsonNode data;
    private State currentState;
    private Date created;
    private Date updated;
}
