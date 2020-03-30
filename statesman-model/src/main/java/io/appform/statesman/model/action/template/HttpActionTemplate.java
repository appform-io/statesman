package io.appform.statesman.model.action.template;

import io.appform.statesman.model.action.ActionType;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

@Data
public class HttpActionTemplate extends ActionTemplate {

    @NotNull
    @NotEmpty
    private String method;

    @NotNull
    @NotEmpty
    private String url;

    private String payload;

    private String headers;

    public HttpActionTemplate() {
        super(ActionType.HTTP);
    }

    @Builder
    public HttpActionTemplate(String templateId, String name, boolean active, String method, String url, String payload, String headers) {
        super(ActionType.HTTP, templateId, name, active);
        this.method = method;
        this.url = url;
        this.payload = payload;
        this.headers = headers;
    }

}
