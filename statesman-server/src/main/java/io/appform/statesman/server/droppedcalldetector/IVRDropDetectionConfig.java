package io.appform.statesman.server.droppedcalldetector;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import io.dropwizard.util.Duration;
import io.dropwizard.validation.ValidationMethod;
import lombok.Data;
import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 *
 */
@Data
public class IVRDropDetectionConfig {

    @Value
    public static class DetectorConfig {
        @NotNull
        DroppedCallDetectorType type;

        List<String> patterns;

        String startTimeFieldPointer;

        Duration maxElapsedTime;

        @ValidationMethod
        @JsonIgnore
        boolean isValidForType() {
            switch (type) {
                case MISSING_PARAM: {
                    if(null != patterns && !patterns.isEmpty()) {
                        return true;
                    }
                }
                case ELAPSED_TIME: {
                    if(!Strings.isNullOrEmpty(startTimeFieldPointer) && null != maxElapsedTime) {
                        return true;
                    }
                }
                default: {
                    throw new IllegalArgumentException("Detector type " + type.name() + " is not supported");
                }
            }
        }
    }

    private boolean enabled;

    @Valid
    private Map<String, DetectorConfig> detectors;
}
