package io.appform.statesman.server.dao.callback;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import io.appform.statesman.model.exception.ResponseCode;
import io.appform.statesman.model.exception.StatesmanError;
import io.appform.statesman.server.callbacktransformation.TransformationTemplate;
import io.appform.statesman.server.callbacktransformation.TransformationTemplateVisitor;
import io.appform.statesman.server.callbacktransformation.TranslationTemplateType;
import io.appform.statesman.server.callbacktransformation.impl.OneShotTransformationTemplate;
import io.appform.statesman.server.callbacktransformation.impl.StepByStepTransformationTemplate;
import io.appform.statesman.server.utils.CallbackTemplateUtils;
import io.appform.statesman.server.utils.MapperUtils;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class CallbackTemplateProviderCommand implements CallbackTemplateProvider {

    private final RelationalDao<StoredCallbackTransformationTemplate> callbackTemplateDao;
    private final LoadingCache<String, Set<TransformationTemplate>> allCallbackTemplates;


    @Inject
    public CallbackTemplateProviderCommand(RelationalDao<StoredCallbackTransformationTemplate> callbackTemplateDao) {
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
            return callbackTemplateDao.save(transformationTemplate.getProvider(), storedCallbackTemplate)
                    .map(CallbackTemplateUtils::toDto);
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }

    @Override
    public Optional<TransformationTemplate> updateTemplate(TransformationTemplate transformationTemplate) {
        try {
            DetachedCriteria detachedCriteria = DetachedCriteria.forClass(StoredCallbackTransformationTemplate.class)
                    .add(Restrictions.eq("translationTemplateType", transformationTemplate.getTranslationTemplateType()))
                    .add(Restrictions.eq("provider", transformationTemplate.getProvider()));
            boolean updated = callbackTemplateDao.update(transformationTemplate.getProvider(), detachedCriteria,
                    storedCallbackTransformationTemplate -> {
                        transformationTemplate.accept(new TransformationTemplateVisitor<Object>() {
                            @Override
                            public Object visit(OneShotTransformationTemplate oneShotTransformationTemplate) {
                                storedCallbackTransformationTemplate.setIdPath(oneShotTransformationTemplate.getIdPath());
                                storedCallbackTransformationTemplate.setFqlPath(oneShotTransformationTemplate.getFqlPath());
                                storedCallbackTransformationTemplate.setTemplate(MapperUtils.serialize(oneShotTransformationTemplate.getTemplate()));
                                return null;
                            }

                            @Override
                            public Object visit(StepByStepTransformationTemplate stepByStepTransformationTemplate) {
                                storedCallbackTransformationTemplate.setIdPath(stepByStepTransformationTemplate.getIdPath());
                                storedCallbackTransformationTemplate.setFqlPath(stepByStepTransformationTemplate.getFqlPath());
                                storedCallbackTransformationTemplate.setTemplate(MapperUtils.serialize(stepByStepTransformationTemplate.getTemplates()));
                                return null;
                            }
                        });
                        storedCallbackTransformationTemplate.setDropDetectionRule(transformationTemplate.getDropDetectionRule());
                        return storedCallbackTransformationTemplate;
                    });
            return updated ? getTemplateFromDb(transformationTemplate.getProvider(), transformationTemplate.getTranslationTemplateType()) : Optional.empty();
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }

    @Override
    public Set<TransformationTemplate> getAll() {
        return allCallbackTemplates.get("all");
    }

    @Override
    public Optional<TransformationTemplate> getTemplate(String provider, TranslationTemplateType translationTemplateType) {
        try {
            return getAll().stream()
                    .filter(transformationTemplate -> transformationTemplate.getProvider().equals(provider)
                            && transformationTemplate.getTranslationTemplateType() == translationTemplateType)
                    .findFirst();
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }

    public Optional<TransformationTemplate> getTemplateFromDb(String provider, TranslationTemplateType translationTemplateType) {
        try {
            DetachedCriteria detachedCriteria = DetachedCriteria.forClass(StoredCallbackTransformationTemplate.class)
                    .add(Restrictions.eq("translationTemplateType", translationTemplateType))
                    .add(Restrictions.eq("provider", provider));
            return callbackTemplateDao.select(provider, detachedCriteria, 0, 1)
                    .stream()
                    .findFirst()
                    .map(CallbackTemplateUtils::toDto);

        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }

    public Set<TransformationTemplate> getAllFromDb() {
        try {
            return callbackTemplateDao.scatterGather(DetachedCriteria.forClass(StoredCallbackTransformationTemplate.class), 0, Integer.MAX_VALUE)
                    .stream()
                    .map(CallbackTemplateUtils::toDto)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            throw StatesmanError.propagate(e, ResponseCode.DAO_ERROR);
        }
    }

}
