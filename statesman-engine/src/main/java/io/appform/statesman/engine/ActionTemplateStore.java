package io.appform.statesman.engine;

import io.appform.statesman.model.action.template.ActionTemplate;

import java.util.List;
import java.util.Optional;

public interface ActionTemplateStore {

    Optional<ActionTemplate> create(ActionTemplate actionTemplate);

    Optional<ActionTemplate> get(String actionTemplateId);

    List<ActionTemplate> all();

    Optional<ActionTemplate> update(ActionTemplate actionTemplate);

}
