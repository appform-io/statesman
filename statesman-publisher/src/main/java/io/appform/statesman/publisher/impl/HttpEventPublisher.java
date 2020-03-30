package io.appform.statesman.publisher.impl;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.appform.core.hystrix.CommandFactory;
import io.appform.functionmetrics.MonitoredFunction;
import io.appform.statesman.model.exception.StatesmanError;
import io.appform.statesman.publisher.EventPublisher;
import io.appform.statesman.publisher.model.Event;
import io.appform.statesman.publisher.util.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * @author shashank.g
 */
@Slf4j
public class HttpEventPublisher extends BasePublisher implements EventPublisher {

    private final Map<String, String> eventTopics;

    /**
     * Constructor
     */
    public HttpEventPublisher(final EventPublisherConfig config,
                              final MetricRegistry registry,
                              final ObjectMapper mapper) {
        super(mapper, config.getEndpoint(), HttpUtil.defaultClient(HttpEventPublisher.class.getSimpleName(), registry, config));
        this.eventTopics = config.getEventTopics();
    }


    /**
     * Constructor
     */
    public HttpEventPublisher(final ObjectMapper mapper,
                              final OkHttpClient client,
                              final String endpoint,
                              final Map<String, String> eventTopics) {
        super(mapper, endpoint, client);
        this.eventTopics = eventTopics;
    }

    @Override
    @MonitoredFunction
    public void publish(final Event event) {
        if (event != null && !Strings.isNullOrEmpty(eventTopics.get(event.getType().name()))) {
            ingest(eventTopics.get(event.getType().name()),
                    event.getData(),
                    Strings.isNullOrEmpty(event.getId()) ? UUID.randomUUID().toString() : event.getId());
        }
    }

    @Override
    public void publish(final Event event, final String topic) {
        if (event != null && !Strings.isNullOrEmpty(eventTopics.get(event.getType().name()))) {
            ingest(topic,
                    event.getData(),
                    Strings.isNullOrEmpty(event.getId()) ? UUID.randomUUID().toString() : event.getId());
        }
    }

    private void ingest(final String topic, final JsonNode node, final String traceId) {
        try {
            String url = String.format("%s/%s", endpoint, topic);
            CommandFactory.<Void>
                    create(HttpEventPublisher.class.getSimpleName(), "data-ingestion", traceId)
                    .executor(() -> {
                        post(node, url);
                        return null;
                    }).execute();
        } catch (final Exception e) {
            log.error("exception_during_ingestion", e);
            throw StatesmanError.propagate(e);
        }
    }

    private void post(final JsonNode node, final String url) throws IOException {
        final RequestBody requestBody = RequestBody.create(APPLICATION_JSON, mapper.writeValueAsBytes(node));
        log.info("ingestion_call: {}", url);

        final Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        final Response response = client.newCall(request).execute();

        //TODO: remove
        log.info("response_code: {}, event: {}", response.code(), node.asText());

        if (!response.isSuccessful()) {
            log.error("response_code: {}, event: {}", response.code(), node.asText());
            throw new StatesmanError();
        }
    }
}
