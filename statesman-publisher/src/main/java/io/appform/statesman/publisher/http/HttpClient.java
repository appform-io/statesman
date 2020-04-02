package io.appform.statesman.publisher.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;

/**
 * @author shashank.g
 */
@Slf4j
@AllArgsConstructor
public class HttpClient {

    private static final MediaType APPLICATION_JSON = MediaType.parse("application/json");

    public final ObjectMapper mapper;
    public final OkHttpClient client;

    public Response post(String url,
                         final Object payload,
                         final Map<String, String> headers) throws IOException {
        final HttpUrl httpUrl = HttpUrl.get(url);
        Request.Builder postBuilder;
        if(payload instanceof String) {
             postBuilder =  new Request.Builder()
                     .url(httpUrl)
                     .post(RequestBody.create(APPLICATION_JSON, (String)payload));
        }
        else {
            postBuilder = new Request.Builder()
                    .url(httpUrl)
                    .post(RequestBody.create(APPLICATION_JSON, mapper.writeValueAsBytes(payload)));
        }
        if (headers != null) {
            headers.forEach(postBuilder::addHeader);
        }
        final Request request = postBuilder.build();
        return client.newCall(request).execute();
    }

    public Response get(final String url,
                        final Map<String, String> headers) throws IOException {
        final HttpUrl httpUrl = HttpUrl.get(url);
        final Request.Builder getBuilder = new Request.Builder()
                .url(httpUrl)
                .get();
        if (headers != null) {
            headers.forEach(getBuilder::addHeader);
        }
        final Request request = getBuilder.build();
        return client.newCall(request).execute();
    }
}
