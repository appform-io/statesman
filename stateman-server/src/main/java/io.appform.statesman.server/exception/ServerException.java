package io.appform.statesman.server.exception;

import com.google.common.base.Strings;
import lombok.Getter;

/**
 * @author shashank.g
 */
public class ServerException extends RuntimeException {

    @Getter
    private final ResponseCode responseCode;

    public ServerException() {
        super("something went wrong");
        this.responseCode = ResponseCode.INTERNAL_SERVER_ERROR;
    }

    public ServerException(final ResponseCode responseCode) {
        super(responseCode.getMessage());
        this.responseCode = responseCode;
    }

    public ServerException(final String message, final ResponseCode responseCode) {
        super(message);
        this.responseCode = responseCode;
    }

    public ServerException(final String message, final Throwable cause, final ResponseCode responseCode) {
        super(message, cause);
        this.responseCode = responseCode;
    }

    public static ServerException propagate(final Throwable e) {
        return propagate(null, e, ResponseCode.INTERNAL_SERVER_ERROR);
    }

    public static ServerException propagate(final Throwable e, final ResponseCode responseCode) {
        return propagate(null, e, responseCode);
    }

    public static ServerException propagate(final String message, final ResponseCode responseCode) {
        return propagate(message, new ServerException(message, responseCode), responseCode);
    }

    private static ServerException propagate(String message, final Throwable e, final ResponseCode responseCode) {
        if (Strings.isNullOrEmpty(message)) {
            message = e.getCause() == null ? null : e.getCause().toString();
        }
        if (e instanceof ServerException) {
            return (ServerException) e;
        }
        return new ServerException(message, e, responseCode);
    }
}
