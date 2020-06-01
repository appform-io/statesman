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
import io.appform.statesman.engine.observer.ObservableEventBus;
import io.appform.statesman.engine.observer.events.IngressCallbackEvent;
import io.appform.statesman.engine.observer.events.StateTransitionEvent;
import io.appform.statesman.engine.utils.StringUtils;
import io.appform.statesman.model.*;
import io.appform.statesman.model.dataaction.impl.MergeDataAction;
import io.appform.statesman.server.callbacktransformation.TransformationTemplate;
import io.appform.statesman.server.callbacktransformation.TransformationTemplateVisitor;
import io.appform.statesman.server.callbacktransformation.TranslationTemplateType;
import io.appform.statesman.server.callbacktransformation.impl.OneShotTransformationTemplate;
import io.appform.statesman.server.callbacktransformation.impl.StepByStepTransformationTemplate;
import io.appform.statesman.server.dao.callback.CallbackTemplateProvider;
import io.appform.statesman.server.droppedcalldetector.DroppedCallDetector;
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
import java.util.Date;
import java.util.UUID;

/**
 *
 */
@Slf4j
@Singleton
public class IngressHandler {

    private final String TRANSLATOR_NOT_FOUND = "TRANSLATOR_NOT_FOUND";
    private final String COULD_NOT_TRANSLATE = "COULD_NOT_TRANSLATE";
    private final String WORKFLOW_TEMPLATE_NOT_FOUND = "WORKFLOW_TEMPLATE_NOT_FOUND";
    private final CallbackTemplateProvider callbackTemplateProvider;
    private final ObjectMapper mapper;
    private final HandleBarsService handleBarsService;
    private final Provider<StateTransitionEngine> engine;
    private final Provider<WorkflowProvider> workflowProvider;
    private final Provider<WorkflowTemplateSelector> templateSelector;
    private final Provider<ObservableEventBus> eventBus;
    private final DroppedCallDetector droppedCallDetector;
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
            Provider<ObservableEventBus> eventBus,
            DroppedCallDetector droppedCallDetector) {
        this.callbackTemplateProvider = callbackTemplateProvider;
        this.mapper = mapper;
        this.handleBarsService = handleBarsService;
        this.engine = engine;
        this.workflowProvider = workflowProvider;
        this.templateSelector = templateSelector;
        this.eventBus = eventBus;
        this.droppedCallDetector = droppedCallDetector;
        this.hopeLangEngine = HopeLangEngine.builder()
                .errorHandlingStrategy(new InjectValueErrorHandlingStrategy())
                .build();
        this.hopeRuleCache = Caffeine.newBuilder()
                .maximumSize(512)
                .build(hopeLangEngine::parse);
    }

    public boolean invokeEngineForOneShot(String ivrProvider, IngressCallback ingressCallback) throws IOException {
        log.debug("Processing ivr callback: {}", ingressCallback);
        val queryParams = parseQueryParams(ingressCallback);
        val node = mapper.valueToTree(queryParams);
        log.info("Processing node: {}", node);
        val tmplLookupKey = ivrProvider + "_" + StringUtils.normalize(node.at("/state/0").asText());
        val ingressCallbackEvent = IngressCallbackEvent.builder()
                .callbackType("OneShot")
                .ivrProvider(ivrProvider)
                .translatorId(tmplLookupKey)
                .queryString(ingressCallback.getQueryString())
                .bodyString(jsonString(ingressCallback.getBody()))
                .build();
        val transformationTemplate = getIngressTransformationTemplate(tmplLookupKey);
        if(null == transformationTemplate) {
            log.error("No matching translation template found for provider: {}", tmplLookupKey);
            ingressCallbackEvent.setErrorMessage(TRANSLATOR_NOT_FOUND);
            eventBus.get().publish(ingressCallbackEvent);
            return false;
        }
        val update = translateIvrPaylaod(transformationTemplate, tmplLookupKey, node);
        if(update == null) {
            ingressCallbackEvent.setErrorMessage(COULD_NOT_TRANSLATE);
            eventBus.get().publish(ingressCallbackEvent);
            return false;
        }
        val wfTemplate = templateSelector.get()
                .determineTemplate(update)
                .orElse(null);
        if (null == wfTemplate) {
            log.warn("No matching workflow template found for provider: {}, context: {}", ivrProvider, mapper.writeValueAsString(update));
            ingressCallbackEvent.setErrorMessage(WORKFLOW_TEMPLATE_NOT_FOUND);
            eventBus.get().publish(ingressCallbackEvent);
            return false;
        }
        var wfId = extractWorkflowId(node, transformationTemplate);
        val wfp = this.workflowProvider.get();
        while (wfp.workflowExists(wfId)) {
            wfId = UUID.randomUUID().toString();
        }
        val date = new Date();
        val dataObject = new DataObject(mapper.createObjectNode(), wfTemplate.getStartState(), date, date);
        val workflow = new Workflow(wfId, wfTemplate.getId(), dataObject, new Date(), new Date());
        wfp.saveWorkflow(workflow);
        final DataUpdate dataUpdate = new DataUpdate(wfId, update, new MergeDataAction());
        eventBus.get().publish(new StateTransitionEvent(wfTemplate, workflow, dataUpdate, null, null));
        final AppliedTransitions appliedTransitions
                = engine.get()
                .handle(dataUpdate);
        log.debug("Workflow: {} with template: {} went through transitions: {}",
                  wfId, wfTemplate.getId(), appliedTransitions.getTransitions());
        ingressCallbackEvent.setSmEngineTriggered(true);
        ingressCallbackEvent.setWorkflowId(dataUpdate.getWorkflowId());
        eventBus.get().publish(ingressCallbackEvent);
        return true;
    }

    public boolean invokeEngineForMultiStep(String ivrProvider, IngressCallback ingressCallback) throws IOException {
        log.debug("Processing ivr callback: {}", ingressCallback);
        log.debug("Processing callback from: {}: Payload: {}", ivrProvider, ingressCallback);
        val queryParams = parseQueryParams(ingressCallback);
        val node = mapper.valueToTree(queryParams);
        log.info("Processing node: {}", node);
        final String tmplLookupKey = ivrProvider + "_" + StringUtils.normalize(node.at("/state/0").asText());
        val ingressCallbackEvent = IngressCallbackEvent.builder()
                .callbackType("MultiStep")
                .ivrProvider(ivrProvider)
                .translatorId(tmplLookupKey)
                .queryString(ingressCallback.getQueryString())
                .bodyString(jsonString(ingressCallback.getBody()))
                .build();
        val transformationTemplate = getIngressTransformationTemplate(
                tmplLookupKey);
        if(null == transformationTemplate) {
            log.warn("No matching translation template found for provider {}. Key: {}. node: {} ",
                     ivrProvider, tmplLookupKey, node);
            ingressCallbackEvent.setErrorMessage(TRANSLATOR_NOT_FOUND);
            eventBus.get().publish(ingressCallbackEvent);
            return false;
        }
        val update = translateIvrPaylaod(transformationTemplate, tmplLookupKey, node);
        if(update == null) {
            ingressCallbackEvent.setErrorMessage(COULD_NOT_TRANSLATE);
            eventBus.get().publish(ingressCallbackEvent);
            return false;
        }
        val date = new Date();
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
            log.debug("Workflow could not be extracted for path: {}", transformationTemplate.getIdPath());
            //We have generated the id
            //Make sure wf id is not clashing with any existing id by chance
            while (wfp.workflowExists(wfId)) {
                wfId = UUID.randomUUID().toString();
            }
            log.debug("Generated ID: {}", wfId);
        }
        val dataUpdate = new DataUpdate(wfId, update, new MergeDataAction());
        if (wf == null) {
            log.debug("Will create new workflow for: {}", wfId);
            //First time .. create workflow
            wfTemplate = templateSelector.get()
                    .determineTemplate(update)
                    .orElse(null);
            if (null == wfTemplate) {
                log.warn("No matching workflow template found for provider: {}, node: {}", ivrProvider, mapper.writeValueAsString(update));
                ingressCallbackEvent.setErrorMessage(WORKFLOW_TEMPLATE_NOT_FOUND);
                eventBus.get().publish(ingressCallbackEvent);
                return false;
            }
            val dataNode = new DataObject(mapper.createObjectNode(), wfTemplate.getStartState(), date, date);
            val workflow = new Workflow(wfId, wfTemplate.getId(),
                    dataNode, new Date(), new Date());
            wfp.saveWorkflow(workflow);
            wf = wfp.getWorkflow(wfId).orElse(null);
            if (null == wf) {
                log.error("Workflow could not be created for: {}, context: {}", ivrProvider, mapper.writeValueAsString(update));
                ingressCallbackEvent.setErrorMessage("WORKFLOW_COULD_NOT_BE_CREATED");
                eventBus.get().publish(ingressCallbackEvent);
                return false;
            }
            eventBus.get().publish(new StateTransitionEvent(wfTemplate, workflow, dataUpdate, null, null));
            log.debug("Workflow created: {}", wf);
        }
        final AppliedTransitions appliedTransitions
                = engine.get()
                .handle(dataUpdate, new MergeDataAction());
        log.debug("Workflow: {} with template: {} went through transitions: {}",
                  wfId, wfTemplate.getId(), appliedTransitions.getTransitions());
        ingressCallbackEvent.setWorkflowId(dataUpdate.getWorkflowId());
        ingressCallbackEvent.setSmEngineTriggered(true);
        eventBus.get().publish(ingressCallbackEvent);
        return true;
    }

    public boolean invokeEngineForOBDCalls(String ivrProvider,
                                           String state,
                                           IngressCallback ingressCallback) throws IOException {
        log.debug("Processing OBD callback from: {}: Payload: {}", ivrProvider, ingressCallback);
        val node = ingressCallback.getBody();
        log.info("Processing node: {}", node);
        final String tmplLookupKey = ivrProvider + "_obd_" + StringUtils.normalize(node.at("/IVRID").asText());
        val ingressCallbackEvent = IngressCallbackEvent.builder()
                .callbackType("OBDCalls")
                .ivrProvider(ivrProvider)
                .translatorId(tmplLookupKey)
                .queryString(ingressCallback.getQueryString())
                .bodyString(jsonString(ingressCallback.getBody()))
                .build();
        val transformationTemplate = callbackTemplateProvider.getTemplate(tmplLookupKey, TranslationTemplateType.OBD_CALL_RESP)
                .orElse(null);
        if(null == transformationTemplate) {
            log.warn("No matching obd call resp translation template found for provider {}. Key: {}. node: {} ",
                     ivrProvider, tmplLookupKey, node);
            ingressCallbackEvent.setErrorMessage(TRANSLATOR_NOT_FOUND);
            eventBus.get().publish(ingressCallbackEvent);
            return false;
        }
        val tmpl = toOneShotTmpl(transformationTemplate);
        if (null == tmpl) {
            log.warn("No matching obd call resp transformation template found for provider: {}, context: {}",
                     ivrProvider, ingressCallback);
            ingressCallbackEvent.setErrorMessage(COULD_NOT_TRANSLATE);
            eventBus.get().publish(ingressCallbackEvent);
            return false;
        }
        val stdPayload = handleBarsService.transform(JsonNodeValueResolver.INSTANCE, tmpl.getTemplate(), node);
        log.info("stdPayload:{}", stdPayload);
        val update = mapper.readTree(stdPayload);
        var wfId = extractWorkflowId(node, transformationTemplate);
        val wfp = this.workflowProvider.get();
        val workflow = wfp.getWorkflow(wfId).orElse(null);
        if(null == workflow) {
            log.error("No existing workflow found for workflow id: {}, update: {}", wfId, update);
            return false;
        }
        val templateId = workflow.getTemplateId();
        val wfTemplate = wfp.getTemplate(templateId).orElse(null);
        if(null == wfTemplate) {
            log.error("No existing workflow template found for workflow id: {}, template: {}, update: {}",
                      wfId, templateId, update);
            ingressCallbackEvent.setErrorMessage(WORKFLOW_TEMPLATE_NOT_FOUND);
            eventBus.get().publish(ingressCallbackEvent);
            return false;
        }
        final DataUpdate dataUpdate = new DataUpdate(wfId, update, new MergeDataAction());
        final AppliedTransitions appliedTransitions
                = engine.get()
                .handle(dataUpdate);
        log.debug("Workflow: {} with template: {} went through transitions: {}",
                  wfId, wfTemplate.getId(), appliedTransitions.getTransitions());
        ingressCallbackEvent.setWorkflowId(dataUpdate.getWorkflowId());
        ingressCallbackEvent.setSmEngineTriggered(true);
        eventBus.get().publish(ingressCallbackEvent);
        return true;
    }

    public boolean invokeEngineForFormPost(String provider, IngressCallback callback) throws IOException {
        log.debug("Processing form post from: {}: Payload: {}", provider, callback);
        final String tmplLookupKey = provider + "_" + StringUtils.normalize(callback.getBody().at("/state").asText());
        val ingressCallbackEvent = IngressCallbackEvent.builder()
                .callbackType("formData")
                .ivrProvider(provider)
                .translatorId(tmplLookupKey)
                .formDataString(jsonString(callback.getBody()))
                .build();
        TransformationTemplate transformationTemplate
                = callbackTemplateProvider.getTemplate(tmplLookupKey, TranslationTemplateType.INGRESS)
                .orElse(null);
        if(null == transformationTemplate) {
            log.debug("Did not find template for {}, trying with {}", tmplLookupKey, provider);
            transformationTemplate = callbackTemplateProvider.getTemplate(provider, TranslationTemplateType.INGRESS)
                    .orElse(null);
            if(null == transformationTemplate) {
                log.warn("No matching form translation template found for key: {} or {}. node: {} ",
                         tmplLookupKey, provider, callback);
                ingressCallbackEvent.setErrorMessage(TRANSLATOR_NOT_FOUND);
                eventBus.get().publish(ingressCallbackEvent);
                return false;
            }
        }
        val tmpl = toOneShotTmpl(transformationTemplate);
        if (null == tmpl) {
            log.warn("Form data transformation template ineffective for provider: {}, context: {}",
                     provider, callback);
            ingressCallbackEvent.setErrorMessage(COULD_NOT_TRANSLATE);
            eventBus.get().publish(ingressCallbackEvent);
            return false;
        }
        val stdPayload = handleBarsService.transform(JsonNodeValueResolver.INSTANCE, tmpl.getTemplate(), callback.getBody());
        log.info("stdPayload:{}", stdPayload);
        val update = mapper.readTree(stdPayload);
        val wfTemplate = templateSelector.get()
                .determineTemplate(update)
                .orElse(null);
        if (null == wfTemplate) {
            log.warn("No matching workflow template found for provider: {}, context: {}", provider, stdPayload);
            ingressCallbackEvent.setErrorMessage(WORKFLOW_TEMPLATE_NOT_FOUND);
            eventBus.get().publish(ingressCallbackEvent);
            return false;
        }
        var wfId = UUID.randomUUID().toString();
        val wfp = this.workflowProvider.get();
        while (wfp.workflowExists(wfId)) {
            wfId = UUID.randomUUID().toString();
        }

        val date = new Date();
        val dataObject = new DataObject(mapper.createObjectNode(), wfTemplate.getStartState(), date, date);
        val workflow = new Workflow(wfId, wfTemplate.getId(), dataObject, new Date(), new Date());
        wfp.saveWorkflow(workflow);
        final DataUpdate dataUpdate = new DataUpdate(wfId, update, new MergeDataAction());
        eventBus.get().publish(new StateTransitionEvent(wfTemplate, workflow, dataUpdate, null, null));
        final AppliedTransitions appliedTransitions
                = engine.get()
                .handle(dataUpdate);
        log.debug("Workflow: {} with template: {} went through transitions: {}",
                  wfId, wfTemplate.getId(), appliedTransitions.getTransitions());
        ingressCallbackEvent.setWorkflowId(dataUpdate.getWorkflowId());
        ingressCallbackEvent.setSmEngineTriggered(true);
        eventBus.get().publish(ingressCallbackEvent);
        return true;
    }

    public boolean invokeEngineForRaw(String provider, IngressCallback callback) throws IOException {
        log.debug("Processing raw post from: {}: Payload: {}", provider, callback);
        val ingressCallbackEvent = IngressCallbackEvent.builder()
                .callbackType("raw")
                .ivrProvider(provider)
                .translatorId("_NONE_")
                .formDataString(jsonString(callback.getBody()))
                .build();
        val update = callback.getBody();
        log.info("stdPayload:{}", update);
        val wfTemplate = templateSelector.get()
                .determineTemplate(update)
                .orElse(null);
        if (null == wfTemplate) {
            log.warn("No matching workflow template found for provider: {}, context: {}", provider, update);
            ingressCallbackEvent.setErrorMessage(WORKFLOW_TEMPLATE_NOT_FOUND);
            eventBus.get().publish(ingressCallbackEvent);
            return false;
        }
        var wfId = UUID.randomUUID().toString();
        val wfp = this.workflowProvider.get();
        while (wfp.workflowExists(wfId)) {
            wfId = UUID.randomUUID().toString();
        }

        val date = new Date();
        val dataObject = new DataObject(mapper.createObjectNode(), wfTemplate.getStartState(), date, date);
        val workflow = new Workflow(wfId, wfTemplate.getId(), dataObject, new Date(), new Date());
        wfp.saveWorkflow(workflow);
        final DataUpdate dataUpdate = new DataUpdate(wfId, update, new MergeDataAction());
        eventBus.get().publish(new StateTransitionEvent(wfTemplate, workflow, dataUpdate, null, null));
        final AppliedTransitions appliedTransitions
                = engine.get()
                .handle(dataUpdate);
        log.debug("Workflow: {} with template: {} went through transitions: {}",
                  wfId, wfTemplate.getId(), appliedTransitions.getTransitions());
        ingressCallbackEvent.setWorkflowId(dataUpdate.getWorkflowId());
        ingressCallbackEvent.setSmEngineTriggered(true);
        eventBus.get().publish(ingressCallbackEvent);
        return true;
    }

    public JsonNode translateIngressIvrPayload(String providerKey,
                                               IngressCallback ingressCallback) throws IOException {
        val queryParams = parseQueryParams(ingressCallback);
        val node = mapper.valueToTree(queryParams);
        val transformationTemplate = getIngressTransformationTemplate(providerKey);
        if(null == transformationTemplate) {
           return null;
        }
        return translateIvrPaylaod(transformationTemplate, providerKey, node);
    }

    private JsonNode translateIvrPaylaod(TransformationTemplate transformationTemplate,
                                         String providerKey,
                                         JsonNode node) throws IOException {
        String template = transformationTemplate.accept(new TransformationTemplateVisitor<String>() {
            @Override
            public String visit(OneShotTransformationTemplate oneShotTransformationTemplate) {
                return oneShotTransformationTemplate.getTemplate();
            }

            @Override
            public String visit(StepByStepTransformationTemplate stepByStepTransformationTemplate) {
                val selectedStep = selectStep(node, stepByStepTransformationTemplate);
                if (null == selectedStep) {
                    log.warn("No matching step transformation template step found for providerKey: {}, node: {}",
                            providerKey, node);
                    return null;
                }
                return selectedStep.getTemplate();
            }
        });
        if (Strings.isNullOrEmpty(template)) {
            log.warn("No matching transformation template found for provider: {}, node:{}",
                    providerKey, node);
            return null;
        }
        val stdPayload =  handleBarsService.transform(JsonNodeValueResolver.INSTANCE, template, node);
        val update = mapper.readTree(stdPayload);
        if (update.isObject()) {
            ((ObjectNode) update).put("callDropped",
                                      droppedCallDetector.detectDroppedCall(transformationTemplate, update));
        }
        log.info("stdPayload:{}", update);
        return update;
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

    private StepByStepTransformationTemplate.StepSelection selectStep(
            JsonNode node,
            StepByStepTransformationTemplate template) {
        return template.getTemplates()
                .stream()
                .filter(tmpl -> hopeLangEngine.evaluate(hopeRuleCache.get(tmpl.getSelectionRule()), node))
                .findFirst()
                .orElse(null);
    }

    private String jsonString(JsonNode body) throws IOException {
        return body == null ? null : mapper.writeValueAsString(body);
    }
}
