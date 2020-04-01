package io.appform.statesman.server.provider;

import io.appform.statesman.engine.ProviderSelector;
import io.appform.statesman.model.Workflow;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class ProviderSelectorImpl implements ProviderSelector {
    @Override
    public String provider(String providerType, Set<String> configuredProviders, Workflow workflow) {
        //TODO: change logic
        return getRandom(configuredProviders);
    }

    private String getRandom(Set<String> configuredProviders) {
        Random random = new Random();
        int randomNumber = random.nextInt(configuredProviders.size());
        List<String> all = configuredProviders.stream().collect(Collectors.toList());
        return all.get(randomNumber);
    }
}
