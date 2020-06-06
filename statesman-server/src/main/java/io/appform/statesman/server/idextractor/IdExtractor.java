package io.appform.statesman.server.idextractor;

import com.fasterxml.jackson.databind.JsonNode;
import io.appform.statesman.server.callbacktransformation.TransformationTemplate;

import java.util.Optional;

/**
 *
 */
public interface IdExtractor {
    Optional<String> extractId(TransformationTemplate template, JsonNode payload);
}
