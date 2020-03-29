package io.appform.statesman.engine.action;

import io.appform.statesman.model.Action;

import java.util.Optional;

/**
 *
 */
public interface ActionRegistry {
    void register(final Action action);
    Optional<Action> get(String id);
}
