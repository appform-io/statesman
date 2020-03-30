package io.appform.statesman.server.dao.workflow;


import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.statesman.engine.WorkflowProvider;
import io.appform.statesman.server.utils.MapperUtils;
import io.appform.statesman.server.utils.WorkflowUtils;
import io.appform.statesman.model.Workflow;
import io.appform.statesman.model.WorkflowTemplate;
import io.appform.statesman.model.exception.ResponseCode;
import io.appform.statesman.model.exception.StatesmanError;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class WorkflowProviderCommand implements WorkflowProvider {

    private final LookupDao<StoredWorkflowTemplate> workflowTemplateLookupDao;
    private final LookupDao<StoredWorkflowInstance> workflowInstanceLookupDao;
    private final LoadingCache<String, Optional<WorkflowTemplate>> workflowTemplateCache;

    @Inject
    public WorkflowProviderCommand(LookupDao<StoredWorkflowTemplate> workflowTemplateLookupDao,
                                   LookupDao<StoredWorkflowInstance> workflowInstanceLookupDao) {
        this.workflowTemplateLookupDao = workflowTemplateLookupDao;
        this.workflowInstanceLookupDao = workflowInstanceLookupDao;
        log.info("Initializing cache WORKFLOW_TEMPLATE_CACHE");
        workflowTemplateCache = Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(300, TimeUnit.SECONDS)
                .refreshAfterWrite(60, TimeUnit.SECONDS)
                .build(key -> {
                    log.debug("Loading data for workflow for key: {}", key);
                    return getTemplateFromDb(key);
                });
    }

    @Override
    public Optional<WorkflowTemplate> createTemplate(WorkflowTemplate workflowTemplate) {
        try {
            StoredWorkflowTemplate storedWorkflowTemplate = WorkflowUtils.toDao(workflowTemplate);
            return workflowTemplateLookupDao.save(storedWorkflowTemplate).map(WorkflowUtils::toDto);
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }

    @Override
    public Optional<WorkflowTemplate> updateTemplate(WorkflowTemplate workflowTemplate) {
        try {
            boolean updated = workflowTemplateLookupDao.update(workflowTemplate.getId(), storedWorkflowTemplate -> {
             if(storedWorkflowTemplate.isPresent()) {
                 storedWorkflowTemplate.get().setActive(workflowTemplate.isActive());
                 storedWorkflowTemplate.get().setName(workflowTemplate.getName());
                 storedWorkflowTemplate.get().setAttributes(MapperUtils.serialize(workflowTemplate.getAttributes()));
             }
             return storedWorkflowTemplate.orElse(null);
            });
            return updated ? getTemplateFromDb(workflowTemplate.getId()) : Optional.empty();
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }

    @Override
    public Optional<WorkflowTemplate> getTemplate(String workflowTemplateId) {
        try {
            return workflowTemplateCache.get(workflowTemplateId);
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }


    public Optional<WorkflowTemplate> getTemplateFromDb(String workflowTemplateId) {
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
