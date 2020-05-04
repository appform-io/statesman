package io.appform.statesman.engine.action.impl;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
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
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Data
@Singleton
@ActionImplementation(name = "HTTP")
public class HttpAction extends BaseAction<HttpActionTemplate> {

    private static final String APPLICATION_JSON = "application/json";
    private HandleBarsService handleBarsService;
    private HttpClient client;

    @Inject
    public HttpAction(
            HandleBarsService handleBarsService,
            MetricRegistry registry,
            @Named("httpActionDefaultConfig") HttpClientConfiguration config,
            @Named("eventPublisher") final EventPublisher publisher,
            ObjectMapper mapper) {
        super(publisher, mapper);
        this.client = new HttpClient(mapper,
                                     HttpUtil.defaultClient(SyncEventPublisher.class.getSimpleName(),
                                                            registry,
                                                            config));
        this.handleBarsService = handleBarsService;
    }

    @Override
    public ActionType getType() {
        return ActionType.HTTP;
    }

    @Override
    public JsonNode execute(HttpActionTemplate actionTemplate, Workflow workflow) {
        String responseTranslator = actionTemplate.getResponseTranslator();
        HttpActionData httpActionData = transformPayload(workflow, actionTemplate);
        log.debug("Action call data: {}", httpActionData);
        JsonNode httpResponse = handle(httpActionData);
        if (httpResponse != null && !Strings.isNullOrEmpty(responseTranslator)) {
            return toJsonNode(handleBarsService.transform(responseTranslator, httpResponse));
        }
        return null;
    }

    private JsonNode handle(HttpActionData actionData) {
        try(Response httpResponse = executeRequest(actionData) ) {
            val responseBodyStr = HttpUtil.body(httpResponse);
            if (Strings.isNullOrEmpty(responseBodyStr)) {
                return NullNode.getInstance();
            }
            log.debug("HTTP Response: {}", responseBodyStr);
            List<String> contentType = Arrays.stream(
                                                httpResponse.header("Content-Type",APPLICATION_JSON)
                                                .split(";"))
                                                .collect(Collectors.toList());
            if (contentType.stream()
                    .anyMatch(value -> value.equalsIgnoreCase(APPLICATION_JSON))) {
                return toJsonNode(responseBodyStr);
            }
            return mapper.createObjectNode()
                    .put("payload", responseBodyStr);
        } catch (final Exception e) {
            throw StatesmanError.propagate(e);
        }
    }

    @SneakyThrows
    private Response executeRequest(HttpActionData actionData) {
        return actionData.getMethod().visit(new HttpMethod.MethodTypeVisitor<Response>() {
            private final Map<String, String> headers = actionData.getHeaders();
            private final String url = actionData.getUrl();

            @Override
            public Response visitPost() throws Exception {
                log.info("HTTP_ACTION POST Call url:{}", url);
                val payload = actionData.getPayload();
                Response response = client.post(url, payload, headers);
                if (!response.isSuccessful()) {
                    log.error("unable to do post action, actionData: {} Response: {}",
                              actionData, HttpUtil.body(response));
                    throw new StatesmanError();
                }
                return response;
            }

            @Override
            public Response visitGet() throws Exception {
                log.info("HTTP_ACTION GET Call url:{}", url);
                Response response = null;
                response = client.get(url, headers);
                if (!response.isSuccessful()) {
                    log.error("unable to do get action, actionData: {} Response: {}",
                            actionData, HttpUtil.body(response));
                    throw new StatesmanError();
                }
                return response;
            }
        });
    }

    private JsonNode toJsonNode(String responseBodyStr) {
        try {
            return mapper.readTree(responseBodyStr);
        } catch (Exception e) {
            log.error("Error while converting to json:" + responseBodyStr, e);
            return null;
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
