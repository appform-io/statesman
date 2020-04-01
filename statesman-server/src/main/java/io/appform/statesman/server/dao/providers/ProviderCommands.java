package io.appform.statesman.server.dao.providers;


import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.appform.dropwizard.sharding.dao.RelationalDao;
import io.appform.functionmetrics.MonitoredFunction;
import io.appform.statesman.model.exception.ResponseCode;
import io.appform.statesman.model.exception.StatesmanError;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Singleton
public class ProviderCommands {

    private final RelationalDao<StoredProvider> providerDao;
    private final LoadingCache<ProviderCacheKey, Optional<StoredProvider>> cache;


    @Inject
    public ProviderCommands(RelationalDao<StoredProvider> providerDao) {
        this.providerDao = providerDao;
        cache = Caffeine.newBuilder()
                .maximumSize(1_000)
                .expireAfterWrite(300, TimeUnit.SECONDS)
                .refreshAfterWrite(60, TimeUnit.SECONDS)
                .build(key -> {
                    log.debug("Loading data for transition for key: {}", key);
                    return getFromDb(key.getProviderId(), key.getProviderType());
                });
    }

    @MonitoredFunction
    public List<StoredProvider> get(String providerId) {
        try {
            DetachedCriteria detachedCriteria = DetachedCriteria.forClass(StoredProvider.class)
                    .add(Restrictions.eq("providerId", providerId));
            return providerDao.select(providerId, detachedCriteria, 0, Integer.MAX_VALUE);
        } catch (Exception e) {
            throw new StatesmanError(ResponseCode.STORAGE_ERROR);
        }
    }

    @MonitoredFunction
    public Optional<StoredProvider> get(String providerId, String providerType) {
        try {
            return cache.get(ProviderCacheKey.builder()
                    .providerId(providerId)
                    .providerType(providerType)
                    .build());
        } catch (Exception e) {
            throw new StatesmanError(ResponseCode.STORAGE_ERROR);
        }
    }

    @MonitoredFunction
    public Optional<StoredProvider> getFromDb(String providerId, String providerType) {
        try {
            DetachedCriteria detachedCriteria = DetachedCriteria.forClass(StoredProvider.class)
                    .add(Restrictions.eq("providerId", providerId))
                    .add(Restrictions.eq("providerType", providerType));
            return providerDao.select(providerId, detachedCriteria, 0, 1)
                    .stream()
                    .findFirst();
        } catch (Exception e) {
            throw new StatesmanError(ResponseCode.STORAGE_ERROR);
        }
    }

    @MonitoredFunction
    public Optional<StoredProvider> save(StoredProvider storedProvider) {
        try {
            return providerDao.save(storedProvider.getProviderId(), storedProvider);
        } catch (ConstraintViolationException e) {
            return getFromDb(storedProvider.getProviderId(), storedProvider.getProviderType());
        } catch (Exception e) {
            throw new StatesmanError(ResponseCode.STORAGE_ERROR);
        }
    }

    @MonitoredFunction
    public boolean update(String providerId, DetachedCriteria detachedCriteria, Function<StoredProvider, StoredProvider> modifier) {
        try {
            return providerDao.update(providerId, detachedCriteria, modifier);
        } catch (Exception e) {
            throw new StatesmanError(ResponseCode.STORAGE_ERROR);
        }
    }


    @Data
    @Builder
    private static class ProviderCacheKey {
        String providerId;
        String providerType;

    }

}
