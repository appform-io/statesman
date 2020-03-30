package io.appform.statesman.engine;

import com.fasterxml.jackson.databind.JsonNode;
import io.appform.statesman.model.Workflow;
import io.appform.statesman.model.WorkflowTemplate;

import java.util.List;
import java.util.Optional;

/**
 *
 */
public interface WorkflowProvider {
    Optional<WorkflowTemplate> createTemplate(String name, List<String> attributes);
    Optional<WorkflowTemplate> getTemplate(String workflowTemplateId);

    Optional<Workflow> createWorkflow(String templateId, JsonNode initialData);
    Optional<Workflow> getWorkflow(String workflowId);
    void saveWorkflow(final Workflow workflow);

}
