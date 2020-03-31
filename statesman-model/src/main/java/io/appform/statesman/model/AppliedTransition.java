package io.appform.statesman.model;

import lombok.Value;

/**
 *
 */
@Value
public class AppliedTransition {
    State oldState;
    State newState;
    int transitionId;
}
