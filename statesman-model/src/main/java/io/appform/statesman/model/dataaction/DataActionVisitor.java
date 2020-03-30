package io.appform.statesman.model.dataaction;

import io.appform.statesman.model.dataaction.impl.MergeDataAction;
import io.appform.statesman.model.dataaction.impl.MergeSelectedDataAction;

/**
 *
 */
public interface DataActionVisitor<T> {
    T visit(MergeDataAction mergeDataAction);

    T visit(MergeSelectedDataAction mergeSelectedDataAction);
}
