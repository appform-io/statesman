package io.appform.statesman.model;

import io.appform.statesman.model.action.ActionType;
import io.appform.statesman.model.action.template.ActionTemplate;

/**
 *
 */

public interface Action<J extends ActionTemplate> {

    ActionType getType();

    void apply(J actionTemplate, Workflow workflow);
}
