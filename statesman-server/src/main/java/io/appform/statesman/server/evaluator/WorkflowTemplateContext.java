package io.appform.statesman.server.evaluator;


import io.appform.hope.core.Evaluatable;
import io.appform.statesman.model.WorkflowTemplate;
import lombok.Value;

@Value
public class WorkflowTemplateContext {
    WorkflowTemplate template;
    Evaluatable parsedRule;
}
