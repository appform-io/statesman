package io.appform.statesman.engine;

import io.appform.statesman.model.Workflow;

import java.util.Set;

public interface ProviderSelector {

    String provider(String useCase, Set<String> configuredProviders, Workflow workflow);
}
