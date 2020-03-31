package io.appform.statesman.publisher.impl;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.appform.core.hystrix.CommandFactory;
import io.appform.functionmetrics.MonitoredFunction;
import io.appform.statesman.model.exception.StatesmanError;
import io.appform.statesman.publisher.EventPublisher;
import io.appform.statesman.publisher.http.HttpClient;
import io.appform.statesman.publisher.http.HttpUtil;
import io.appform.statesman.publisher.model.Event;
import io.appform.statesman.publisher.model.EventType;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Response;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author shashank.g
 */
@Slf4j
public class SyncEventPublisher extends HttpClient implements EventPublisher {

    private final String endpoint;

    /**
     * Constructor
     */
    public SyncEventPublisher(final EventPublisherConfig config,
                              final MetricRegistry registry,
                              final ObjectMapper mapper) {
        super(mapper, HttpUtil.defaultClient(SyncEventPublisher.class.getSimpleName(), registry, config.getHttpClientConfiguration()));
        this.endpoint = config.getEndpoint();
    }

    /**
     * Constructor
     */
    public SyncEventPublisher(final ObjectMapper mapper,
                              final OkHttpClient client,
                              final String endpoint,
                              final Map<String, String> eventTopics) {
        super(mapper, client);
        this.endpoint = endpoint;
    }

    @Override
    @MonitoredFunction
    public void publish(final Event event, final EventType type) {
        if (event != null && !Strings.isNullOrEmpty(type.name())) {
            publish(Collections.singletonList(event), type);
        }
    }

    @Override
    public void publish(final List<Event> events, final EventType type) {
        if (events == null || events.isEmpty()) {
            return;
        }

        ingest(type.name(), events);
    }

    @Override
    public void publish(final Event event, final String topic) {
        publish(Collections.singletonList(event), topic);
    }

    @Override
    public void publish(final List<Event> events, final String topic) {
        if (events == null || events.isEmpty()) {
            return;
        }
        ingest(topic, events);
    }

    //ingest via http
    private void ingest(final String topic, final List<Event> messages) {
        try {
            final String url = String.format("%s/%s", this.endpoint, topic);
            CommandFactory.<Void>
                    create(SyncEventPublisher.class.getSimpleName(), "kafka-publisher", UUID.randomUUID().toString())
                    .executor(() -> {
                        final Response response = post(url, mapper.writeValueAsBytes(messages), null);
                        if (!response.isSuccessful()) {
                            log.error("unable to make ingest the data");
                            throw new StatesmanError();
                        }
                        return null;
                    }).execute();
        } catch (final Exception e) {
            log.error("exception_during_ingestion", e);
            throw StatesmanError.propagate(e);
        }
    }
}
