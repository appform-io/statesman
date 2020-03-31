package io.appform.statesman.server.dao.callback;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.statesman.model.WorkflowTemplate;
import io.appform.statesman.model.exception.ResponseCode;
import io.appform.statesman.model.exception.StatesmanError;
import io.appform.statesman.server.callbacktransformation.TransformationTemplate;
import io.appform.statesman.server.callbacktransformation.TransformationTemplateVisitor;
import io.appform.statesman.server.callbacktransformation.impl.OneShotTransformationTemplate;
import io.appform.statesman.server.callbacktransformation.impl.StepByStepTransformationTemplate;
import io.appform.statesman.server.utils.CallbackTemplateUtils;
import io.appform.statesman.server.utils.MapperUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.criterion.DetachedCriteria;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class CallbackTemplateProviderCommand implements CallbackTemplateProvider {

    private final LookupDao<StoredCallbackTransformationTemplate> callbackTemplateDao;
    private final LoadingCache<String, List<TransformationTemplate>> allCallbackTemplates;


    @Inject
    public CallbackTemplateProviderCommand(LookupDao<StoredCallbackTransformationTemplate> callbackTemplateDao) {
        this.callbackTemplateDao = callbackTemplateDao;
        log.info("Initializing cache CALLBACK_TEMPLATE_CACHE");
        allCallbackTemplates = Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(300, TimeUnit.SECONDS)
                .refreshAfterWrite(60, TimeUnit.SECONDS)
                .build(key -> {
                    log.debug("Loading data for workflow for key: {}", key);
                    return getAllFromDb();
                });
    }
    @Override
    public Optional<TransformationTemplate> createTemplate(TransformationTemplate transformationTemplate) {
        try {
            StoredCallbackTransformationTemplate storedCallbackTemplate = CallbackTemplateUtils.toDao(transformationTemplate);
            return callbackTemplateDao.save(storedCallbackTemplate).map(CallbackTemplateUtils::toDto);
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }

    @Override
    public Optional<TransformationTemplate> updateTemplate(TransformationTemplate transformationTemplate) {
        try {
            boolean updated = callbackTemplateDao.update(transformationTemplate.getProvider(), storedCallbackTransformationTemplate -> {
                if(storedCallbackTransformationTemplate.isPresent()) {
                    val storedTemplate = storedCallbackTransformationTemplate.get();
                    transformationTemplate.accept(new TransformationTemplateVisitor<Object>() {
                        @Override
                        public Object visit(OneShotTransformationTemplate oneShotTransformationTemplate) {
                            storedTemplate.setIdPath(oneShotTransformationTemplate.getIdPath());
                            storedTemplate.setTemplate(MapperUtils.serialize(oneShotTransformationTemplate.getTemplate()));
                            return null;
                        }

                        @Override
                        public Object visit(StepByStepTransformationTemplate stepByStepTransformationTemplate) {
                            storedTemplate.setIdPath(stepByStepTransformationTemplate.getIdPath());
                            storedTemplate.setTemplate(MapperUtils.serialize(stepByStepTransformationTemplate.getTemplates()));
                            return null;
                        }
                    });
                }
                return storedCallbackTransformationTemplate.orElse(null);
            });
            return updated ? getTemplate(transformationTemplate.getProvider()) : Optional.empty();
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }

    @Override
    public List<TransformationTemplate> getAll() {
        return allCallbackTemplates.get("all");
    }

    @Override
    public Optional<TransformationTemplate> getTemplate(String provider) {
        try {
            return callbackTemplateDao.get(provider).map(CallbackTemplateUtils::toDto);
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }

    public List<TransformationTemplate> getAllFromDb() {
        try {
            return callbackTemplateDao.scatterGather(DetachedCriteria.forClass(StoredCallbackTransformationTemplate.class))
                    .stream()
                    .map(CallbackTemplateUtils::toDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }

}
