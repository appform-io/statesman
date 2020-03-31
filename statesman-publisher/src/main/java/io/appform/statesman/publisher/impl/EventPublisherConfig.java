package io.appform.statesman.publisher.impl;

import com.google.common.collect.Maps;
import io.appform.statesman.model.HttpClientConfiguration;
import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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


    @NotNull
    @Valid
    private HttpClientConfiguration httpClientConfiguration;

    //serviceLevel
    private String endpoint = "http://localhost:8080/events";
    private Map<String, String> eventTopics = Maps.newHashMap();
}
