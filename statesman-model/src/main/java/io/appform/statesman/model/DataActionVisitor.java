package io.appform.statesman.model;

/**
 *
 */
public interface DataActionVisitor<T> {
    T visit(MergeDataAction mergeDataAction);
}
