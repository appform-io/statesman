package io.appform.statesman.engine.action.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.common.base.Strings;
import com.google.inject.name.Named;
import io.appform.statesman.engine.ProviderSelector;
import io.appform.statesman.engine.action.BaseAction;
import io.appform.statesman.engine.action.ActionExecutor;
import io.appform.statesman.model.ActionImplementation;
import io.appform.statesman.model.Workflow;
import io.appform.statesman.model.action.ActionType;
import io.appform.statesman.model.action.template.RoutedActionTemplate;
import io.appform.statesman.model.exception.ResponseCode;
import io.appform.statesman.model.exception.StatesmanError;
import io.appform.statesman.publisher.EventPublisher;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;


@Slf4j
@Data
@Singleton
@ActionImplementation(name = "ROUTED")
public class RoutedAction extends BaseAction<RoutedActionTemplate> {

    private final ProviderSelector providerSelector;
    private Provider<ActionExecutor> actionExecutor;

    @Inject
    public RoutedAction(Provider<ActionExecutor> actionExecutor,
                        @Named("eventPublisher") final EventPublisher publisher,
                        ProviderSelector providerSelector,
                        ObjectMapper mapper) {
        super(publisher, mapper);
        this.providerSelector = providerSelector;
        this.actionExecutor = actionExecutor;
    }


    @Override
    public ActionType getType() {
        return ActionType.ROUTED;
    }


    @Override
    public JsonNode execute(RoutedActionTemplate routedActionTemplate, Workflow workflow) {
        String provider = providerSelector.provider(routedActionTemplate.getUseCase(), routedActionTemplate.getProviderTemplates().keySet(), workflow);
        if (Strings.isNullOrEmpty(provider)) {
            throw new StatesmanError("No provider found for action:" + routedActionTemplate.getTemplateId(),
                    ResponseCode.NO_PROVIDER_FOUND);
        }
        return actionExecutor.get().execute(routedActionTemplate.getProviderTemplates().get(provider), workflow)
                .orElse(NullNode.getInstance());
    }
}
