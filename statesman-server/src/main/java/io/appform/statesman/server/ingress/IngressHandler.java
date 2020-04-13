package io.appform.statesman.server.ingress;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.jknack.handlebars.JsonNodeValueResolver;
import com.google.common.base.Strings;
import io.appform.hope.core.Evaluatable;
import io.appform.hope.core.exceptions.errorstrategy.InjectValueErrorHandlingStrategy;
import io.appform.hope.lang.HopeLangEngine;
import io.appform.statesman.engine.StateTransitionEngine;
import io.appform.statesman.engine.WorkflowProvider;
import io.appform.statesman.engine.handlebars.HandleBarsService;
import io.appform.statesman.engine.utils.StringUtils;
import io.appform.statesman.model.*;
import io.appform.statesman.model.dataaction.impl.MergeDataAction;
import io.appform.statesman.server.callbacktransformation.TransformationTemplate;
import io.appform.statesman.server.callbacktransformation.TransformationTemplateVisitor;
import io.appform.statesman.server.callbacktransformation.TranslationTemplateType;
import io.appform.statesman.server.callbacktransformation.impl.OneShotTransformationTemplate;
import io.appform.statesman.server.callbacktransformation.impl.StepByStepTransformationTemplate;
import io.appform.statesman.server.dao.callback.CallbackTemplateProvider;
import io.appform.statesman.server.droppedcalldetector.IvrDropDetectionConfig;
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
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.*;
import java.util.stream.StreamSupport;

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
    private final IvrDropDetectionConfig dropDetectionConfig;
    private final HopeLangEngine hopeLangEngine;
    private final LoadingCache<String, Evaluatable> hopeRuleCache;

    @Inject
    public IngressHandler(
            CallbackTemplateProvider callbackTemplateProvider,
            final ObjectMapper mapper,
            HandleBarsService handleBarsService,
            Provider<StateTransitionEngine> engine,
            Provider<WorkflowProvider> workflowProvider,
            Provider<WorkflowTemplateSelector> templateSelector,
            IvrDropDetectionConfig dropDetectionConfig) {
        this.callbackTemplateProvider = callbackTemplateProvider;
        this.mapper = mapper;
        this.handleBarsService = handleBarsService;
        this.engine = engine;
        this.workflowProvider = workflowProvider;
        this.templateSelector = templateSelector;
        this.dropDetectionConfig = dropDetectionConfig;
        this.hopeLangEngine = HopeLangEngine.builder()
                .errorHandlingStrategy(new InjectValueErrorHandlingStrategy())
                .build();
        this.hopeRuleCache = Caffeine.newBuilder()
                .maximumSize(512)
                .build(hopeLangEngine::parse);
    }

    public boolean invokeEngineForOneShot(String ivrProvider, IngressCallback ingressCallback) throws IOException {
        val queryParams = parseQueryParams(ingressCallback);
        val node = mapper.valueToTree(queryParams);
        val tmplLookupKey = ivrProvider + "_" + StringUtils.normalize(node.at("/state/0").asText());
        val transformationTemplate = getIngressTransformationTemplate(tmplLookupKey);
        if(null == transformationTemplate) {
            log.error("No matching translation template found for provider: " + ivrProvider);
            return false;
        }
        val tmpl = toOneShotTmpl(transformationTemplate);
        if (null == tmpl) {
            log.warn("No matching transformation template found for provider: {}, context: {}",
                     ivrProvider, ingressCallback);
            return false;
        }
        val stdPayload = handleBarsService.transform(JsonNodeValueResolver.INSTANCE, tmpl.getTemplate(), node);
        log.info("stdPayload:{}", stdPayload);
        val update = mapper.readTree(stdPayload);
        if (update.isObject()) {
            ((ObjectNode) update).put("callDropped", isDroppedCallSingleShot(ivrProvider, node, dropDetectionConfig));
        }
        val wfTemplate = templateSelector.get()
                .determineTemplate(update)
                .orElse(null);
        if (null == wfTemplate) {
            log.warn("No matching workflow template found for provider: {}, context: {}", ivrProvider, stdPayload);
            return false;
        }
        var wfId = extractWorkflowId(node, transformationTemplate);
        val wfp = this.workflowProvider.get();
        while (wfp.workflowExists(wfId)) {
            wfId = UUID.randomUUID().toString();
        }
        val date = new Date();
        val dataObject = new DataObject(mapper.createObjectNode(), wfTemplate.getStartState(), date, date);
        wfp.saveWorkflow(new Workflow(wfId, wfTemplate.getId(), dataObject, new Date(), new Date()));
        final AppliedTransitions appliedTransitions
                = engine.get()
                .handle(new DataUpdate(wfId, update, new MergeDataAction()));
        log.debug("Workflow: {} with template: {} went through transitions: {}",
                  wfId, wfTemplate.getId(), appliedTransitions.getTransitions());
        return true;
    }

    public boolean invokeEngineForMultiStep(String ivrProvider, IngressCallback ingressCallback) throws IOException {
        log.debug("Processing callback from: {}: Payload: {}", ivrProvider, ingressCallback);
        val queryParams = parseQueryParams(ingressCallback);
        val node = mapper.valueToTree(queryParams);
        log.info("Processing node: {}", node);
        final String tmplLookupKey = ivrProvider + "_" + StringUtils.normalize(node.at("/state/0").asText());
        val transformationTemplate = getIngressTransformationTemplate(
                tmplLookupKey);
        if(null == transformationTemplate) {
            log.warn("No matching translation template found for provider {}. Key: {}. node: {} ",
                     ivrProvider, tmplLookupKey, node);
            return false;
        }
        val tmpl = toMultiStepTemplate(transformationTemplate);
        if (null == tmpl) {
            log.warn("No matching step transformation template found for provider: {}, node: {}",
                     ivrProvider, node);
            return false;
        }
        val date = new Date();
        val selectedStep = selectStep(node, tmpl);
        if (null == selectedStep) {
            log.warn("No matching step transformation template step found for provider: {}, node: {}",
                     ivrProvider, node);
            return false;
        }
        val stdPayload = handleBarsService.transform(JsonNodeValueResolver.INSTANCE, selectedStep.getTemplate(), node);
        val update = mapper.readTree(stdPayload);
        log.debug("Translated payload: {}", update);
        if (update.isObject()) {
            ((ObjectNode) update).put("callDropped", isDroppedCallSingleShot(ivrProvider, node, dropDetectionConfig));
        }
        val wfIdNode = node.at(transformationTemplate.getIdPath());
        log.debug("WFID node: {}", wfIdNode);
        String wfId = UUID.randomUUID().toString();
        Workflow wf = null;
        WorkflowTemplate wfTemplate = null;
        val wfp = this.workflowProvider.get();
        if (isValid(wfIdNode)) {
            //We found ID node .. so we have to reuse if present
            wfId = extractWorkflowId(node, transformationTemplate);
            log.debug("Workflow id extracted: {}", wfId);
            wf = wfp.getWorkflow(wfId).orElse(null);
            if (wf != null) {
                log.debug("Existing workflow found for : {}", wfId);
                //Found existing workflow
                wfTemplate = wfp.getTemplate(wf.getTemplateId()).orElse(null);
                if (null == wfTemplate) {
                    log.warn("No matching workflow template found for provider: {}, node: {}",
                              ivrProvider,
                              node);
                    return false;
                }
            }
        }
        else {
            log.debug("Workflow could not be extracted for path: {}", tmpl.getIdPath());
            //We have generated the id
            //Make sure wf id is not clashing with any existing id by chance
            while (wfp.workflowExists(wfId)) {
                wfId = UUID.randomUUID().toString();
            }
            log.debug("Generated ID: {}", wfId);
        }
        if (wf == null) {
            log.debug("Will create new workflow for: {}", wfId);
            //First time .. create workflow
            wfTemplate = templateSelector.get()
                    .determineTemplate(update)
                    .orElse(null);
            if (null == wfTemplate) {
                log.warn("No matching workflow template found for provider: {}, node: {}", ivrProvider, stdPayload);
                return false;
            }
            val dataNode = new DataObject(mapper.createObjectNode(), wfTemplate.getStartState(), date, date);
            wfp.saveWorkflow(new Workflow(wfId, wfTemplate.getId(),
                                          dataNode, new Date(), new Date()));
            wf = wfp.getWorkflow(wfId).orElse(null);
            if (null == wf) {
                log.error("Workflow could not be created for: {}, context: {}", ivrProvider, stdPayload);
                return false;
            }
            log.debug("Workflow created: {}", wf);
        }
        final AppliedTransitions appliedTransitions
                = engine.get()
                .handle(new DataUpdate(wfId, update, new MergeDataAction()), new MergeDataAction());
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

    public static MultivaluedMap<String, String> parseQueryParams(IngressCallback ingressCallback) {
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
        return callbackTemplateProvider.getTemplate(ivrProvider, TranslationTemplateType.INGRESS)
                .orElse(null);
    }

    final StepByStepTransformationTemplate.StepSelection selectStep(
            JsonNode node,
            StepByStepTransformationTemplate template) {
        return template.getTemplates()
                .stream()
                .filter(tmpl -> hopeLangEngine.evaluate(hopeRuleCache.get(tmpl.getSelectionRule()), node))
                .findFirst()
                .orElse(null);
    }

    public static boolean isDroppedCallSingleShot(
            final String provider, JsonNode jsonNode, IvrDropDetectionConfig dropDetectionConfig) {
        if (!dropDetectionConfig.isEnabled()) {
            return false;
        }
        val detectionPatterns = dropDetectionConfig.getDetectionPatterns();
        val patterns = null == detectionPatterns
                       ? Collections.<String>emptyList()
                       : detectionPatterns.getOrDefault(provider, Collections.emptyList());
        if (patterns.isEmpty()) {
            log.debug("No call drop detection patterns found for provider: {}", provider);
            return false;
        }
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(jsonNode.fields(), Spliterator.ORDERED), false)
                .filter(field -> patterns.stream()
                        .anyMatch(pattern -> field.getKey().matches(pattern)))
                .anyMatch(field -> field.getValue().isArray()
                        && (field.getValue().size() == 0
                        || (field.getValue().size() == 1
                        && Strings.isNullOrEmpty(field.getValue().get(0).asText()))));
    }
}
