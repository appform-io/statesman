package io.appform.statesman.engine.observer.events;

import io.appform.statesman.engine.observer.ObservableEvent;
import io.appform.statesman.engine.observer.ObservableEventType;
import io.appform.statesman.engine.observer.ObservableEventVisitor;
import io.appform.statesman.model.*;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

/**
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StateTransitionEvent extends ObservableEvent {
    WorkflowTemplate template;
    Workflow workflow;
    DataUpdate update;
    State oldState;
    String appliedAction;

    public StateTransitionEvent(
            WorkflowTemplate template,
            Workflow workflow,
            DataUpdate update,
            State oldState,
            String appliedAction) {
        super(ObservableEventType.STATE_TRANSITION);
        this.template = template;
        this.workflow = workflow;
        this.update = update;
        this.oldState = oldState;
        this.appliedAction = appliedAction;
    }

    @Override
    public <T> T accept(ObservableEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
