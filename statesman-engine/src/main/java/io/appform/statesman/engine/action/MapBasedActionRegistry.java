package io.appform.statesman.engine.action;

import io.appform.statesman.engine.action.impl.HttpAction;
import io.appform.statesman.model.Action;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
@Singleton
public class MapBasedActionRegistry implements ActionRegistry {

    private Map<String, Action> registry;

    @Inject
    public MapBasedActionRegistry(HttpAction httpAction) {
        registry = new ConcurrentHashMap<>();
        register(httpAction);
    }

    @Override
    public void register(Action action) {
        registry.put(action.getType().name(), action);
    }

    @Override
    public Optional<Action> get(String id) {
        return Optional.ofNullable(registry.get(id));
    }

}
