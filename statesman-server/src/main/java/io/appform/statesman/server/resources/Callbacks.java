package io.appform.statesman.server.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import io.appform.statesman.server.ingress.IngressHandler;
import io.appform.statesman.server.ingress.ServiceProviderCallbackHandler;
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
@Path("/callbacks")
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
@Api("Callbacks")
public class Callbacks {

    private final Provider<IngressHandler> ingressHandler;
    private final Provider<ServiceProviderCallbackHandler> providerCallbackHandler;

    @Inject
    public Callbacks(
            Provider<IngressHandler> ingressHandler,
            Provider<ServiceProviderCallbackHandler> providerCallbackHandler) {
        this.ingressHandler = ingressHandler;
        this.providerCallbackHandler = providerCallbackHandler;
    }


    @POST
    @Path("/ingress/final/{ivrProvider}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response finalIngressCallback(
            @PathParam("ivrProvider") final String ivrProvider,
            final IngressCallback ingressCallback) throws IOException {
        return Response.ok()
                .entity(ImmutableMap.of("success",
                                        ingressHandler.get()
                                                .invokeEngineForOneShot(ivrProvider, ingressCallback)))
                .build();
    }

    @POST
    @Path("/ingress/step/{ivrProvider}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response stepIngressCallback(
            @PathParam("ivrProvider") final String ivrProvider,
            final IngressCallback ingressCallback) throws IOException {
        return Response.ok()
                .entity(ImmutableMap.of("success",
                                        ingressHandler.get()
                                                .invokeEngineForMultiStep(ivrProvider, ingressCallback)))
                .build();
    }

    @POST
    @Path("/provider/{serviceProvider}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response providerCallback(
            @PathParam("serviceProvider") final String providerId,
            JsonNode incomingData) {
        return Response.ok()
                .entity(ImmutableMap.of("success",
                                        providerCallbackHandler.get()
                                                .handleServiceProviderCallback(providerId, incomingData)))
                .build();
    }
}
