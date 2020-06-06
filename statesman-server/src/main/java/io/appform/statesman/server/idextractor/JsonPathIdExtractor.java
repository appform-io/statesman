package io.appform.statesman.server.idextractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import io.appform.statesman.server.callbacktransformation.TransformationTemplate;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Singleton;
import java.util.Optional;

/**
 * Extracts a path based on provided json path
 */
@Singleton
@Slf4j
public class JsonPathIdExtractor implements IdExtractor {

    @Override
    public Optional<String> extractId(TransformationTemplate template, JsonNode payload) {
        if(null == template) {
            log.debug("No template specified");
            return Optional.empty();
        }
        val idPath = template.getIdPath();
        if(Strings.isNullOrEmpty(idPath)) {
            log.debug("No id path specified in template for provider: {}", template.getProvider());
            return Optional.empty();
        }
        val node = payload.at(idPath);
        if(node == null || node.isMissingNode() || node.isNull() || !node.isTextual()) {
            return Optional.empty();
        }
        return Optional.ofNullable(node.asText());
    }
}
