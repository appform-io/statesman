package io.appform.statesman.engine.action.impl;

import io.appform.statesman.engine.action.BaseAction;
import io.appform.statesman.engine.handlebars.HandleBarsService;
import io.appform.statesman.model.ActionImplementation;
import io.appform.statesman.model.Workflow;
import io.appform.statesman.model.action.data.impl.HttpActionData;
import io.appform.statesman.model.action.template.HttpActionTemplate;
import lombok.Data;

import javax.inject.Inject;
import javax.inject.Singleton;

@Data
@Singleton
@ActionImplementation(name = "HTTP")
public class HttpAction extends BaseAction<HttpActionData, HttpActionTemplate> {

    private HandleBarsService handleBarsService;

    @Inject
    public HttpAction(HandleBarsService handleBarsService) {
        this.handleBarsService = handleBarsService;
    }

    @Override
    public void handle(HttpActionData actionData) {

    }

    @Override
    public HttpActionData transformPayload(Workflow workflow, HttpActionTemplate actionTemplate) {
        return HttpActionData.builder()
                .method(actionTemplate.getMethod())
                .url(handleBarsService.transform(actionTemplate.getUrl(), workflow))
                .headers(handleBarsService.transform(actionTemplate.getHeaders(), workflow))
                .payload(handleBarsService.transform(actionTemplate.getPayload(), workflow))
                .build();
    }

}
