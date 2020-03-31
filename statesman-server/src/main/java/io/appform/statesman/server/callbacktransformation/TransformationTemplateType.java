package io.appform.statesman.server.callbacktransformation;

import lombok.Getter;

/**
 *
 */
public enum TransformationTemplateType {
    ONE_SHOT(Values.ONE_SHOT),
    STEP_BY_STEP(Values.STEP_BY_STEP);

    public static final class Values {
        public static final String ONE_SHOT = "ONE_SHOT";
        public static final String STEP_BY_STEP = "STEP_BY_STEP";
    }

    @Getter
    private final String value;

    TransformationTemplateType(String value) {
        this.value = value;
    }

}
