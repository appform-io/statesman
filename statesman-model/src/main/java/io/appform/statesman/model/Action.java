package io.appform.statesman.model;

import io.appform.statesman.model.action.template.ActionTemplate;

/**
 *
 */
@FunctionalInterface
public interface Action {
    void apply(ActionTemplate actionTemplate, Workflow workflow);
}
