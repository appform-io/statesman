package io.appform.statesman.model;

import lombok.*;

import javax.validation.Valid;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StateTransition {

    public static enum Type {
        EVALUATED,
        DEFAULT
    }

    @Setter
    String id;

    Type type;

    String fromState;

    boolean active;

    String rule;

    @Valid
    State toState;

    String action;

}
