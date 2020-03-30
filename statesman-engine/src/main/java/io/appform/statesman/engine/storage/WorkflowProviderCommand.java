package io.appform.statesman.engine.storage;


import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.statesman.engine.WorkflowProvider;
import io.appform.statesman.engine.storage.data.StoredWorkflowInstance;
import io.appform.statesman.engine.storage.data.StoredWorkflowTemplate;
import io.appform.statesman.engine.util.WorkflowUtils;
import io.appform.statesman.model.Workflow;
import io.appform.statesman.model.WorkflowTemplate;
import io.appform.statesman.model.exception.ResponseCode;
import io.appform.statesman.model.exception.StatesmanError;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Slf4j
@Singleton
public class WorkflowProviderCommand implements WorkflowProvider {

    private final LookupDao<StoredWorkflowTemplate> workflowTemplateLookupDao;
    private final LookupDao<StoredWorkflowInstance> workflowInstanceLookupDao;

    @Inject
    public WorkflowProviderCommand(LookupDao<StoredWorkflowTemplate> workflowTemplateLookupDao,
                                   LookupDao<StoredWorkflowInstance> workflowInstanceLookupDao) {
        this.workflowTemplateLookupDao = workflowTemplateLookupDao;
        this.workflowInstanceLookupDao = workflowInstanceLookupDao;
    }

    @Override
    public Optional<WorkflowTemplate> createTemplate(String name, List<String> attributes) {
        try {
            StoredWorkflowTemplate storedWorkflowTemplate = WorkflowUtils.toDao(name, attributes);
            return workflowTemplateLookupDao.save(storedWorkflowTemplate).map(WorkflowUtils::toDto);
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }

    @Override
    public Optional<WorkflowTemplate> getTemplate(String workflowTemplateId) {
        try {
            return workflowTemplateLookupDao.get(workflowTemplateId).map(WorkflowUtils::toDto);
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }

    @Override
    public Optional<Workflow> createWorkflow(String templateId, JsonNode initialData) {
        try {
            StoredWorkflowInstance storedWorkflowInstance = WorkflowUtils.toInstanceDao(templateId, initialData);
            return workflowInstanceLookupDao.save(storedWorkflowInstance).map(WorkflowUtils::toInstanceDto);
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }

    @Override
    public Optional<Workflow> getWorkflow(String workflowId) {
        try {
            return workflowInstanceLookupDao.get(workflowId).map(WorkflowUtils::toInstanceDto);
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }

    @Override
    public void saveWorkflow(Workflow workflow) {
        try {
            StoredWorkflowInstance storedWorkflowInstance = WorkflowUtils.toInstanceDao(workflow);
            workflowInstanceLookupDao.save(storedWorkflowInstance).map(WorkflowUtils::toInstanceDto);
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }
}
