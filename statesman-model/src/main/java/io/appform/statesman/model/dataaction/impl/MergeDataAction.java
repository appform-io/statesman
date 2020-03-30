package io.appform.statesman.model.dataaction.impl;

import io.appform.statesman.model.dataaction.DataAction;
import io.appform.statesman.model.dataaction.DataActionType;
import io.appform.statesman.model.dataaction.DataActionVisitor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

/**
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MergeDataAction extends DataAction {
    public MergeDataAction() {
        super(DataActionType.MERGE);
    }

    @Override
    public <T> T accept(DataActionVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
