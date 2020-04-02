package io.appform.statesman.server.ingress;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.JsonNodeValueResolver;
import com.google.common.base.Strings;
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
import io.appform.statesman.server.callbacktransformation.TranslationTemplateType;
import io.appform.statesman.server.callbacktransformation.impl.OneShotTransformationTemplate;
import io.appform.statesman.server.callbacktransformation.impl.StepByStepTransformationTemplate;
import io.appform.statesman.server.dao.callback.CallbackTemplateProvider;
import io.appform.statesman.server.evaluator.WorkflowTemplateSelector;
import io.appform.statesman.server.requests.IngressCallback;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.glassfish.jersey.internal.util.collection.ImmutableMultivaluedMap;
import org.glassfish.jersey.uri.UriComponent;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

/**
 *
 */
@Slf4j
@Singleton
public class IngressHandler {
    private final CallbackTemplateProvider callbackTemplateProvider;
    private final ObjectMapper mapper;
    private final HandleBarsService handleBarsService;
    private final Provider<StateTransitionEngine> engine;
    private final Provider<WorkflowProvider> workflowProvider;
    private final Provider<WorkflowTemplateSelector> templateSelector;
    private final HopeLangEngine hopeLangEngine;

    @Inject
    public IngressHandler(
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

    public boolean invokeEngineForOneShot(String ivrProvider, IngressCallback ingressCallback) throws IOException {
        val queryParams = parseQueryParams(ingressCallback);
        val node = mapper.valueToTree(queryParams);
        val transformationTemplate = getIngressTransformationTemplate(ivrProvider);
        val tmpl = toOneShotTmpl(transformationTemplate);
        if(null == tmpl) {
            log.error("No matching transformation template found for provider: {}, context: {}",
                      ivrProvider, ingressCallback);
            return false;
        }
        val stdPayload = handleBarsService.transform(JsonNodeValueResolver.INSTANCE, tmpl.getTemplate(), node);
        log.info("stdPayload:{}", stdPayload);
        val context = mapper.readTree(stdPayload);
        val wfTemplate = templateSelector.get()
                .determineTemplate(context)
                .orElse(null);
        if (null == wfTemplate) {
            log.error("No matching workflow template found for provider: {}, context: {}", ivrProvider, stdPayload);
            return false;
        }
        var wfId = extractWorkflowId(node, transformationTemplate);
        val wfp = this.workflowProvider.get();
        while (wfp.workflowExists(wfId)) {
            wfId = UUID.randomUUID().toString();
        }
        val date = new Date();
        val dataObject = new DataObject(mapper.createObjectNode(), wfTemplate.getStartState(), date, date);
        wfp.saveWorkflow(new Workflow(wfId, wfTemplate.getId(), dataObject));
        final AppliedTransitions appliedTransitions
                = engine.get()
                .handle(new DataUpdate(wfId, context, new MergeDataAction()));
        log.info("Workflow: {} with template: {} went through transitions: {}",
                 wfId, wfTemplate.getId(), appliedTransitions.getTransitions());
        return true;
    }

    public boolean invokeEngineForMultiStep(String ivrProvider, IngressCallback ingressCallback) throws IOException {
        val queryParams = parseQueryParams(ingressCallback);
        val node = mapper.valueToTree(queryParams);
        val transformationTemplate = getIngressTransformationTemplate(ivrProvider);
        val tmpl = toMultiStepTemplate(transformationTemplate);
        if(null == tmpl) {
            log.error("No matching step transformation template found for provider: {}, context: {}",
                      ivrProvider, ingressCallback);
            return false;
        }
        val date = new Date();
        val selectedStep = selectStep(node, tmpl);
        if(null == selectedStep) {
            log.error("No matching step transformation template step found for provider: {}, context: {}",
                      ivrProvider, ingressCallback);
            return false;
        }
        val stdPayload = handleBarsService.transform(JsonNodeValueResolver.INSTANCE, selectedStep.getTemplate(), node);
        val context = mapper.readTree(stdPayload);
        val wfIdNode = node.at(transformationTemplate.getIdPath());
        String wfId = UUID.randomUUID().toString();
        Workflow wf = null;
        WorkflowTemplate wfTemplate = null;
        val wfp = this.workflowProvider.get();
        if (isValid(wfIdNode)) {
            //We found ID node .. so we have to reuse if present
            wfId = extractWorkflowId(node, transformationTemplate);
            wf = wfp.getWorkflow(wfId).orElse(null);
            if (wf != null) {
                //Found existing workflow
                wfTemplate = wfp.getTemplate(wf.getTemplateId()).orElse(null);
                if (null == wfTemplate) {
                    log.error("No matching workflow template found for provider: {}, context: {}", ivrProvider, stdPayload);
                    return false;
                }
            }
        }
        else {
            //We have generated the id
            //Make sure wf id is not clashing with any existing id by chance
            while (wfp.workflowExists(wfId)) {
                wfId = UUID.randomUUID().toString();
            }
        }
        if (wf == null) {
            //First time .. create workflow
            wfTemplate = templateSelector.get()
                    .determineTemplate(context)
                    .orElse(null);
            if (null == wfTemplate) {
                log.error("No matching workflow template found for provider: {}, context: {}", ivrProvider, stdPayload);
                return false;
            }
            val dataNode = new DataObject(mapper.createObjectNode(), wfTemplate.getStartState(), date, date);
            wfp.saveWorkflow(new Workflow(wfId, wfTemplate.getId(),
                                               dataNode));
            wf = wfp.getWorkflow(wfId).orElse(null);
            if (null == wf) {
                log.error("Workflow could not be created for: {}, context: {}", ivrProvider, stdPayload);
                return false;
            }
        }
        final AppliedTransitions appliedTransitions
                = engine.get()
                .handle(new DataUpdate(wfId, context, new MergeDataAction()));
        log.debug("Workflow: {} with template: {} went through transitions: {}",
                  wfId, wfTemplate.getId(), appliedTransitions.getTransitions());
        return true;
    }


    private static OneShotTransformationTemplate toOneShotTmpl(TransformationTemplate transformationTemplate) {
        return transformationTemplate.accept(new TransformationTemplateVisitor<OneShotTransformationTemplate>() {
            @Override
            public OneShotTransformationTemplate visit(OneShotTransformationTemplate oneShotTransformationTemplate) {
                return oneShotTransformationTemplate;
            }

            @Override
            public OneShotTransformationTemplate visit(StepByStepTransformationTemplate stepByStepTransformationTemplate) {
                return null;
            }
        });
    }

    private static StepByStepTransformationTemplate toMultiStepTemplate(TransformationTemplate transformationTemplate) {
        return transformationTemplate.accept(new TransformationTemplateVisitor<StepByStepTransformationTemplate>() {
            @Override
            public StepByStepTransformationTemplate visit(OneShotTransformationTemplate oneShotTransformationTemplate) {
                return null;
            }

            @Override
            public StepByStepTransformationTemplate visit(StepByStepTransformationTemplate stepByStepTransformationTemplate) {
                return stepByStepTransformationTemplate;
            }
        });
    }

    private static ImmutableMultivaluedMap<String, String> parseQueryParams(IngressCallback ingressCallback) {
        return new ImmutableMultivaluedMap<>(
                UriComponent.decodeQuery(ingressCallback.getQueryString(), true));
    }

    private static String extractWorkflowId(JsonNode node, TransformationTemplate transformationTemplate) {
        val wfIdNode = node.at(transformationTemplate.getIdPath());
        return Strings.isNullOrEmpty(transformationTemplate.getIdPath()) || !isValid(node)
               ? UUID.randomUUID().toString()
               : wfIdNode.asText();
    }

    private static boolean isValid(final JsonNode node) {
        return node != null
                && !node.isNull()
                && !node.isMissingNode();
    }

    private TransformationTemplate getIngressTransformationTemplate(String ivrProvider) {
        val transformationTemplate = callbackTemplateProvider.getTemplate(ivrProvider, TranslationTemplateType.INGRESS)
                .orElse(null);
        if (null == transformationTemplate) {
            throw new StatesmanError("No matching translation template found for provider: " + ivrProvider,
                                     ResponseCode.INVALID_OPERATION);
        }
        return transformationTemplate;
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
