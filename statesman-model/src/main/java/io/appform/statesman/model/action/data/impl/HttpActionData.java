package io.appform.statesman.model.action.data.impl;

import io.appform.statesman.model.action.ActionType;
import io.appform.statesman.model.action.data.ActionData;
import lombok.Data;

@Data
public class HttpActionData extends ActionData {
    private String method;
    private String url;
    private String payload;
    private String headers;

    public HttpActionData() {
        super(ActionType.HTTP);
    }

    public HttpActionData(String method, String url, String payload, String headers) {
        this();
        this.method = method;
        this.url = url;
        this.payload = payload;
        this.headers = headers;
    }

}
