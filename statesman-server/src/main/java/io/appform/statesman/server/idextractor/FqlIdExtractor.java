package io.appform.statesman.server.idextractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.JsonNodeValueResolver;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import io.appform.statesman.engine.handlebars.HandleBarsService;
import io.appform.statesman.model.FoxtrotClientConfig;
import io.appform.statesman.publisher.http.HttpClient;
import io.appform.statesman.publisher.http.HttpUtil;
import io.appform.statesman.server.callbacktransformation.TransformationTemplate;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import okhttp3.Response;
import org.eclipse.jetty.http.HttpStatus;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Optional;

/**
 *
 */
@Singleton
@Slf4j
public class FqlIdExtractor implements IdExtractor {

    private final Provider<HandleBarsService> handleBarsProvider;
    private final FoxtrotClientConfig foxtrotClientConfig;
    private final Provider<HttpClient> httpClientProvider;
    private final ObjectMapper mapper;

    @Inject
    public FqlIdExtractor(
            Provider<HandleBarsService> handleBarsProvider,
            FoxtrotClientConfig foxtrotClientConfig,
            Provider<HttpClient> httpClientProvider,
            ObjectMapper mapper) {
        this.handleBarsProvider = handleBarsProvider;
        this.foxtrotClientConfig = foxtrotClientConfig;
        this.httpClientProvider = httpClientProvider;
        this.mapper = mapper;
    }

    @SneakyThrows
    @Override
    public Optional<String> extractId(TransformationTemplate template, JsonNode payload) {
        if(null == template) {
            log.debug("Empty template provided");
            return Optional.empty();
        }
        if(Strings.isNullOrEmpty(template.getFqlPath())) {
            log.debug("Empty fql path provided");
            return Optional.empty();
        }
        val fqlQuery = handleBarsProvider.get()
                .transform(JsonNodeValueResolver.INSTANCE, template.getFqlPath(), payload);
        log.debug("FQL Query: {}", fqlQuery);
        try (final Response response = httpClientProvider.get()
                .get(foxtrotClientConfig.getEndpoint() + "/foxtrot/v1/fql",
                     ImmutableMap.of("Authorization", "Bearer " + foxtrotClientConfig.getAccessToken()))) {
            if(response.code() == HttpStatus.NO_CONTENT_204) {
                log.debug("No results found for query: {}",  fqlQuery);
                return Optional.empty();
            }
            val responseBody = HttpUtil.body(response);
            log.debug("Response: {}", responseBody);
            val rows = mapper.readTree(responseBody)
                    .get("rows");
            if(null == rows || rows.isMissingNode() || !rows.isArray()) {
                log.debug("No valid response row found for: {}. Data: {}", fqlQuery, responseBody);
                return Optional.empty();
            }
            val dataNode = rows.get(0);
            if(null == dataNode || dataNode.isMissingNode() || !dataNode.get(0).isTextual()) {
                log.debug("Row 0 does not contain valid data for: {}", fqlQuery);
                return Optional.empty();
            }
            val field = dataNode.fieldNames()
                    .next();
            if(Strings.isNullOrEmpty(field)) {
                log.debug("No field found in row element for: {}", fqlQuery);
                return Optional.empty();
            }
            return Optional.ofNullable(dataNode.get(field).asText());
        }
    }
}
