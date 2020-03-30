package io.appform.statesman.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataObject {
    private JsonNode data;
    private State currentState;
    private Date created;
    private Date updated;
}
