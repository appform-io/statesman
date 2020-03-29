package io.appform.statesman.publisher.datapublisher;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.core.hystrix.CommandFactory;
import io.appform.statesman.model.exception.StatesmanError;
import io.appform.statesman.publisher.Publisher;
import io.appform.statesman.publisher.util.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.util.UUID;

/**
 * @author shashank.g
 */
@Slf4j
public class DataPublisher extends BasePublisher implements Publisher {

    /**
     * Constructor
     */
    public DataPublisher(final DataPublisherConfig config,
                         final MetricRegistry registry,
                         final ObjectMapper mapper) {
        super(mapper, config.getEndpoint(), HttpUtil.defaultClient(DataPublisher.class.getSimpleName(), registry, config));
    }


    /**
     * Constructor
     */
    public DataPublisher(final ObjectMapper mapper,
                         final OkHttpClient client,
                         final String endpoint) {
        super(mapper, endpoint, client);
    }

    @Override
    public void publish(final JsonNode data) {
        if (data != null) {
            ingest(data);
        }
    }

    private void ingest(final JsonNode node) {
        try {
            CommandFactory.<Void>
                    create(DataPublisher.class.getSimpleName(), "data-ingestion", UUID.randomUUID().toString())
                    .executor(() -> {
                        final RequestBody requestBody = RequestBody.create(APPLICATION_JSON, mapper.writeValueAsBytes(node));
                        final Request request = new Request.Builder()
                                .url(url)
                                .post(requestBody)
                                .build();
                        final Response response = client.newCall(request).execute();
                        if (!response.isSuccessful()) {
                            throw new StatesmanError();
                        }
                        return null;
                    }).execute();
        } catch (final Exception e) {
            log.error("exception_during_ingestion", e);
            throw new StatesmanError();
        }
    }
}
