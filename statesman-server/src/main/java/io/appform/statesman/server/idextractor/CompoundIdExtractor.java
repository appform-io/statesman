package io.appform.statesman.server.idextractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import io.appform.statesman.server.callbacktransformation.TransformationTemplate;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.UUID;

/**
 * This is what gets bound and used and contains logic for id extraction
 */
@Singleton
@Slf4j
public class CompoundIdExtractor implements IdExtractor {

    private final JsonPathIdExtractor jsonPathPathIdExtractor;
    private final FqlIdExtractor fqlPathIdExtractor;

    @Inject
    public CompoundIdExtractor(
            JsonPathIdExtractor jsonPathPathIdExtractor,
            FqlIdExtractor fqlPathIdExtractor) {
        this.jsonPathPathIdExtractor = jsonPathPathIdExtractor;
        this.fqlPathIdExtractor = fqlPathIdExtractor;
    }

    @Override
    public Optional<String> extractId(TransformationTemplate template, JsonNode payload) {
        val fqlResponse = fqlPathIdExtractor.extractId(template, payload).orElse(null);
        if (!Strings.isNullOrEmpty(fqlResponse)) {
            return Optional.of(fqlResponse);
        }
        val jsonResponse = jsonPathPathIdExtractor.extractId(template, payload).orElse(null);
        return Optional.of(Strings.isNullOrEmpty(jsonResponse)
                           ? UUID.randomUUID().toString()
                           : jsonResponse);
    }
}
