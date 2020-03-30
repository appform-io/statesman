package io.appform.statesman.publisher.impl;

import com.google.common.collect.Maps;
import lombok.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Map;

/**
 * @author shashank.g
 */
@Data
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventPublisherConfig {
    @Min(10L)
    @Max(1024L)
    private int connections = 10;
    @Max(86400L)
    private int idleTimeOutSeconds = 30;
    @Max(86400000L)
    private int connectTimeoutMs = 10000;
    @Max(86400000L)
    private int opTimeoutMs = 10000;

    //serviceLevel
    private String endpoint = "http://localhost:8080/events";
    private Map<String, String> eventTopics = Maps.newHashMap();
}
