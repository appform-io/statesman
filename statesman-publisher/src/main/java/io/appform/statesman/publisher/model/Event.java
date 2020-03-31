package io.appform.statesman.publisher.model;

import lombok.*;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.Date;

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

    @NotNull
    @NotEmpty
    private String app;
    @NotNull
    @NotEmpty
    private EventType eventType;
    @NotNull
    @NotEmpty
    private String id;
    @NotEmpty
    @NotNull
    private String groupingKey = "";
    private String partitionKey;
    private String eventSchemaVersion = "v1";
    private Date time = new Date();
    @NotNull
    private Object eventData;
}
