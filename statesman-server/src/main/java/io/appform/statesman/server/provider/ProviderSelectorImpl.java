package io.appform.statesman.server.provider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.appform.statesman.engine.ProviderSelector;
import io.appform.statesman.model.Workflow;
import io.appform.statesman.server.dao.providers.ProviderCommands;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class ProviderSelectorImpl implements ProviderSelector {


    private final ProviderCommands providerCommands;

    @Inject
    public ProviderSelectorImpl(ProviderCommands providerCommands) {
        this.providerCommands = providerCommands;
    }

    @Override
    public String provider(String useCase, Set<String> configuredProviders, Workflow workflow) {
        List<String> activeProviders =
                configuredProviders.stream()
                        .map(providerId -> providerCommands.get(providerId, useCase))
                        .filter(storedProvider -> storedProvider.isPresent() && storedProvider.get().isActive())
                        .map(storedProvider -> storedProvider.get().getProviderId())
                        .collect(Collectors.toList());
        return getRandom(activeProviders);
    }

    private String getRandom(List<String> configuredProviders) {
        if (configuredProviders.isEmpty()) {
            return null;
        }
        if (configuredProviders.size() == 1) {
            return configuredProviders.get(0);
        }
        Random random = new Random();
        int randomNumber = random.nextInt(configuredProviders.size());
        return configuredProviders.get(randomNumber);
    }
}
