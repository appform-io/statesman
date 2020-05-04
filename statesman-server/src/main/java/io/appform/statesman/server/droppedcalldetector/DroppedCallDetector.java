package io.appform.statesman.server.droppedcalldetector;

import com.fasterxml.jackson.databind.JsonNode;
import io.appform.statesman.server.callbacktransformation.TransformationTemplate;

/**
 *
 */
public interface DroppedCallDetector {
    boolean detectDroppedCall(TransformationTemplate template, JsonNode node);
}
