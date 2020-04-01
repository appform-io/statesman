package io.appform.statesman.server.requests;

import lombok.Value;

/**
 *
 */
@Value
public class IngressCallback {
    String id;
    String queryString;
    String apiPath;
}
