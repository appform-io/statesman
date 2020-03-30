package io.appform.statesman.model;

import lombok.Value;

/**
 *
 */
@Value
public class StateTransition {
    EvalRule rule;
    State toState;
    String action;
}
