package io.appform.statesman.server.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import io.appform.statesman.model.*;
import io.appform.statesman.model.action.template.*;
import io.appform.statesman.server.dao.action.StoredActionTemplate;
import io.appform.statesman.server.dao.transition.StoredStateTransition;
import io.appform.statesman.server.dao.workflow.StoredWorkflowInstance;
import io.appform.statesman.server.dao.workflow.StoredWorkflowTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@Slf4j
public class WorkflowUtils {

    private WorkflowUtils() {
    }

    public static StoredWorkflowTemplate toDao(WorkflowTemplate workflowTemplate) {
        String templateId = Strings.isNullOrEmpty(workflowTemplate.getId()) ? UUID.randomUUID().toString() : workflowTemplate.getId();
        workflowTemplate.setId(templateId);
        return StoredWorkflowTemplate.builder()
                .active(workflowTemplate.isActive())
                .name(workflowTemplate.getName())
                .templateId(templateId)
                .rules(MapperUtils.serialize(workflowTemplate.getRules()))
                .startState(MapperUtils.serialize(workflowTemplate.getStartState()))
                .build();
    }

    public static WorkflowTemplate toDto(StoredWorkflowTemplate workflowTemplate) {
        return WorkflowTemplate.builder()
                .active(workflowTemplate.isActive())
                .id(workflowTemplate.getTemplateId())
                .name(workflowTemplate.getName())
                .rules(MapperUtils.deserialize(workflowTemplate.getRules(), new TypeReference<List<String>>() {
                }))
                .startState(MapperUtils.deserialize(workflowTemplate.getStartState(), State.class))
                .build();
    }

    public static StoredWorkflowInstance toInstanceDao(String templateId, JsonNode initialData) {
        return StoredWorkflowInstance.builder()
                .templateId(templateId)
                .completed(false)
                .workflowId(UUID.randomUUID().toString())
                .data(MapperUtils.serialize(DataObject.builder()
                        .data(initialData)
                        .build()))
                .build();
    }

    public static StoredWorkflowInstance toInstanceDao(Workflow workflow) {
        return StoredWorkflowInstance.builder()
                .templateId(workflow.getTemplateId())
                .workflowId(workflow.getId())
                .data(MapperUtils.serialize(workflow.getDataObject()))
                .currentState(workflow.getDataObject().getCurrentState().getName())
                .completed(workflow.getDataObject().getCurrentState().isTerminal())
                .build();
    }

    public static Workflow toInstanceDto(StoredWorkflowInstance storedWorkflowInstance) {
        return Workflow.builder()
                .id(storedWorkflowInstance.getWorkflowId())
                .templateId(storedWorkflowInstance.getTemplateId())
                .dataObject(MapperUtils.deserialize(storedWorkflowInstance.getData(), DataObject.class))
                .created(storedWorkflowInstance.getCreated())
                .updated(storedWorkflowInstance.getUpdated())
                .build();
    }

    public static StateTransition toDto(StoredStateTransition storedStateTransitions) {
        return MapperUtils.deserialize(storedStateTransitions.getData(), StateTransition.class);
    }

    public static StoredStateTransition toDao(String workflowTemplateId, StateTransition stateTransition) {
        String transitionId = Strings.isNullOrEmpty(stateTransition.getId())
                ? UUID.randomUUID().toString() : stateTransition.getId();
        stateTransition.setId(transitionId);
        return StoredStateTransition.builder()
                .transitionId(transitionId)
                .active(true)
                .fromState(stateTransition.getFromState())
                .workflowTemplateId(workflowTemplateId)
                .data(MapperUtils.serialize(stateTransition))
                .build();
    }

    public static ActionTemplate toDto(StoredActionTemplate storedActionTemplate) {
        return MapperUtils.deserialize(storedActionTemplate.getData(), ActionTemplate.class);
    }

    public static StoredActionTemplate toDao(ActionTemplate actionTemplate) {
        String templateId = Strings.isNullOrEmpty(actionTemplate.getTemplateId())
                ? UUID.randomUUID().toString() : actionTemplate.getTemplateId();
        return StoredActionTemplate.builder()
                .templateId(templateId)
                .active(actionTemplate.isActive())
                .actionType(actionTemplate.getType().name())
                .name(actionTemplate.getName())
                .data(MapperUtils.serialize(actionTemplate))
                .build();
    }
}
