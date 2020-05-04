package io.appform.statesman.engine.action;

import com.fasterxml.jackson.databind.JsonNode;
import io.appform.statesman.model.Workflow;

import java.util.Optional;

/**
 *
 */
public interface ActionExecutor {
    Optional<JsonNode> execute(String actionId, Workflow workflow);
}
