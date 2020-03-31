package io.appform.statesman.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HttpClientConfiguration {

    @Min(10L)
    @Max(1024L)
    private int connections = 10;

    @Max(86400L)
    private int idleTimeOutSeconds = 30;

    @Max(86400000L)
    private int connectTimeoutMs = 10000;

    @Max(86400000L)
    private int opTimeoutMs = 10000;
}
