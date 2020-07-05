package io.appform.statesman.server.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.JsonNodeValueResolver;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import io.appform.hope.core.exceptions.errorstrategy.InjectValueErrorHandlingStrategy;
import io.appform.hope.lang.HopeLangEngine;
import io.appform.statesman.engine.StateTransitionEngine;
import io.appform.statesman.engine.WorkflowProvider;
import io.appform.statesman.engine.action.ActionExecutor;
import io.appform.statesman.engine.handlebars.HandleBarsService;
import io.appform.statesman.model.*;
import io.appform.statesman.model.dataaction.impl.MergeDataAction;
import io.appform.statesman.model.testing.HandlebarsTemplateTestingRequest;
import io.appform.statesman.model.testing.HopeRuleEvaluationTestingRequest;
import io.appform.statesman.model.testing.IngressTemplateTestingRequest;
import io.appform.statesman.server.ingress.IngressHandler;
import io.appform.statesman.server.requests.IngressCallback;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.glassfish.jersey.uri.UriComponent;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

@Path("/v1/housekeeping")
@Slf4j
@Api("Housekeeping APIs")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class HousekeepingResource {


    private final Provider<WorkflowProvider> workflowProvider;
    private final Provider<StateTransitionEngine> engine;
    private final Provider<ActionExecutor> actionExecutor;
    private final Provider<IngressHandler> ingressHandler;
    private final ObjectMapper mapper;
    private final HandleBarsService handleBarsService;
    private final HopeLangEngine hopeLangEngine;

    @Inject
    public HousekeepingResource(
            Provider<WorkflowProvider> workflowProvider,
            Provider<StateTransitionEngine> engine,
            Provider<ActionExecutor> actionExecutor,
            Provider<IngressHandler> ingressHandler,
            ObjectMapper mapper,
            HandleBarsService handleBarsService) {
        this.workflowProvider = workflowProvider;
        this.engine = engine;
        this.actionExecutor = actionExecutor;
        this.ingressHandler = ingressHandler;
        this.mapper = mapper;
        this.handleBarsService = handleBarsService;
        this.hopeLangEngine = HopeLangEngine.builder()
                .errorHandlingStrategy(new InjectValueErrorHandlingStrategy())
                .build();
    }

    @GET
    @Timed
    @Path("/debug/workflow/{workflowId}")
    @ApiOperation("Workflow debug")
    public Response getWorkflow(@PathParam("workflowId") String workflowId) {

        return Response.ok()
                .entity(workflowProvider.get().getWorkflow(workflowId))
                .build();
    }

    @POST
    @Timed
    @Path("/trigger/action/{actionId}")
    @ApiOperation("Trigger Action")
    public Response triggerAction(@PathParam("actionId") String actionId,
                                  @Valid Workflow workflow) {
        return Response.ok()
                .entity(actionExecutor.get().execute(actionId, workflow))
                .build();
    }

    @POST
    @Timed
    @Path("/ingress/template/test")
    @SneakyThrows
    @ApiOperation("Test ingestion template")
    public Response testIngressTemplate(@Valid IngressTemplateTestingRequest templateTestingRequest) {
        final JsonNode data;
        if(!Strings.isNullOrEmpty(templateTestingRequest.getQueryParams())) {
            data = mapper.valueToTree(UriComponent.decodeQuery(templateTestingRequest.getQueryParams(), true));
        }
        else {
            data = templateTestingRequest.getBody();
        }
        if(null == data) {
            return Response.ok().build();
        }
        return Response.ok()
                .entity(handleBarsService.transform(JsonNodeValueResolver.INSTANCE, templateTestingRequest.getTemplate(), data))
                .build();
    }

    @POST
    @Timed
    @Path("/ingress/rule/test")
    @SneakyThrows
    @ApiOperation("Test hope rule")
    public Response testRule(@Valid HopeRuleEvaluationTestingRequest hopeRuleEvaluationTest) {
        return Response.ok()
                .entity(Collections.singletonMap("result",
                                                 hopeLangEngine.evaluate(hopeRuleEvaluationTest.getRule(), hopeRuleEvaluationTest.getPayload())))
                .build();
    }

    @POST
    @Timed
    @Path("/handlebars/template/test")
    @SneakyThrows
    @ApiOperation("Test raw handlebars template")
    public Response testHandlebarsTemplate(@Valid HandlebarsTemplateTestingRequest request) {
        return Response.ok()
                .entity(handleBarsService.transform(request.getTemplate(), request.getBody()))
                .build();
    }

    @POST
    @Timed
    @Path("/ingress/translate/{translatorId}")
    @ApiOperation("Test ingress translator")
    public Response translateIngressPayload(@PathParam("translatorId") String translatorId,
                                     @Valid IngressCallback ingressCallback) throws Exception {
        return Response.ok()
                .entity(ingressHandler.get().translateIngressIvrPayload(translatorId, ingressCallback))
                .build();
    }

    @POST
    @Timed
    @Path("/trigger/workflow/template/{workflowTemplateId}")
    @ApiOperation("Trigger new workflow for given WorkflowTemplateId")
    public Response triggerWorkflowTemplate(@PathParam("workflowTemplateId") String workflowTemplateId,
                                            @Valid JsonNode translatedData) {
        WorkflowTemplate wfTemplate = workflowProvider.get().getTemplate(workflowTemplateId).orElse(null);
        if (null == wfTemplate) {
            log.warn("No such workflow template:{}", workflowTemplateId);
            return Response.noContent()
                    .build();
        }
        val date = new Date();
        val dataObject = new DataObject(mapper.createObjectNode(), wfTemplate.getStartState(), date, date);
        val workflow = new Workflow(UUID.randomUUID().toString(), wfTemplate.getId(), dataObject, date, date);
        workflowProvider.get().saveWorkflow(workflow);
        final AppliedTransitions appliedTransitions
                = engine.get().handle(new DataUpdate(workflow.getId(), translatedData, new MergeDataAction()), new MergeDataAction());
        return Response.ok()
                .entity(appliedTransitions)
                .build();
    }

    @POST
    @Timed
    @Path("/trigger/workflow/{workflowId}")
    @ApiOperation("Trigger workflow")
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


    @PUT
    @Timed
    @Path("/update/workflow/")
    @ApiOperation("Update workflow")
    public Response updateWorkflow(@Valid Workflow workflow) {
        WorkflowProvider wp = workflowProvider.get();
        Workflow wf = wp.getWorkflow(workflow.getId()).orElse(null);
        if (wf == null) {
            return Response.noContent()
                    .build();
        }
        wp.updateWorkflow(workflow);
        return Response.ok()
                .entity(workflow)
                .build();
    }

    @PUT
    @Timed
    @Path("/update/workflow/{workflowId}/state/")
    @ApiOperation("Update workflow state")
    public Response updateState(@PathParam("workflowId") String workflowId, @Valid State state) {
        WorkflowProvider wp = workflowProvider.get();
        Workflow wf = wp.getWorkflow(workflowId).orElse(null);
        if (wf == null) {
            return Response.noContent()
                    .build();
        }
        State currentState = wf.getDataObject().getCurrentState();
        currentState.setName(state.getName());
        currentState.setTerminal(state.isTerminal());
        wp.updateWorkflow(wf);
        return Response.ok()
                .entity(wf)
                .build();
    }

}
