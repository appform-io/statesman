package io.appform.statesman.model;

/**
 *
 */
@FunctionalInterface
public interface Action {
    void apply(Workflow workflow);
}
