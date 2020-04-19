package io.appform.statesman.server.droppedcalldetector;

import io.appform.statesman.server.droppedcalldetector.impl.ElapsedTimeDroppedCallDetector;
import io.appform.statesman.server.droppedcalldetector.impl.MissingParamDroppedCallDetector;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 */
@Singleton
public class DroppedCallDetectors {
    private final Map<String, DroppedCallDetector> detectors;

    @Inject
    public DroppedCallDetectors(IVRDropDetectionConfig dropDetectionConfig) {
        if(!dropDetectionConfig.isEnabled()) {
            this.detectors = Collections.emptyMap();
            return;
        }
        this.detectors = dropDetectionConfig.getDetectors()
                .entrySet()
                .stream()
                .map(e -> build(e.getKey(), e.getValue()))
                .collect(Collectors.toMap(DroppedCallDetector::getProvider, Function.identity()));
    }

    public Optional<DroppedCallDetector> detectorFor(String provider) {
        return Optional.ofNullable(detectors.get(provider));
    }

    private static DroppedCallDetector build(String provider, IVRDropDetectionConfig.DetectorConfig detectorConfig) {
        switch (detectorConfig.getType()) {

            case MISSING_PARAM: {
                return new MissingParamDroppedCallDetector(provider, detectorConfig.getPatterns());
            }
            case ELAPSED_TIME: {
                return new ElapsedTimeDroppedCallDetector(provider, detectorConfig.getStartTimeFieldPointer(), detectorConfig.getMaxElapsedTime());
            }
            default: {
                throw new IllegalArgumentException("Unknown detector type: " + detectorConfig.getType());
            }
        }
    }
}
