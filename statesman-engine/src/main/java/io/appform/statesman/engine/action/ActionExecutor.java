package io.appform.statesman.engine.action;

import com.google.inject.Inject;
import io.appform.statesman.engine.ActionTemplateStore;
import io.appform.statesman.model.Workflow;

import javax.inject.Provider;

public class ActionExecutor {

    Provider<ActionRegistry> actionRegistry;
    Provider<ActionTemplateStore> actionTemplateStore;

    @Inject
    public void ActionExecutor(final Provider<ActionRegistry> actionRegistry,
                               final Provider<ActionTemplateStore> actionTemplateStore) {
        this.actionRegistry = actionRegistry;
        this.actionTemplateStore = actionTemplateStore;
    }

    public void execute(String actionId, Workflow workflow) {
        actionTemplateStore.get().get(actionId)
        .ifPresent(actionTemplate -> actionRegistry.get().get(actionTemplate.getType().name())
                .ifPresent(action -> action.apply(actionTemplate, workflow)));
    }
}
