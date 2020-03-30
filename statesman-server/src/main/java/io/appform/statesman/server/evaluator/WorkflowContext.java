package io.appform.statesman.server.evaluator;


import io.appform.hope.core.Evaluatable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class WorkflowContext {

    private String workflowId;
    private Evaluatable parsedTemplateRule;
}
