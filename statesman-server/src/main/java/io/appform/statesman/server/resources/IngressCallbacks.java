package io.appform.statesman.server.resources;

import com.google.common.collect.ImmutableMap;
import io.appform.statesman.server.ingress.IngressHandler;
import io.appform.statesman.server.requests.IngressCallback;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 *
 */
@Path("/callbacks/ingress")
@Api("Ingress callbacks")
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class IngressCallbacks {

    private final Provider<IngressHandler> ingressHandler;

    @Inject
    public IngressCallbacks(Provider<IngressHandler> ingressHandler) {
        this.ingressHandler = ingressHandler;
    }


    @POST
    @Path("/final/{ivrProvider}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response finalIVRCallback(
            @PathParam("ivrProvider") final String ivrProvider,
            final IngressCallback ingressCallback) throws IOException {
        return Response.ok()
                .entity(ImmutableMap.of("success",
                                        ingressHandler.get()
                                                .invokeEngineForOneShot(ivrProvider, ingressCallback)))
                .build();
    }

    @GET
    @Path("/step/{ivrProvider}")
    public Response stepIVRCallback(
            @PathParam("ivrProvider") final String ivrProvider,
            final IngressCallback ingressCallback) throws IOException {
        return Response.ok()
                .entity(ImmutableMap.of("success",
                                        ingressHandler.get()
                                                .invokeEngineForMultiStep(ivrProvider, ingressCallback)))
                .build();
    }

}
