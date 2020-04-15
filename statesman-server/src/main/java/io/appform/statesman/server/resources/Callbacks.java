package io.appform.statesman.server.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import io.appform.statesman.server.ingress.IngressHandler;
import io.appform.statesman.server.ingress.ServiceProviderCallbackHandler;
import io.appform.statesman.server.requests.IngressCallback;
import io.swagger.annotations.Api;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
    @Path("/ingress/final/{ingressProvider}")
    @Consumes(MediaType.APPLICATION_JSON)
    @SneakyThrows
    public Response finalIngressCallback(
            @PathParam("ingressProvider") final String ingressProvider, final IngressCallback ingressCallback) {
        final boolean status = ingressHandler.get()
                .invokeEngineForOneShot(ingressProvider, ingressCallback);
        if(!status) {
            log.warn("Ignored ingress provider {} callback: {}", ingressProvider, ingressCallback);
        }
        return Response.ok()
                .entity(ImmutableMap.of("success", status))
                .build();
    }

    @POST
    @Path("/ingress/step/{ingressProvider}")
    @Consumes(MediaType.APPLICATION_JSON)
    @SneakyThrows
    public Response stepIngressCallback(
            @PathParam("ingressProvider") final String ingressProvider, final IngressCallback ingressCallback) {
        final boolean status = ingressHandler.get()
                .invokeEngineForMultiStep(ingressProvider, ingressCallback);
        if(!status) {
            log.warn("Ignored ingress provider {} callback: {}", ingressProvider, ingressCallback);
        }
        return Response.ok()
                .entity(ImmutableMap.of("success", status))
                .build();
    }

    @POST
    @Path("/ingress/obd/{ingressProvider}")
    @Consumes(MediaType.APPLICATION_JSON)
    @SneakyThrows
    public Response stepIngressObdCallback(
            @PathParam("ingressProvider") final String ingressProvider,
            @QueryParam("state") final String state,
            final IngressCallback ingressCallback) {
        final boolean status = ingressHandler.get()
                .invokeEngineForOBDCalls(ingressProvider, state, ingressCallback);
        if(!status) {
            log.warn("Ignored ingress OBD callback from provider {} callback: {}", ingressProvider, ingressCallback);
        }
        return Response.ok()
                .entity(ImmutableMap.of("success", status))
                .build();
    }

    @POST
    @Path("/ingress/form/{callcenter}")
    @Consumes(MediaType.APPLICATION_JSON)
    @SneakyThrows
    public Response stepIngressForm(
            @PathParam("callcenter") final String callcenter, final JsonNode data) {
        final boolean status = ingressHandler.get()
                .invokeEngineForFormPost(callcenter, data);
        if(!status) {
            log.warn("Ignored ingress form post callback from callcenter {} callback: {}", callcenter, data);
        }
        return Response.ok()
                .entity(ImmutableMap.of("success", status))
                .build();
    }

    @POST
    @Path("/provider/{serviceProvider}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response providerCallback(
            @PathParam("serviceProvider") final String serviceProvider, JsonNode incomingData) throws Exception {
        final boolean status = providerCallbackHandler.get()
                .handleServiceProviderCallback(serviceProvider, incomingData);
        if(!status) {
            log.warn("Ignored service provider {} callback: {}", serviceProvider, incomingData);
        }
        return Response.ok()
                .entity(ImmutableMap.of("success", status))
                .build();
    }
}
