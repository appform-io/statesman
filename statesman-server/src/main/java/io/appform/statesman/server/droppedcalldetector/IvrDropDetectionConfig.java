package io.appform.statesman.server.droppedcalldetector;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 *
 */
@Data
public class IvrDropDetectionConfig {
    private boolean enabled;
    private Map<String, List<String>> detectionPatterns;
}
