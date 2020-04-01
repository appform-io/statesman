package io.appform.statesman.engine.action.impl;

import com.google.common.base.Strings;
import io.appform.statesman.engine.ProviderSelector;
import io.appform.statesman.engine.action.ActionExecutor;
import io.appform.statesman.engine.action.BaseAction;
import io.appform.statesman.model.ActionImplementation;
import io.appform.statesman.model.Workflow;
import io.appform.statesman.model.action.ActionType;
import io.appform.statesman.model.action.template.RoutedHttpActionTemplate;
import io.appform.statesman.model.exception.ResponseCode;
import io.appform.statesman.model.exception.StatesmanError;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;


@Slf4j
@Data
@Singleton
@ActionImplementation(name = "ROUTED_HTTP")
public class RoutedHttpAction extends BaseAction<RoutedHttpActionTemplate> {

    private final ProviderSelector providerSelector;
    private Provider<ActionExecutor> actionExecutor;

    @Inject
    public RoutedHttpAction(Provider<ActionExecutor> actionExecutor,
                            ProviderSelector providerSelector) {
        this.providerSelector = providerSelector;
        this.actionExecutor = actionExecutor;
    }


    @Override
    public ActionType getType() {
        return ActionType.ROUTED_HTTP;
    }

    @Override
    protected void fallback(RoutedHttpActionTemplate actionTemplate, Workflow workflow) {
        //TODO
    }

    @Override
    public void execute(RoutedHttpActionTemplate routedHttpActionTemplate, Workflow workflow) {
        String provider = providerSelector.provider(routedHttpActionTemplate.getProviderType(), routedHttpActionTemplate.getProviderTemplates().keySet(), workflow);
        if (Strings.isNullOrEmpty(provider)) {
            throw new StatesmanError("No provider found for action:" + routedHttpActionTemplate.getTemplateId(),
                    ResponseCode.NO_PROVIDER_FOUND);
        }
        actionExecutor.get().execute(workflow, routedHttpActionTemplate.getProviderTemplates().get(provider));
    }
}
