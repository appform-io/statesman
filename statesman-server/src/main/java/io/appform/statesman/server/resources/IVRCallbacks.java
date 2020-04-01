package io.appform.statesman.server.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.JsonNodeValueResolver;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import io.appform.hope.core.exceptions.errorstrategy.InjectValueErrorHandlingStrategy;
import io.appform.hope.lang.HopeLangEngine;
import io.appform.statesman.engine.StateTransitionEngine;
import io.appform.statesman.engine.WorkflowProvider;
import io.appform.statesman.engine.handlebars.HandleBarsService;
import io.appform.statesman.model.*;
import io.appform.statesman.model.dataaction.impl.MergeDataAction;
import io.appform.statesman.model.exception.ResponseCode;
import io.appform.statesman.model.exception.StatesmanError;
import io.appform.statesman.server.callbacktransformation.TransformationTemplate;
import io.appform.statesman.server.callbacktransformation.TransformationTemplateVisitor;
import io.appform.statesman.server.callbacktransformation.impl.OneShotTransformationTemplate;
import io.appform.statesman.server.callbacktransformation.impl.StepByStepTransformationTemplate;
import io.appform.statesman.server.dao.callback.CallbackTemplateProvider;
import io.appform.statesman.server.evaluator.WorkflowTemplateSelector;
import io.appform.statesman.server.requests.IVROneShot;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.glassfish.jersey.internal.util.collection.ImmutableMultivaluedMap;
import org.glassfish.jersey.uri.UriComponent;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 *
 */
