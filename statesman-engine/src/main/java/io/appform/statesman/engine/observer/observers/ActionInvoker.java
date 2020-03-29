package io.appform.statesman.engine.observer.observers;

import com.google.common.base.Strings;
import com.google.inject.name.Named;
import io.appform.statesman.engine.action.ActionExecutor;
import io.appform.statesman.engine.observer.ObservableEvent;
import io.appform.statesman.engine.observer.ObservableEventBusSubscriber;
import io.appform.statesman.engine.observer.ObservableEventVisitor;
import io.appform.statesman.engine.observer.events.StateTransitionEvent;
import lombok.val;

import javax.inject.Provider;

/**
 *
 */
public class ActionInvoker extends ObservableEventBusSubscriber {
    private final Provider<ActionExecutor> actionExecutor;

    public ActionInvoker(
            @Named("foxtrotEventSender") ObservableEventBusSubscriber next,
            Provider<ActionExecutor> actionExecutor) {
        super(next);
        this.actionExecutor = actionExecutor;
    }

    @Override
    public void handleEvent(ObservableEvent event) {
        event.accept(new ObservableEventVisitor<Void>() {
            @Override
            public Void visit(StateTransitionEvent stateTransitionEvent) {
                val selectedTransition = stateTransitionEvent.getTransition();
                if(!Strings.isNullOrEmpty(selectedTransition.getAction())) {
                    actionExecutor.get()
                            .execute(selectedTransition.getAction(),
                                     stateTransitionEvent.getWorkflow());
                }
                return null;
            }
        });
    }
}
