package io.appform.statesman.server.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.appform.statesman.publisher.EventPublisher;
import io.appform.statesman.publisher.model.Event;
import io.appform.statesman.publisher.model.EventType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author shashank.g
 */
@Produces(MediaType.APPLICATION_JSON)
@Path("/v1/events")
@Slf4j
@Api("Event APIs")
@Singleton
public class EventResource {

    private EventPublisher publisher;

    @Inject
    public EventResource(EventPublisher publisher) {
        this.publisher = publisher;
    }

    @POST
    @Timed
    @Path("/reporting/publish")
    @ApiOperation("publish event")
    public Response save(@Valid final Event event) throws Exception {
        publisher.publish(event);
        return Response.ok().build();
    }
}
