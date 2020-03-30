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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import java.util.List;
import java.util.Objects;
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
                .map(template -> {
                    Evaluatable parsedTemplateRule;
                    try {
                        parsedTemplateRule = hopeLangEngine.parse(template.getAttributes().get(0));
                    } catch (Exception e) {
                        log.error("Error parsing template for workflow: " + Objects.toString(template.getId()), e);
                        return null;
                    }
                    return WorkflowContext.builder()
                            .parsedTemplateRule(parsedTemplateRule)
                            .workflowId(template.getId())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<String> workflowIds = parsedTemplates
                .stream()
                .filter(parsedWorkflowContext -> hopeLangEngine
                        .evaluate(parsedWorkflowContext.getParsedTemplateRule(), translatedPayload))
                .map(WorkflowContext::getWorkflowId)
                .collect(Collectors.toList());

        if (workflowIds.isEmpty()) {
            throw new StatesmanError("Could not evaluate workflow ", ResponseCode.OPERATION_NOT_SUPPORTED);
        }
        if (workflowIds.size() > 1) {
            throw new StatesmanError("More than one workflow evaluated", ResponseCode.INTERNAL_SERVER_ERROR);
        }

        return workflowIds.get(0);

    }


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    private class WorkflowContext {
        private String workflowId;
        private Evaluatable parsedTemplateRule;
    }

}