@Path("/callbacks/ivr")
@Api("IVR callbacks")
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class IVRCallbacks {
    private final CallbackTemplateProvider callbackTemplateProvider;
    private final ObjectMapper mapper;
    private final HandleBarsService handleBarsService;
    private final Provider<StateTransitionEngine> engine;
    private final Provider<WorkflowProvider> workflowProvider;
    private final Provider<WorkflowTemplateSelector> templateSelector;
    private final HopeLangEngine hopeLangEngine;

    @Inject
    public IVRCallbacks(
            CallbackTemplateProvider callbackTemplateProvider,
            final ObjectMapper mapper,
            HandleBarsService handleBarsService,
            Provider<StateTransitionEngine> engine,
            Provider<WorkflowProvider> workflowProvider,
            Provider<WorkflowTemplateSelector> templateSelector) {
        this.callbackTemplateProvider = callbackTemplateProvider;
        this.mapper = mapper;
        this.handleBarsService = handleBarsService;
        this.engine = engine;
        this.workflowProvider = workflowProvider;
        this.templateSelector = templateSelector;
        hopeLangEngine = HopeLangEngine.builder()
                .errorHandlingStrategy(new InjectValueErrorHandlingStrategy())
                .build();
    }

/*    @GET
    @Path("/final/{ivrProvider}")
    public Response finalIVRCallback(
            @PathParam("ivrProvider") final String ivrProvider,
            @Context final UriInfo uriInfo) throws IOException {*/
    @POST
    @Path("/final/{ivrProvider}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response finalIVRCallback(
            @PathParam("ivrProvider") final String ivrProvider,
            final IVROneShot ivrOneShot) throws IOException {

        log.info("Request:{}", ivrOneShot);
        val queryParams = new ImmutableMultivaluedMap<>(UriComponent.decodeQuery(ivrOneShot.getQueryString(), true));
        val node = mapper.valueToTree(queryParams);
        Optional<TransformationTemplate> transformationTemplateOptional = callbackTemplateProvider.getAll()
                .stream()
                .filter(template -> template.getProvider().equals(ivrProvider))
                .findAny();
        if (!transformationTemplateOptional.isPresent()) {
            throw new StatesmanError("No matching translation template found for context: " + node,
                                     ResponseCode.INVALID_OPERATION);
        }
        val transformationTemplate = transformationTemplateOptional.get();
        val tmpl = transformationTemplate.accept(new TransformationTemplateVisitor<OneShotTransformationTemplate>() {
            @Override
            public OneShotTransformationTemplate visit(OneShotTransformationTemplate oneShotTransformationTemplate) {
                return oneShotTransformationTemplate;
            }

            @Override
            public OneShotTransformationTemplate visit(StepByStepTransformationTemplate stepByStepTransformationTemplate) {
                return null;
            }
        });
        Preconditions.checkNotNull(tmpl);
        val stdPayload = handleBarsService.transform(JsonNodeValueResolver.INSTANCE, tmpl.getTemplate(), node);
        log.info("stdPayload:{}", stdPayload);
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
                && isValid(wfIdNode);
        val wfId = extractWorkflowId(node, transformationTemplate);
        val date = new Date();
        Workflow workflow = new Workflow(wfId,
                                         wfTemplate.getId(),
                                         new DataObject(mapper.createObjectNode(),
                                                        wfTemplate.getStartState(),
                                                        date,
                                                        date));
        if (workflowExists) {
            workflowProvider.get().updateWorkflow(workflow);
        }
        else {
            workflowProvider.get()
                    .saveWorkflow(workflow);
        }
        final AppliedTransitions appliedTransitions
                = engine.get()
                .handle(new DataUpdate(wfId, context, new MergeDataAction()));
        log.info("Workflow: {} with template: {} went through transitions: {}",
                  wfId, wfTemplate.getId(), appliedTransitions.getTransitions());
        return Response.ok()
                .entity(ImmutableMap.of("success", true))
                .build();
    }

    @GET
    @Path("/step/{ivrProvider}")
    public Response stepIVRCallback(
            @PathParam("ivrProvider") final String ivrProvider,
            @Context final UriInfo uriInfo) throws IOException {
        val queryParams = uriInfo.getQueryParameters();
        val node = mapper.valueToTree(queryParams);
        Optional<TransformationTemplate> transformationTemplateOptional = callbackTemplateProvider.getAll()
                .stream()
                .filter(template -> template.getProvider().equals(ivrProvider))
                .findAny();
        if (!transformationTemplateOptional.isPresent()) {
            throw new StatesmanError("No matching translation template found for context: " + node,
                                     ResponseCode.INVALID_OPERATION);
        }
        val transformationTemplate = transformationTemplateOptional.get();
        val tmpl = transformationTemplate.accept(new TransformationTemplateVisitor<StepByStepTransformationTemplate>() {
            @Override
            public StepByStepTransformationTemplate visit(OneShotTransformationTemplate oneShotTransformationTemplate) {
                return null;
            }

            @Override
            public StepByStepTransformationTemplate visit(StepByStepTransformationTemplate stepByStepTransformationTemplate) {
                return stepByStepTransformationTemplate;
            }
        });
        Preconditions.checkNotNull(tmpl);
        val date = new Date();
        val selectedStep = selectStep(node, tmpl);
        Preconditions.checkNotNull(selectedStep);
        val stdPayload = handleBarsService.transform(JsonNodeValueResolver.INSTANCE, selectedStep.getTemplate(), node);
        val context = mapper.readTree(stdPayload);
        val wfIdNode = node.at(transformationTemplate.getIdPath());
        String wfId = UUID.randomUUID().toString();
        Workflow wf = null;
        WorkflowTemplate wfTemplate = null;
        if (isValid(wfIdNode)) {
            //We found ID node .. so we have to reuse
            wfId = extractWorkflowId(node, transformationTemplate);
            wf = workflowProvider.get()
                    .getWorkflow(wfId)
                    .orElse(null);
            if (wf != null) {
                wfTemplate = workflowProvider.get()
                        .getTemplate(wf.getTemplateId())
                        .orElse(null);
                Preconditions.checkNotNull(wfTemplate);
            }
        }
        if (wf == null) {
            //First time .. create workflow
            wfTemplate = templateSelector.get()
                    .determineTemplate(context)
                    .orElse(null);
            if (null == wfTemplate) {
                throw new StatesmanError("No matching workflow template found for context: " + stdPayload,
                                         ResponseCode.INVALID_OPERATION);
            }
            workflowProvider.get()
                    .saveWorkflow(new Workflow(wfId, wfTemplate.getId(),
                                               new DataObject(mapper.createObjectNode(),
                                                              wfTemplate.getStartState(),
                                                              date,
                                                              date)));
            wf = workflowProvider.get()
                    .getWorkflow(wfId)
                    .orElse(null);
            Preconditions.checkNotNull(wf);
        }
        final AppliedTransitions appliedTransitions
                = engine.get()
                .handle(new DataUpdate(wfId, context, new MergeDataAction()));
        log.debug("Workflow: {} with template: {} went through transitions: {}",
                  wfId, wfTemplate.getId(), appliedTransitions.getTransitions());
        return Response.ok()
                .entity(ImmutableMap.of("success", true))
                .build();
    }

    private String extractWorkflowId(JsonNode node, TransformationTemplate transformationTemplate) {
        val wfIdNode = node.at(transformationTemplate.getIdPath());
        return Strings.isNullOrEmpty(transformationTemplate.getIdPath()) || !isValid(node)
               ? UUID.randomUUID().toString()
               : wfIdNode.asText();
    }

    private boolean isValid(final JsonNode node) {
        return node != null
                && !node.isNull()
                && !node.isMissingNode();
    }

    final StepByStepTransformationTemplate.StepSelection selectStep(
            JsonNode node,
            StepByStepTransformationTemplate template) {
        return template.getTemplates()
                .stream()
                .filter(tmpl -> hopeLangEngine.evaluate(tmpl.getSelectionRule(), node))
                .findFirst()
                .orElse(null);
    }
}
