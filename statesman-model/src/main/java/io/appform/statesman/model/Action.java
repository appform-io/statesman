package io.appform.statesman.model;

import io.appform.statesman.model.action.template.ActionTemplate;

/**
 *
 */
@FunctionalInterface
public interface Action<J extends ActionTemplate> {
    void apply(J actionTemplate, Workflow workflow);
}
