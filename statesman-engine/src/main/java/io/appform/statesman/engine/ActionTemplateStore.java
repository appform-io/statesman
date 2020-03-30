package io.appform.statesman.engine;

import io.appform.statesman.model.action.template.ActionTemplate;

import java.util.Optional;

public interface ActionTemplateStore {

    Optional<ActionTemplate> save(ActionTemplate actionTemplate);

    Optional<ActionTemplate> get(String actionTemplateId);
}
