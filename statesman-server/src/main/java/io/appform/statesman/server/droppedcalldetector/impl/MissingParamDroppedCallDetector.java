package io.appform.statesman.server.droppedcalldetector.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import io.appform.statesman.server.droppedcalldetector.DroppedCallDetector;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

/**
 *
 */
@Slf4j
@Value
public class MissingParamDroppedCallDetector implements DroppedCallDetector {
    String provider;
    List<String> patterns;

    @Override
    public boolean detectDroppedCall(JsonNode paramMap) {
        if (null == patterns || patterns.isEmpty()) {
            log.debug("No call drop detection patterns found for provider: {}", provider);
            return false;
        }
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(paramMap.fields(), Spliterator.ORDERED), false)
                .filter(field -> patterns.stream()
                        .anyMatch(pattern -> field.getKey().matches(pattern)))
                .anyMatch(field -> field.getValue().isArray()
                        && (field.getValue().size() == 0
                        || (field.getValue().size() == 1
                        && Strings.isNullOrEmpty(field.getValue().get(0).asText()))));
    }
}
