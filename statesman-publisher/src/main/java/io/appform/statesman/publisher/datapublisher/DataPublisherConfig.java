package io.appform.statesman.publisher.datapublisher;

import lombok.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * @author shashank.g
 */
@Data
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DataPublisherConfig {
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
}
