package io.appform.statesman.model;

import lombok.*;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StateTransition {

    @Setter
    String id;

    String fromState;

    boolean active;

    @Valid
    EvalRule rule;

    @Valid
    State toState;

    @NotEmpty
    String action;

    @Deprecated
    public StateTransition(EvalRule rule, State toState, String action) {
        this.rule = rule;
        this.toState = toState;
        this.action = action;
    }
}
