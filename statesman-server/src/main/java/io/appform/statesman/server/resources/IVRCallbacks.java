package io.appform.statesman.server.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.appform.statesman.engine.StateTransitionEngine;
import io.appform.statesman.engine.WorkflowProvider;
import io.appform.statesman.engine.handlebars.HandleBarsService;
import io.appform.statesman.server.callbacktransformation.CallbackTransformationTemplates;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;

/**
 *
 */
@Path("/callbacks/ivr")
public class IVRCallbacks {
    private final CallbackTransformationTemplates transformationTemplates;
    private final ObjectMapper mapper;
    private final HandleBarsService handleBarsService;
    private final Provider<StateTransitionEngine> engine;
    private final Provider<WorkflowProvider> workflowProvider;

    @Inject
    public IVRCallbacks(
            CallbackTransformationTemplates transformationTemplates,
            final ObjectMapper mapper,
            HandleBarsService handleBarsService,
            Provider<StateTransitionEngine> engine,
            Provider<WorkflowProvider> workflowProvider) {
        this.transformationTemplates = transformationTemplates;
        this.mapper = mapper;
        this.handleBarsService = handleBarsService;
        this.engine = engine;
        this.workflowProvider = workflowProvider;
    }

    @GET
    @Path("/final/${ivrProvider}")
    public Response ivrCallback(
            @PathParam ("ivrProvider") final String ivrProvider,
            @Context final UriInfo uriInfo) throws IOException {
        val queryParams = uriInfo.getQueryParameters();
        val node = mapper.valueToTree(queryParams);
        val template = transformationTemplates.getTemplates().get(ivrProvider);
        if(Strings.isNullOrEmpty(template)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
        }
        val stdPayload = handleBarsService.transform(template, node);
        val context = mapper.readTree(stdPayload);
        return Response.ok()
                .build();
    }

}
