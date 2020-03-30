package io.appform.statesman.model.action.data.impl;

import io.appform.statesman.model.action.ActionType;
import io.appform.statesman.model.action.data.ActionData;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
public class HttpActionData extends ActionData {

    private HttpMethod method;
    private String url;
    private String payload;
    private Map<String, String> headers;

    public HttpActionData() {
        super(ActionType.HTTP);
    }

    @Builder
    public HttpActionData(HttpMethod method, String url, String payload, Map<String, String> headers) {
        this();
        this.method = method;
        this.url = url;
        this.payload = payload;
        this.headers = headers;
    }

}
