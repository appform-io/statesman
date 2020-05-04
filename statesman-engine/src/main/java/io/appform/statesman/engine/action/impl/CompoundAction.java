package io.appform.statesman.engine.action.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.name.Named;
import io.appform.statesman.engine.action.ActionExecutor;
import io.appform.statesman.engine.action.BaseAction;
import io.appform.statesman.model.ActionImplementation;
import io.appform.statesman.model.Workflow;
import io.appform.statesman.model.action.ActionType;
import io.appform.statesman.model.action.template.CompoundActionTemplate;
import io.appform.statesman.publisher.EventPublisher;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Objects;


@Slf4j
@Data
@Singleton
@ActionImplementation(name = "COMPOUND")
public class CompoundAction extends BaseAction<CompoundActionTemplate> {

    private Provider<ActionExecutor> actionExecutor;

    @Inject
    public CompoundAction(Provider<ActionExecutor> actionExecutor,
                          @Named("eventPublisher") final EventPublisher publisher,
                          ObjectMapper mapper) {
        super(publisher, mapper);
        this.actionExecutor = actionExecutor;
    }


    @Override
    public ActionType getType() {
        return ActionType.COMPOUND;
    }

    @Override
    public JsonNode execute(CompoundActionTemplate compoundActionTemplate, Workflow workflow) {
        ObjectNode response = mapper.createObjectNode();
        compoundActionTemplate.getActionTemplates().stream()
                .map(actionTemplate -> actionExecutor.get().execute(actionTemplate, workflow))
                .filter(Objects::nonNull)
                .forEach(actionResponse -> response.setAll((ObjectNode) actionResponse));
        return response;
    }

}
