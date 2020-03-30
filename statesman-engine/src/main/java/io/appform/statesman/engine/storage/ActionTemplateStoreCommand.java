package io.appform.statesman.engine.storage;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.statesman.engine.ActionTemplateStore;
import io.appform.statesman.engine.storage.data.StoredActionTemplate;
import io.appform.statesman.engine.util.WorkflowUtils;
import io.appform.statesman.model.action.template.ActionTemplate;
import io.appform.statesman.model.exception.ResponseCode;
import io.appform.statesman.model.exception.StatesmanError;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class ActionTemplateStoreCommand implements ActionTemplateStore {

    private final LookupDao<StoredActionTemplate> actionTemplateLookupDao;
    private final LoadingCache<String, Optional<ActionTemplate>> ACTION_TEMPLATE_CACHE;


    public ActionTemplateStoreCommand(LookupDao<StoredActionTemplate> actionTemplateLookupDao) {
        this.actionTemplateLookupDao = actionTemplateLookupDao;
        log.info("Initializing cache ACTION_TEMPLATE_CACHE");
        ACTION_TEMPLATE_CACHE = Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(300, TimeUnit.SECONDS)
                .refreshAfterWrite(60, TimeUnit.SECONDS)
                .build(key -> {
                    log.debug("Loading data for action for key: {}", key);
                    return getFromDb(key);
                });

    }

    public Optional<ActionTemplate> getFromDb(String actionTemplateId) {
        try {
            return actionTemplateLookupDao.get(actionTemplateId)
                    .map(WorkflowUtils::toDto);
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }


    @Override
    public Optional<ActionTemplate> save(ActionTemplate actionTemplate) {
        try {
            return actionTemplateLookupDao.save(WorkflowUtils.toDao(actionTemplate))
                    .map(WorkflowUtils::toDto);
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }

    @Override
    public Optional<ActionTemplate> get(String actionTemplateId) {
        try {
            return ACTION_TEMPLATE_CACHE.get(actionTemplateId);
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }


}
