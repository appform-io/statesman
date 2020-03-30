package io.appform.statesman.engine.action;

import io.appform.statesman.model.Action;

import javax.inject.Singleton;
import java.util.Optional;

/**
 *
 */
@Singleton
public class MapBasedActionRegistry implements ActionRegistry {
    @Override
    public void register(Action action) {

    }

    @Override
    public Optional<Action> get(String id) {
        return Optional.empty();
    }
}
