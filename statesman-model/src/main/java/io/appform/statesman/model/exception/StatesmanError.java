package io.appform.statesman.model.exception;

import com.google.common.base.Strings;
import lombok.Getter;

/**
 * @author shashank.g
 */
public class StatesmanError extends RuntimeException {

    @Getter
    private final ResponseCode responseCode;

    public StatesmanError() {
        super("something went wrong");
        this.responseCode = ResponseCode.INTERNAL_SERVER_ERROR;
    }

    public StatesmanError(final ResponseCode responseCode) {
        super(responseCode.getMessage());
        this.responseCode = responseCode;
    }

    public StatesmanError(final String message, final ResponseCode responseCode) {
        super(message);
        this.responseCode = responseCode;
    }

    public StatesmanError(final String message, final Throwable cause, final ResponseCode responseCode) {
        super(message, cause);
        this.responseCode = responseCode;
    }

    public static StatesmanError propagate(final Throwable e) {
        return propagate(null, e, ResponseCode.INTERNAL_SERVER_ERROR);
    }

    public static StatesmanError propagate(final Throwable e, final ResponseCode responseCode) {
        return propagate(null, e, responseCode);
    }

    public static StatesmanError propagate(final String message, final ResponseCode responseCode) {
        return propagate(message, new StatesmanError(message, responseCode), responseCode);
    }

    private static StatesmanError propagate(String message, final Throwable e, final ResponseCode responseCode) {
        if (Strings.isNullOrEmpty(message)) {
            message = e.getCause() == null ? null : e.getCause().toString();
        }
        if (e instanceof StatesmanError) {
            return (StatesmanError) e;
        }
        return new StatesmanError(message, e, responseCode);
    }
}
