package io.appform.statesman.server.dao.providers;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.appform.dropwizard.sharding.dao.LookupDao;
import io.appform.statesman.server.exception.ResponseCode;
import io.appform.statesman.server.exception.StatesmanError;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;

import java.util.Optional;

@Slf4j
@Singleton
public class ProviderCommands {

    private final LookupDao<StoredProvider> providerDao;

    @Inject
    public ProviderCommands(LookupDao<StoredProvider> providerDao) {
        this.providerDao = providerDao;
    }

    public Optional<StoredProvider> get(String providerId){
        try {
            return providerDao.get(providerId);
        }
        catch (Exception e){
            throw new StatesmanError(ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    public Optional<StoredProvider> save(StoredProvider storedProvider) {
        try {
            return providerDao.save(storedProvider);
        } catch (ConstraintViolationException e) {
            return get(storedProvider.getProviderId());
        } catch (Exception e) {
            throw new StatesmanError(ResponseCode.PROVIDER_CREATE_ERROR);
        }
    }




}
