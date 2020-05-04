package io.appform.statesman.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.appform.statesman.model.action.ActionType;
import io.appform.statesman.model.action.template.ActionTemplate;

/**
 *
 */

public interface Action<J extends ActionTemplate> {

    ActionType getType();

    JsonNode apply(J actionTemplate, Workflow workflow);
}
