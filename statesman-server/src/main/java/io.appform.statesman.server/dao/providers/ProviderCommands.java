package io.appform.statesman.server.dao.providers;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.functionmetrics.MonitoredFunction;
import io.appform.statesman.model.exception.ResponseCode;
import io.appform.statesman.model.exception.StatesmanError;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;

import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Singleton
public class ProviderCommands {

    private final LookupDao<StoredProvider> providerDao;

    @Inject
    public ProviderCommands(LookupDao<StoredProvider> providerDao) {
        this.providerDao = providerDao;
    }

    @MonitoredFunction
    public Optional<StoredProvider> get(String providerId) {
        try {
            return providerDao.get(providerId);
        } catch (Exception e) {
            throw new StatesmanError(ResponseCode.STORAGE_ERROR);
        }
    }

    @MonitoredFunction
    public Optional<StoredProvider> save(StoredProvider storedProvider) {
        try {
            return providerDao.save(storedProvider);
        } catch (ConstraintViolationException e) {
            return get(storedProvider.getProviderId());
        } catch (Exception e) {
            throw new StatesmanError(ResponseCode.STORAGE_ERROR);
        }
    }

    @MonitoredFunction
    public boolean update(String providerId, Function<Optional<StoredProvider>, StoredProvider> modifier) {
        try {
            return providerDao.update(providerId, modifier);
        } catch (Exception e) {
            throw new StatesmanError(ResponseCode.STORAGE_ERROR);
        }
    }

}
