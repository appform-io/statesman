package io.appform.statesman.server.ingress;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.JsonNodeValueResolver;
import com.google.common.base.Strings;
import io.appform.statesman.engine.StateTransitionEngine;
import io.appform.statesman.engine.WorkflowProvider;
import io.appform.statesman.engine.handlebars.HandleBarsService;
import io.appform.statesman.model.AppliedTransitions;
import io.appform.statesman.model.DataUpdate;
import io.appform.statesman.model.dataaction.impl.MergeDataAction;
import io.appform.statesman.server.callbacktransformation.TransformationTemplate;
import io.appform.statesman.server.callbacktransformation.TransformationTemplateVisitor;
import io.appform.statesman.server.callbacktransformation.TranslationTemplateType;
import io.appform.statesman.server.callbacktransformation.impl.OneShotTransformationTemplate;
import io.appform.statesman.server.callbacktransformation.impl.StepByStepTransformationTemplate;
import io.appform.statesman.server.dao.callback.CallbackTemplateProvider;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Optional;

/**
 *
 */
@Singleton
@Slf4j
public class ServiceProviderCallbackHandler {
    private final Provider<CallbackTemplateProvider> callbackTemplateProvider;
    private Provider<HandleBarsService> handleBarsService;
    private final Provider<StateTransitionEngine> engine;
    private final Provider<WorkflowProvider> workflowProvider;
    private ObjectMapper mapper;

    @Inject
    public ServiceProviderCallbackHandler(
            Provider<CallbackTemplateProvider> callbackTemplateProvider,
            Provider<HandleBarsService> handleBarsService,
            Provider<StateTransitionEngine> engine,
            Provider<WorkflowProvider> workflowProvider,
            ObjectMapper mapper) {
        this.callbackTemplateProvider = callbackTemplateProvider;
        this.handleBarsService = handleBarsService;
        this.engine = engine;
        this.workflowProvider = workflowProvider;
        this.mapper = mapper;
    }

    public boolean handleServiceProviderCallback(final String serviceProviderId, JsonNode incomingData)  throws IOException {
        val tmpl = callbackTemplateProvider.get().getTemplate(serviceProviderId, TranslationTemplateType.PROVIDER_CALLBACK).orElse(null);
        if(null == tmpl) {
            log.warn("No template found for callback from service provider: {}. Cannot proceed.", serviceProviderId);
            return false;
        }
        if(Strings.isNullOrEmpty(tmpl.getIdPath())) {
            log.warn("Workflow id detection template missing for service provider: {}. Cannot proceed.",
                     serviceProviderId);
            return false;
        }

        val wfId = workflowId(tmpl, incomingData).orElse(null);
        if(Strings.isNullOrEmpty(wfId)) {
            log.warn("Could not extract workflow id for service provider callback: {} with data: {} at path: {}. Cannot proceed.",
                     serviceProviderId, incomingData, tmpl.getIdPath());
            return false;
        }
        val wf = workflowProvider.get().getWorkflow(wfId).orElse(null);
        if(null == wf) {
            log.warn("No workflow found for provider callback. Workflow ID: {}. provider Id: {}. Cannot proceed.",
                     serviceProviderId, wfId);
            return false;
        }
        if(wf.getDataObject().getCurrentState().isTerminal()) {
            log.warn("Workflow already in terminal state. Workflow ID: {}. provider Id: {}. State: {}. Cannot proceed.",
                     serviceProviderId, wfId, wf.getDataObject().getCurrentState());
            return false;
        }
        val translatedProviderCallbackPayload = handleBarsService.get().transform(JsonNodeValueResolver.INSTANCE, getTemplate(tmpl), incomingData);
        log.info("translatedProviderCallbackPayload:{}", translatedProviderCallbackPayload);
        val context = mapper.readTree(translatedProviderCallbackPayload);
        val dataUpdate = new DataUpdate(wfId, context, new MergeDataAction());
        final AppliedTransitions appliedTransitions = engine.get().handle(dataUpdate);
        log.debug("Workflow: {} went through transitions: {}.", wfId, appliedTransitions.getTransitions());
        return true;
    }

    private Optional<String> workflowId(final TransformationTemplate template, JsonNode data) {
        val node = data.at(template.getIdPath());
        if(null == node || node.isMissingNode() || node.isNull() || !node.isValueNode()) {
            return Optional.empty();
        }
        return Optional.ofNullable(node.asText());
    }

    private static String getTemplate(TransformationTemplate transformationTemplate) {
        return transformationTemplate.accept(new TransformationTemplateVisitor<String>() {
            @Override
            public String visit(OneShotTransformationTemplate oneShotTransformationTemplate) {
                return oneShotTransformationTemplate.getTemplate();
            }

            @Override
            public String visit(StepByStepTransformationTemplate stepByStepTransformationTemplate) {
                return stepByStepTransformationTemplate.getTemplates().stream().map(StepByStepTransformationTemplate.StepSelection::getTemplate).findFirst().orElse(null);
            }
        });
    }
}
