package io.appform.statesman.engine.action.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import io.appform.statesman.engine.action.BaseAction;
import io.appform.statesman.engine.handlebars.HandleBarsService;
import io.appform.statesman.model.ActionImplementation;
import io.appform.statesman.model.Workflow;
import io.appform.statesman.model.action.data.impl.HttpActionData;
import io.appform.statesman.model.action.data.impl.HttpMethod;
import io.appform.statesman.model.action.template.HttpActionTemplate;
import io.appform.statesman.model.exception.StatesmanError;
import io.appform.statesman.publisher.http.HttpClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

@Slf4j
@Data
@Singleton
@ActionImplementation(name = "HTTP")
public class HttpAction extends BaseAction<HttpActionData, HttpActionTemplate> {

    private HandleBarsService handleBarsService;
    private HttpClient client;
    private ObjectMapper mapper;

    @Inject
    public HttpAction(HandleBarsService handleBarsService,
                      HttpClient client,
                      ObjectMapper mapper) {
        this.handleBarsService = handleBarsService;
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public void handle(HttpActionData actionData) {
        try {
            actionData.getMethod().visit(new HttpMethod.MethodTypeVisitor<Void>() {
                @Override
                public Void visitPost() throws Exception {
                    final Response response = client.post(actionData.getUrl(),
                            actionData.getPayload(),
                            actionData.getHeaders());
                    if (!response.isSuccessful()) {
                        log.error("unable to do post action, actionData: {}", actionData);
                        throw new StatesmanError();
                    }
                    return null;
                }

                @Override
                public Void visitGet() throws Exception {
                    final Response response = client.get(actionData.getUrl(),
                            actionData.getHeaders());
                    if (!response.isSuccessful()) {
                        log.error("unable to do get action, actionData: {}", actionData);
                        throw new StatesmanError();
                    }
                    return null;
                }
            });
        } catch (final Exception e) {
            throw StatesmanError.propagate(e);
        }
    }

    @Override
    public HttpActionData transformPayload(Workflow workflow, HttpActionTemplate actionTemplate) {
        return HttpActionData.builder()
                .method(HttpMethod.valueOf(actionTemplate.getMethod()))
                .url(handleBarsService.transform(actionTemplate.getUrl(), workflow))
                .headers(getheaders(workflow, actionTemplate.getHeaders()))
                .payload(handleBarsService.transform(actionTemplate.getPayload(), workflow))
                .build();
    }

    //assuming the header string in below format
    //headerStr = "key1:value1,key2:value2"
    private Map<String, String> getheaders(Workflow workflow, String headers) {
        return Splitter.on(",")
                .withKeyValueSeparator(":")
                .split(handleBarsService.transform(headers, workflow));
    }

}
