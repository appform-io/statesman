package io.appform.statesman.engine.observer.events;

import io.appform.statesman.engine.observer.ObservableEvent;
import io.appform.statesman.engine.observer.ObservableEventType;
import io.appform.statesman.engine.observer.ObservableEventVisitor;
import io.appform.statesman.model.Workflow;
import lombok.*;

/**
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WorkflowInitEvent extends ObservableEvent {
    Workflow workflow;

    @Builder
    public WorkflowInitEvent(Workflow workflow) {
        super(ObservableEventType.WORKFLOW_INIT);
        this.workflow = workflow;
    }

    @Override
    public <T> T accept(ObservableEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
