package io.appform.statesman.model.dataaction.impl;

import io.appform.statesman.model.dataaction.DataAction;
import io.appform.statesman.model.dataaction.DataActionType;
import io.appform.statesman.model.dataaction.DataActionVisitor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

import java.util.List;

/**
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MergeSelectedDataAction extends DataAction {
    List<String> fields;

    public MergeSelectedDataAction(List<String> fields) {
        super(DataActionType.MERGE_SELECTED);
        this.fields = fields;
    }

    @Override
    public <T> T accept(DataActionVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
