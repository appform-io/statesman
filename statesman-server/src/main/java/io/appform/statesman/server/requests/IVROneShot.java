package io.appform.statesman.server.requests;

import lombok.Value;

/**
 *
 */
@Value
public class IVROneShot {
    String id;
    String queryString;
    String apiPath;
}
