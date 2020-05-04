package io.appform.statesman.engine.action;

import com.fasterxml.jackson.databind.JsonNode;
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

    public JsonNode execute(String actionId, Workflow workflow) {
        return actionTemplateStore.get().get(actionId)
                .map(actionTemplate -> execute(workflow, actionTemplate))
                .orElse(null);
    }

    private JsonNode execute(Workflow workflow, ActionTemplate actionTemplate) {
        return actionRegistry.get().get(actionTemplate.getType().name())
                .map(action -> action.apply(actionTemplate, workflow))
                .orElse(null);
    }

}
