package io.appform.statesman.engine.observer;

/**
 *
 */
public class FlowTrackingObservableEvent extends ObservableEvent {
    protected FlowTrackingObservableEvent(ObservableEventType eventType) {
        super(eventType);
    }

    @Override
    public <T> T accept(ObservableEventVisitor<T> visitor) {
        return null;
    }
}
