package io.appform.statesman.engine;

import io.appform.statesman.model.action.template.ActionTemplate;

import java.util.Optional;

public interface ActionDao {

    Optional<ActionTemplate> get(String actionId);
}
