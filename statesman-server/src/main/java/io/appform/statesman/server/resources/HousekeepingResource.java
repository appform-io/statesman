package io.appform.statesman.server.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import io.appform.statesman.engine.StateTransitionEngine;
import io.appform.statesman.engine.WorkflowProvider;
import io.appform.statesman.engine.action.ActionExecutor;
import io.appform.statesman.model.AppliedTransitions;
import io.appform.statesman.model.DataUpdate;
import io.appform.statesman.model.Workflow;
import io.appform.statesman.model.dataaction.impl.MergeDataAction;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v1/housekeeping")
@Slf4j
@Api("Housekeeping APIs")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class HousekeepingResource {


    private final Provider<WorkflowProvider> workflowProvider;
    private final Provider<StateTransitionEngine> engine;
    private final Provider<ActionExecutor> actionExecutor;

    @Inject
    public HousekeepingResource(Provider<WorkflowProvider> workflowProvider,
                                Provider<StateTransitionEngine> engine,
                                Provider<ActionExecutor> actionExecutor) {
        this.workflowProvider = workflowProvider;
        this.engine = engine;
        this.actionExecutor = actionExecutor;
    }

    @GET
    @Timed
    @Path("/debug/workflow/{workflowId}")
    @ApiOperation("Workflow")
    public Response getWorkflow(@PathParam("workflowId") String workflowId) {

        return Response.ok()
                .entity(workflowProvider.get().getWorkflow(workflowId))
                .build();
    }

    @POST
    @Timed
    @Path("/trigger/action/{actionId}")
    @ApiOperation("trigger Action")
    public Response triggerAction(@PathParam("actionId") String actionId,
                                  @Valid Workflow workflow) {
        actionExecutor.get().execute(actionId, workflow);
        return Response.ok()
                .build();
    }

    @POST
    @Timed
    @Path("/trigger/workflow/{workflowId}")
    @ApiOperation("trigger workflow")
    public Response triggerWorkflow(@PathParam("workflowId") String workflowId,
                                    @Valid JsonNode update) {
        Workflow wf = workflowProvider.get().getWorkflow(workflowId).orElse(null);
        if (wf == null) {
            return Response.noContent()
                    .build();
        }
        final AppliedTransitions appliedTransitions
                = engine.get().handle(new DataUpdate(workflowId, update, new MergeDataAction()));
        return Response.ok()
                .entity(appliedTransitions)
                .build();
    }

}
