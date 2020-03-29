package io.appform.statesman.model.action.template;

import io.appform.statesman.model.action.ActionType;
import lombok.Builder;
import lombok.Data;

@Data
public class HttpActionTemplate extends ActionTemplate {

    private String method;
    private String url;
    private String payload;
    private String headers;

    public HttpActionTemplate() {
        super(ActionType.HTTP);
    }

    @Builder
    public HttpActionTemplate(boolean sync, String method, String url, String payload, String headers) {
        super(ActionType.HTTP, sync);
        this.method = method;
        this.url = url;
        this.payload = payload;
        this.headers = headers;
    }

}
