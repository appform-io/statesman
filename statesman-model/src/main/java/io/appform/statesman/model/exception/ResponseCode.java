package io.appform.statesman.model.exception;

import lombok.Getter;

/**
 * @author shashank.g
 */
public enum ResponseCode {

    VALIDATION_ERROR(400, "VALIDATION FAILED"),
    OPERATION_NOT_SUPPORTED(400, "OPERATION NOT SUPPORTED"),
    TRANSFORMATION_ERROR(500, "TRANSFORMATION ERROR"),
    INTERNAL_SERVER_ERROR(500, "INTERNAL SERVER ERROR"),
    ;

    @Getter
    private String message;

    @Getter
    private int code;

    ResponseCode(final int code, final String message) {
        this.code = code;
        this.message = message;
    }
}
