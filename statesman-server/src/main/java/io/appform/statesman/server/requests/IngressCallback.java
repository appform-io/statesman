package io.appform.statesman.server.requests;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;

/**
 *
 */
@Value
public class IngressCallback {
    String id;
    String queryString;
    String apiPath;
    JsonNode body;
}
