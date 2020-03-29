package io.appform.statesman.server.exception;

import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * @author shashank.g
 */
@Slf4j
public class GenericExceptionMapper implements ExceptionMapper<RuntimeException> {

    @Override
    public Response toResponse(final RuntimeException ex) {

        log.error("exception_occurred:", ex);
        if (ex instanceof ServerException) {
            final ServerException e = (ServerException) ex;
            return Response
                    .status(e.getResponseCode().getCode())
                    .build();
        }

        if (ex instanceof IllegalArgumentException) {
            return Response
                    .status(400)
                    .build();
        }

        return Response
                .status(500)
                .build();
    }
}
