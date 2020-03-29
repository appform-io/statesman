package io.appform.statesman.engine.action.impl;

import io.appform.statesman.engine.action.BaseAction;
import io.appform.statesman.model.ActionImplementation;
import io.appform.statesman.model.Workflow;
import io.appform.statesman.model.action.data.impl.HttpActionData;
import io.appform.statesman.model.action.template.ActionTemplate;
import lombok.Data;

@Data
@ActionImplementation(name = "HTTP")
public class HttpAction extends BaseAction<HttpActionData> {

    @Override
    public void handle(HttpActionData actionData) {

    }

    @Override
    public HttpActionData transformPayload(Workflow workflow, ActionTemplate actionTemplate) {
        return null;
    }

}
