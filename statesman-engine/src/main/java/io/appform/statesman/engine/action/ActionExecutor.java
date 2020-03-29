package io.appform.statesman.engine.action;

import com.google.inject.Inject;
import io.appform.statesman.engine.ActionDao;
import io.appform.statesman.model.Workflow;

import javax.inject.Provider;

public class ActionExecutor {

    Provider<ActionRegistry> actionRegistry;
    Provider<ActionDao> actionDao;

    @Inject
    public void ActionExecutor(final Provider<ActionRegistry> actionRegistry,
                               final Provider<ActionDao> actionDao) {
        this.actionRegistry = actionRegistry;
        this.actionDao = actionDao;
    }

    public void execute(String actionId, Workflow workflow) {
        actionDao.get().get(actionId)
        .ifPresent(actionTemplate -> actionRegistry.get().get(actionTemplate.getType().name())
                .ifPresent(action -> action.apply(actionTemplate, workflow)));
    }
}
