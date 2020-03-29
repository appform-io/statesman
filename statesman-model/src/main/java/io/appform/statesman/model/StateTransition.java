package io.appform.statesman.model;

import lombok.Data;

/**
 *
 */
@Data
public class StateTransition {
    String rule;
    State toState;
    String action;
}
