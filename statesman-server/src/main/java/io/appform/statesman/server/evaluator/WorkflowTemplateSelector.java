package io.appform.statesman.server.evaluator;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.appform.hope.core.Evaluatable;
import io.appform.hope.core.exceptions.errorstrategy.InjectValueErrorHandlingStrategy;
import io.appform.hope.lang.HopeLangEngine;
import io.appform.statesman.engine.WorkflowProvider;
import io.appform.statesman.model.WorkflowTemplate;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class WorkflowTemplateSelector {

    private WorkflowProvider workflowProvider;
    private HopeLangEngine hopeLangEngine;

    @Inject
    @Builder
    public WorkflowTemplateSelector(WorkflowProvider workflowProvider) {
        this.workflowProvider = workflowProvider;
        this.hopeLangEngine = HopeLangEngine.builder()
                .errorHandlingStrategy(new InjectValueErrorHandlingStrategy())
                .build();
    }

    public Optional<WorkflowTemplate> determineTemplate(JsonNode translatedPayload) {

        List<WorkflowTemplateContext> parsedTemplates = workflowProvider.getAll()
                .stream()
                .flatMap(template -> template.getRules().stream().map(attribute -> {
                    Evaluatable parsedTemplateRule;
                    try {
                        parsedTemplateRule = hopeLangEngine.parse(attribute);
                    } catch (Exception e) {
                        log.error("Error parsing template for workflow: " + template.getId(), e);
                        return null;
                    }
                    return new WorkflowTemplateContext(template, parsedTemplateRule);
                }))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return parsedTemplates
                .stream()
                .filter(parsedWorkflowContext -> hopeLangEngine
                        .evaluate(parsedWorkflowContext.getParsedRule(), translatedPayload))
                .map(WorkflowTemplateContext::getTemplate)
                .findFirst();
    }

}
