package io.appform.statesman.publisher.impl;

import io.appform.statesman.model.HttpClientConfiguration;
import io.appform.statesman.publisher.model.PublisherType;
import lombok.*;

import javax.validation.Valid;

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

    boolean disabled;

    @Valid
    private HttpClientConfiguration httpClientConfiguration;

    //serviceLevel
    private String endpoint = "http://localhost:8080/events";
    private PublisherType type = PublisherType.sync;
    private String queuePath = "/tmp";
    private int batchSize = 50;
}
