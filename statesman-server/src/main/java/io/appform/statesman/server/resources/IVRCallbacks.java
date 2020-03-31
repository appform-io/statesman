package io.appform.statesman.server.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import io.appform.statesman.engine.StateTransitionEngine;
import io.appform.statesman.engine.WorkflowProvider;
import io.appform.statesman.engine.handlebars.HandleBarsService;
import io.appform.statesman.model.AppliedTransitions;
import io.appform.statesman.model.DataObject;
import io.appform.statesman.model.DataUpdate;
import io.appform.statesman.model.Workflow;
import io.appform.statesman.model.dataaction.impl.MergeDataAction;
import io.appform.statesman.model.exception.ResponseCode;
import io.appform.statesman.model.exception.StatesmanError;
import io.appform.statesman.server.callbacktransformation.CallbackTransformationTemplates;
import io.appform.statesman.server.evaluator.WorkflowTemplateSelector;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

/**
 *
 */
@Path("/callbacks/ivr")
@Api("IVR callbacks")
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class IVRCallbacks {
    private final CallbackTransformationTemplates transformationTemplates;
    private final ObjectMapper mapper;
    private final HandleBarsService handleBarsService;
    private final Provider<StateTransitionEngine> engine;
    private final Provider<WorkflowProvider> workflowProvider;
    private final Provider<WorkflowTemplateSelector> templateSelector;

    @Inject
    public IVRCallbacks(
            CallbackTransformationTemplates transformationTemplates,
            final ObjectMapper mapper,
            HandleBarsService handleBarsService,
            Provider<StateTransitionEngine> engine,
            Provider<WorkflowProvider> workflowProvider,
            Provider<WorkflowTemplateSelector> templateSelector) {
        this.transformationTemplates = transformationTemplates;
        this.mapper = mapper;
        this.handleBarsService = handleBarsService;
        this.engine = engine;
        this.workflowProvider = workflowProvider;
        this.templateSelector = templateSelector;
    }

    @GET
    @Path("/final/{ivrProvider}")
    public Response finalIVRCallback(
            @PathParam("ivrProvider") final String ivrProvider,
            @Context final UriInfo uriInfo) throws IOException {

        val queryParams = uriInfo.getQueryParameters();
        val node = mapper.valueToTree(queryParams);
        val transformationTemplate = transformationTemplates.getTemplates().get(ivrProvider);
        if (null == transformationTemplate) {
            throw new StatesmanError("No matching translation template found for context: " + node,
                    ResponseCode.INVALID_OPERATION);
        }
        val stdPayload = handleBarsService.transform(transformationTemplate.getTemplate(), node);
        val context = mapper.readTree(stdPayload);
        val wfTemplate = templateSelector.get()
                .determineTemplate(context)
                .orElse(null);
        if (null == wfTemplate) {
            throw new StatesmanError("No matching workflow template found for context: " + stdPayload,
                    ResponseCode.INVALID_OPERATION);
        }
        val wfIdNode = node.at(transformationTemplate.getIdPath());
        boolean workflowExists = !Strings.isNullOrEmpty(transformationTemplate.getIdPath())
                && wfIdNode.isMissingNode();
        val wfId = workflowExists
                ? wfIdNode.asText()
                : UUID.randomUUID().toString();
        val date = new Date();
        Workflow workflow = new Workflow(wfId,
                wfTemplate.getId(),
                new DataObject(mapper.createObjectNode(),
                        wfTemplate.getStartState(),
                        date,
                        date));
        if (workflowExists) {
            workflowProvider.get().updateWorkflow(workflow);
        } else {
            workflowProvider.get()
                    .saveWorkflow(workflow);
        }
        final AppliedTransitions appliedTransitions
                = engine.get()
                .handle(new DataUpdate(wfId, node, new MergeDataAction()));
        log.debug("Workflow: {} with template: {} went through transitions: {}",
                wfId, wfTemplate.getId(), appliedTransitions.getTransitions());
        return Response.ok()
                .entity(ImmutableMap.of("success", true))
                .build();
    }

}
