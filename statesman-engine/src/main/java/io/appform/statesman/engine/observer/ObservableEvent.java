package io.appform.statesman.engine.observer;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 *
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ObservableEvent {
    private final ObservableEventType eventType;

    public abstract <T> T accept(ObservableEventVisitor<T> visitor);
}
