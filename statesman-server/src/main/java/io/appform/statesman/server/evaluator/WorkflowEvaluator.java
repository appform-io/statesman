package io.appform.statesman.server.evaluator;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.appform.hope.core.Evaluatable;
import io.appform.hope.core.exceptions.errorstrategy.InjectValueErrorHandlingStrategy;
import io.appform.hope.lang.HopeLangEngine;
import io.appform.statesman.model.exception.ResponseCode;
import io.appform.statesman.model.exception.StatesmanError;
import io.appform.statesman.server.dao.workflow.WorkflowProviderCommand;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class WorkflowEvaluator {

    private WorkflowProviderCommand workflowProviderCommand;
    private HopeLangEngine hopeLangEngine;

    @Inject
    @Builder
    public WorkflowEvaluator(WorkflowProviderCommand workflowProviderCommand) {
        this.workflowProviderCommand = workflowProviderCommand;
        this.hopeLangEngine = HopeLangEngine.builder()
                .errorHandlingStrategy(new InjectValueErrorHandlingStrategy())
                .build();
    }

    public String determineWorkflowId(JsonNode translatedPayload) {

        List<WorkflowContext> parsedTemplates = workflowProviderCommand.getAll()
                .stream()
                .flatMap(template -> template.getAttributes().stream().map(attribute -> {
                    Evaluatable parsedTemplateRule;
                    try {
                        parsedTemplateRule = hopeLangEngine.parse(attribute);
                    } catch (Exception e) {
                        log.error("Error parsing template for workflow: " + Objects.toString(template.getId()), e);
                        return null;
                    }
                    return WorkflowContext.builder()
                            .parsedTemplateRule(parsedTemplateRule)
                            .workflowId(template.getId())
                            .build();
                }))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Set<String> workflowIds = parsedTemplates
                .stream()
                .filter(parsedWorkflowContext -> hopeLangEngine
                        .evaluate(parsedWorkflowContext.getParsedTemplateRule(), translatedPayload))
                .map(WorkflowContext::getWorkflowId)
                .collect(Collectors.toSet());

        if (workflowIds.isEmpty()) {
            throw new StatesmanError("Could not evaluate workflow ", ResponseCode.OPERATION_NOT_SUPPORTED);
        }
        if (workflowIds.size() > 1) {
            throw new StatesmanError("More than one workflow evaluated", ResponseCode.INTERNAL_SERVER_ERROR);
        }

        return workflowIds.stream().findFirst().get();

    }

}
