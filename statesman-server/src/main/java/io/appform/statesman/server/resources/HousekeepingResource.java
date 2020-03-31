package io.appform.statesman.server.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.appform.statesman.engine.WorkflowProvider;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/v1/housekeeping")
@Slf4j
@Api("Housekeeping APIs")
@Singleton
public class HousekeepingResource {


    private final WorkflowProvider workflowProvider;

    @Inject
    public HousekeepingResource(WorkflowProvider workflowProvider) {
        this.workflowProvider = workflowProvider;
    }

    @GET
    @Timed
    @Path("/debug/workflow/{workflowId}")
    @ApiOperation("Workflow")
    public Response getWorkflow(@PathParam("workflowId") String workflowId) {

        return Response.ok()
                .entity(workflowProvider.getWorkflow(workflowId))
                .build();
    }

}
