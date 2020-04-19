package io.appform.statesman.server.droppedcalldetector;

import com.fasterxml.jackson.databind.JsonNode;

/**
 *
 */
public interface DroppedCallDetector {
    String getProvider();
    boolean detectDroppedCall(JsonNode paramMap);
}
