package io.appform.statesman.server.publisher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.core.hystrix.CommandFactory;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.util.UUID;

/**
 * @author shashank.g
 */
@Slf4j
public class HttpDataPublisher extends BasePublisher implements DataPublisher {

    private static final String relativeUrl = "";
    private static final HttpUrl url = url("", relativeUrl);

    /**
     * Constructor
     */
    public HttpDataPublisher(final ObjectMapper mapper, final OkHttpClient client) {
        super(mapper, client);
    }

    @Override
    public void publish(final JsonNode data) {
        ingest(data);
    }

    private void ingest(final JsonNode node) {
        try {
            CommandFactory.<Void>
                    create(HttpDataPublisher.class.getSimpleName(), "ingestion", UUID.randomUUID().toString())
                    .executor(() -> {
                        final RequestBody requestBody = RequestBody.create(APPLICATION_JSON, mapper.writeValueAsBytes(node));
                        final Request request = new Request.Builder()
                                .url(url)
                                .post(requestBody)
                                .build();
                        final Response response = client.newCall(request).execute();
                        if (!response.isSuccessful()) {
                            //TODO: move to app custom exception
                            throw new RuntimeException();
                        }
                        return null;
                    }).execute();
        } catch (final Exception e) {
            log.error("exception_during_ingestion", e);
            throw new RuntimeException();
        }
    }
}
