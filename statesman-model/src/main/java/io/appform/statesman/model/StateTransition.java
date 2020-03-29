package io.appform.statesman.model;

import lombok.Data;

/**
 *
 */
@Data
public class StateTransition {
    EvalRule rule;
    State toState;
    String action;
}
