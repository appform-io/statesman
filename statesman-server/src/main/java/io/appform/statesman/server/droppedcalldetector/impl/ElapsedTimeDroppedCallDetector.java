package io.appform.statesman.server.droppedcalldetector.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import io.appform.statesman.server.droppedcalldetector.DroppedCallDetector;
import io.dropwizard.util.Duration;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Date;

/**
 *
 */
@Value
@Slf4j
public class ElapsedTimeDroppedCallDetector implements DroppedCallDetector {
    String provider;

    String startTimeFieldPointer;

    Duration maxElapsedTime;

    @Override
    public boolean detectDroppedCall(JsonNode paramMap) {
        if(Strings.isNullOrEmpty(startTimeFieldPointer)) {
            return false;
        }
        final JsonNode param = paramMap.at(startTimeFieldPointer);
        if(param.isMissingNode() || !param.isLong()) {
            log.debug("No field found at {} for {}", startTimeFieldPointer, paramMap);
            return false;
        }
        val elapsedTime = param.asLong() - new Date().getTime();
        return elapsedTime > maxElapsedTime.toMilliseconds();
    }
}
