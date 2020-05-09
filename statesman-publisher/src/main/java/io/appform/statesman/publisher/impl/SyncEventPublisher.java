package io.appform.statesman.publisher.impl;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.appform.functionmetrics.MonitoredFunction;
import io.appform.statesman.model.exception.ResponseCode;
import io.appform.statesman.model.exception.StatesmanError;
import io.appform.statesman.publisher.EventPublisher;
import io.appform.statesman.publisher.http.HttpClient;
import io.appform.statesman.publisher.http.HttpUtil;
import io.appform.statesman.publisher.model.Event;
import io.appform.statesman.publisher.model.KMessage;
import io.appform.statesman.publisher.model.Msg;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author shashank.g
 */
@Slf4j
public class SyncEventPublisher implements EventPublisher {

    private final String endpoint;
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final boolean disabled;

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
        this.mapper = mapper;
        this.disabled = config.disabled;
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
        validateTopic(event);
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
        if(disabled) {
            log.debug("Event ingestion disabled");
            return;
        }
        final KMessage message = convertToKMessage(events);
        final String url = String.format("%s/%s", this.endpoint, topic);
        log.trace("[KafkaPublisher] url: {}", url);

        try {
            log.trace("[KafkaPublisher] messageToKafka: {}", mapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            throw StatesmanError.propagate(e);
        }

        try (final Response response = client.post(url, message, null)) {
            if (!response.isSuccessful()) {
                log.error("unable to make ingest the data, responseCode: {}", response.code());
                throw new StatesmanError();
            }
        } catch (final Exception e) {
            log.error("exception_during_ingestion for topic: " + topic + ", eventSize: {}", events.size(), e);
            throw StatesmanError.propagate(e);
        }
    }

    private KMessage convertToKMessage(final List<Event> events) {
        return KMessage.builder()
                .messages(events
                        .stream()
                        .map(event -> Msg.builder()
                                .partitionKey(
                                        Strings.isNullOrEmpty(event.getPartitionKey())
                                                ? UUID.randomUUID().toString()
                                                : event.getPartitionKey()
                                )
                                .message(event)
                                .build()).collect(Collectors.toList()
                        ))
                .build();
    }

    private void validateTopic(final Event event) {
        if (Strings.isNullOrEmpty(event.getTopic())) {
            throw new StatesmanError("event.topic must be not null", ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }
}
