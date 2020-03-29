package io.appform.statesman.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MergeDataAction extends DataAction {
    @Override
    public <T> T accept(DataActionVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
