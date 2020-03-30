package io.appform.statesman.publisher.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

/**
 * @author shashank.g
 */
@Data
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Event {
    private EventType type;
    //for tracing - if null, uuid will be used
    private String id;
    private JsonNode data;
}
