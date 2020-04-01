package io.appform.statesman.server.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import io.appform.statesman.engine.WorkflowProvider;
import io.appform.statesman.engine.action.ActionExecutor;
import io.appform.statesman.model.Workflow;
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
    private final Provider<ActionExecutor> actionExecutor;

    @Inject
    public HousekeepingResource(Provider<WorkflowProvider> workflowProvider,
                                Provider<ActionExecutor> actionExecutor) {
        this.workflowProvider = workflowProvider;
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
    @ApiOperation("trigger ction")
    public Response triggerAction(@PathParam("action") String actionId,
                                  @Valid Workflow workflowId) {
        actionExecutor.get().execute(actionId, workflowId);
        return Response.ok()
                .build();
    }

}
