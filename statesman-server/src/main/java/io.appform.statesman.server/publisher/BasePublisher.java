package io.appform.statesman.server.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;

/**
 * @author shashank.g
 */
@Slf4j
@AllArgsConstructor
class BasePublisher {

    static final MediaType APPLICATION_JSON = MediaType.parse("application/json");

    final ObjectMapper mapper;
    final OkHttpClient client;

    static HttpUrl url(final String endpoint, final String relativePath) {
        final HttpUrl url = HttpUrl.get(String.format("%s:%s", endpoint, relativePath));
        log.info("url: {}", url);
        return url;
    }
}
