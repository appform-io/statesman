package io.appform.statesman.engine.action.impl;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import io.appform.statesman.engine.action.BaseAction;
import io.appform.statesman.engine.handlebars.HandleBarsService;
import io.appform.statesman.model.ActionImplementation;
import io.appform.statesman.model.HttpClientConfiguration;
import io.appform.statesman.model.Workflow;
import io.appform.statesman.model.action.ActionType;
import io.appform.statesman.model.action.template.HttpActionTemplate;
import io.appform.statesman.model.exception.StatesmanError;
import io.appform.statesman.publisher.EventPublisher;
import io.appform.statesman.publisher.http.HttpClient;
import io.appform.statesman.publisher.http.HttpUtil;
import io.appform.statesman.publisher.impl.SyncEventPublisher;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Data
@Singleton
@ActionImplementation(name = "HTTP")
public class HttpAction extends BaseAction<HttpActionTemplate> {

    private HandleBarsService handleBarsService;
    private HttpClient client;

    @Inject
    public HttpAction(HandleBarsService handleBarsService,
                      MetricRegistry registry,
                      @Named("httpActionDefaultConfig") HttpClientConfiguration config,
                      @Named("eventPublisher") final EventPublisher publisher,
                      ObjectMapper mapper) {
        super(publisher, mapper);
        this.client = new HttpClient(mapper, HttpUtil.defaultClient(SyncEventPublisher.class.getSimpleName(), registry, config));
        this.handleBarsService = handleBarsService;
    }

    @Override
    public ActionType getType() {
        return ActionType.HTTP;
    }
        
    @Override
    public void execute(HttpActionTemplate actionTemplate, Workflow workflow) {
        HttpActionData httpActionData = transformPayload(workflow, actionTemplate);
        handle(httpActionData);
    }


    private void handle(HttpActionData actionData) {
        try {
            actionData.getMethod().visit(new HttpMethod.MethodTypeVisitor<Void>() {
                @Override
                public Void visitPost() throws Exception {
                    log.info("HTTP_ACTION POST Call url:{}", actionData.getUrl());
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
                    log.info("HTTP_ACTION GET Call url:{}", actionData.getUrl());
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

    private HttpActionData transformPayload(Workflow workflow, HttpActionTemplate actionTemplate) {
        JsonNode jsonNode = mapper.valueToTree(workflow);
        return HttpActionData.builder()
                .method(HttpMethod.valueOf(actionTemplate.getMethod()))
                .url(handleBarsService.transform(actionTemplate.getUrl(), jsonNode))
                .headers(getheaders(jsonNode, actionTemplate.getHeaders()))
                .payload(handleBarsService.transform(actionTemplate.getPayload(), jsonNode))
                .build();
    }

    //assuming the header string in below format
    //headerStr = "key1:value1,key2:value2"
    private Map<String, String> getheaders(JsonNode workflow, String headers) {
        if (Strings.isNullOrEmpty(headers)) {
            return Collections.emptyMap();
        }
        return Splitter.on(",")
                .withKeyValueSeparator(":")
                .split(handleBarsService.transform(headers, workflow));
    }


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    private static class HttpActionData {

        private HttpMethod method;
        private String url;
        private String payload;
        private Map<String, String> headers;

    }


    private enum HttpMethod {

        POST {
            @Override
            public <T> T visit(final MethodTypeVisitor<T> visitor) throws Exception {
                return visitor.visitPost();
            }
        },

        GET {
            @Override
            public <T> T visit(final MethodTypeVisitor<T> visitor) throws Exception {
                return visitor.visitGet();
            }
        };

        public abstract <T> T visit(final MethodTypeVisitor<T> visitor) throws Exception;

        /**
         * Visitor
         *
         * @param <T>
         */
        public interface MethodTypeVisitor<T> {
            T visitPost() throws Exception;

            T visitGet() throws Exception;
        }
    }


}
