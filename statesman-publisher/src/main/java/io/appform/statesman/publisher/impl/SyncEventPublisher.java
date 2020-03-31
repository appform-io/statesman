package io.appform.statesman.publisher.impl;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.core.hystrix.CommandFactory;
import io.appform.functionmetrics.MonitoredFunction;
import io.appform.statesman.model.exception.StatesmanError;
import io.appform.statesman.publisher.EventPublisher;
import io.appform.statesman.publisher.http.HttpClient;
import io.appform.statesman.publisher.http.HttpUtil;
import io.appform.statesman.publisher.model.Event;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * @author shashank.g
 */
@Slf4j
public class SyncEventPublisher implements EventPublisher {

    private final String endpoint;
    private final HttpClient client;

    /**
     * Constructor
     */
    public SyncEventPublisher(final ObjectMapper mapper,
                              final EventPublisherConfig config,
                              final MetricRegistry registry) {
        this.client = new HttpClient(mapper,
                HttpUtil.defaultClient(
                        SyncEventPublisher.class.getSimpleName(),
                        registry,
                        config.getHttpClientConfiguration()
                ));
        this.endpoint = config.getEndpoint();
    }

    @Override
    public void start() {
        //do nothing
    }

    @Override
    public void stop() {
        //do nothing
    }

    @Override
    @MonitoredFunction
    public void publish(final Event event) {
        publish(event.getTopic(), Collections.singletonList(event));
    }

    @Override
    public void publish(final String topic, final List<Event> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        ingest(topic, events);
    }

    //ingest via http
    private void ingest(final String topic, final List<Event> events) {
        try {
            final String url = String.format("%s/%s", this.endpoint, topic);
            CommandFactory.<Void>
                    create(SyncEventPublisher.class.getSimpleName(), "sync-publisher", UUID.randomUUID().toString())
                    .executor(() -> {
                        try (final Response response = client.post(url, events, null)) {
                            if (!response.isSuccessful()) {
                                log.error("unable to make ingest the data, responseCode: {}", response.code());
                                throw new StatesmanError();
                            }
                        } catch (final Exception e) {
                            throw StatesmanError.propagate(e);
                        }
                        return null;
                    }).execute();
        } catch (final Exception e) {
            log.error("exception_during_ingestion", e);
            throw StatesmanError.propagate(e);
        }
    }
}
