package io.appform.statesman.server.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.appform.statesman.engine.ActionTemplateStore;
import io.appform.statesman.engine.WorkflowProvider;
import io.appform.statesman.model.WorkflowTemplate;
import io.appform.statesman.model.action.template.ActionTemplate;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

@Produces(MediaType.APPLICATION_JSON)
@Path("/v1/template")
@Slf4j
@Api("Template related APIs")
@Singleton
public class TemplateResource {

    private final ActionTemplateStore actionTemplateStore;
    private final WorkflowProvider workflowProvider;

    @Inject
    public TemplateResource(final ActionTemplateStore actionTemplateStore, final WorkflowProvider workflowProvider) {
        this.actionTemplateStore = actionTemplateStore;
        this.workflowProvider = workflowProvider;
    }


    @POST
    @Timed
    @Path("/create/workflow")
    @ApiOperation("Create Workflow Template")
    public Response createWorkflow(@Valid WorkflowTemplate workflowTemplate) {
        workflowTemplate.setId(null);
        Optional<WorkflowTemplate> workflowTemplateOptional =
                workflowProvider.createTemplate(workflowTemplate);

        if (!workflowTemplateOptional.isPresent()) {
            return Response.serverError()
                    .build();
        }
        return Response.ok()
                .entity(workflowTemplateOptional.get())
                .build();
    }

    @GET
    @Timed
    @Path("/get/workflow/{templateId}")
    @ApiOperation("Get Workflow Template")
    public Response getWorkflow(@PathParam("templateId") String templateId) {
        Optional<WorkflowTemplate> workflowTemplateOptional =
                workflowProvider.getTemplate(templateId);

        if (!workflowTemplateOptional.isPresent()) {
            return Response.noContent()
                    .build();
        }
        return Response.ok()
                .entity(workflowTemplateOptional.get())
                .build();
    }

    @PUT
    @Timed
    @Path("/update/workflow")
    @ApiOperation("Update Workflow Template")
    public Response updateWorkflow(@Valid WorkflowTemplate workflowTemplate) {
        Optional<WorkflowTemplate> workflowTemplateOptional =
                workflowProvider.updateTemplate(workflowTemplate);

        if (!workflowTemplateOptional.isPresent()) {
            return Response.serverError()
                    .build();
        }
        return Response.ok()
                .entity(workflowTemplateOptional.get())
                .build();
    }


    @POST
    @Timed
    @Path("/create/action")
    @ApiOperation("Create Action Template")
    public Response createAction(@Valid ActionTemplate actionTemplate) {
        Optional<ActionTemplate> actionTemplateOptional = actionTemplateStore.create(actionTemplate);
        if (!actionTemplateOptional.isPresent()) {
            return Response.serverError()
                    .build();
        }
        return Response.ok()
                .entity(actionTemplateOptional.get())
                .build();
    }


    @GET
    @Timed
    @Path("/get/action/{templateId}")
    @ApiOperation("Get Action Template")
    public Response createAction(@PathParam("templateId") String templateId) {
        Optional<ActionTemplate> actionTemplateOptional = actionTemplateStore.get(templateId);
        if (!actionTemplateOptional.isPresent()) {
            return Response.serverError()
                    .build();
        }
        return Response.ok()
                .entity(actionTemplateOptional.get())
                .build();
    }

    @PUT
    @Timed
    @Path("/update/action")
    @ApiOperation("Update Action Template")
    public Response updateAction(@Valid ActionTemplate actionTemplate) {
        Optional<ActionTemplate> actionTemplateOptional = actionTemplateStore.update(actionTemplate);
        if (!actionTemplateOptional.isPresent()) {
            return Response.serverError()
                    .build();
        }
        return Response.ok()
                .entity(actionTemplateOptional.get())
                .build();
    }


}
