package io.appform.statesman.server.exception;

import lombok.Getter;

/**
 * @author shashank.g
 */
public enum ResponseCode {

    PROVIDER_CREATE_ERROR(500, "ERROR CREATING PROVIDER"),
    VALIDATION_ERROR(400, "VALIDATION FAILED"),
    INTERNAL_SERVER_ERROR(500, "INTERNAL SERVER ERROR");

    @Getter
    private String message;

    @Getter
    private int code;

    ResponseCode(final int code, final String message) {
        this.code = code;
        this.message = message;
    }
}
