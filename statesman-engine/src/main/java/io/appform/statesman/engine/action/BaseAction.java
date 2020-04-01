package io.appform.statesman.engine.action;

import io.appform.statesman.model.Action;
import io.appform.statesman.model.Workflow;
import io.appform.statesman.model.action.template.ActionTemplate;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public abstract class BaseAction<T extends ActionTemplate> implements Action<T> {

    protected abstract void fallback(T actionTemplate, Workflow workflow);

    protected abstract void execute(T actionTemplate, Workflow workflow);

    @Override
    public void apply(T actionTemplate, Workflow workflow) {
        //TODO: ADD retrier
        try {
            execute(actionTemplate, workflow);
        } catch (Exception e) {
            log.error("Error while executing action", e);
            fallback(actionTemplate, workflow);
        }

    }
}