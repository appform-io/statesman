package io.appform.statesman.engine.observer.events;

import io.appform.statesman.engine.observer.ObservableEvent;
import io.appform.statesman.engine.observer.ObservableEventType;
import io.appform.statesman.engine.observer.ObservableEventVisitor;
import io.appform.statesman.model.Workflow;
import lombok.Builder;
import lombok.Data;

/**
 *
 */
@Data
public class WorkflowInitEvent extends ObservableEvent {
    private Workflow workflow;

    public WorkflowInitEvent() {
        super(ObservableEventType.WORKFLOW_INIT);
    }

    @Builder
    public WorkflowInitEvent(Workflow workflow) {
        this();
        this.workflow = workflow;
    }

    @Override
    public <T> T accept(ObservableEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
