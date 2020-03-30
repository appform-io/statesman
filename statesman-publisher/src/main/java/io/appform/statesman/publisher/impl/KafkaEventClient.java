package io.appform.statesman.publisher.impl;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.appform.core.hystrix.CommandFactory;
import io.appform.functionmetrics.MonitoredFunction;
import io.appform.statesman.model.exception.StatesmanError;
import io.appform.statesman.publisher.EventPublisher;
import io.appform.statesman.publisher.http.HttpClient;
import io.appform.statesman.publisher.model.Event;
import io.appform.statesman.publisher.model.EventType;
import io.appform.statesman.publisher.model.KMessage;
import io.appform.statesman.publisher.http.HttpUtil;
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
public class KafkaEventClient extends HttpClient implements EventPublisher {

    private final Map<String, String> eventTopics;

    /**
     * Constructor
     */
    public KafkaEventClient(final EventPublisherConfig config,
                            final MetricRegistry registry,
                            final ObjectMapper mapper) {
        super(mapper, config.getEndpoint(), HttpUtil.defaultClient(KafkaEventClient.class.getSimpleName(), registry, config));
        this.eventTopics = config.getEventTopics();
    }


    /**
     * Constructor
     */
    public KafkaEventClient(final ObjectMapper mapper,
                            final OkHttpClient client,
                            final String endpoint,
                            final Map<String, String> eventTopics) {
        super(mapper, endpoint, client);
        this.eventTopics = eventTopics;
    }

    @Override
    @MonitoredFunction
    public void publish(final Event event, final EventType type) {
        if (event != null && !Strings.isNullOrEmpty(eventTopics.get(type.name()))) {
            publish(Collections.singletonList(event), type);
        }
    }

    @Override
    public void publish(final List<Event> events, final EventType type) {
        if (events == null || events.isEmpty()) {
            return;
        }

        ingest(eventTopics.get(type.name()), Converter.toMessages(events));
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
        ingest(eventTopics.get(topic), Converter.toMessages(events));
    }

    //ingest via http
    private void ingest(final String topic, final List<KMessage> messages) {
        try {
            final String url = String.format("%s/%s", endpoint, topic);
            CommandFactory.<Void>
                    create(KafkaEventClient.class.getSimpleName(), "kafka-publisher", UUID.randomUUID().toString())
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
