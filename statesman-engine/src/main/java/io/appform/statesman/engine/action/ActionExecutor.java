package io.appform.statesman.engine.action;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.appform.statesman.engine.ActionTemplateStore;
import io.appform.statesman.model.Workflow;
import io.appform.statesman.model.action.template.ActionTemplate;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Provider;

@Slf4j
@Singleton
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
                .ifPresent(actionTemplate -> execute(workflow, actionTemplate));
    }

    public void execute(Workflow workflow, ActionTemplate actionTemplate) {
        actionRegistry.get().get(actionTemplate.getType().name())
                .ifPresent(action -> action.apply(actionTemplate, workflow));
    }

}
