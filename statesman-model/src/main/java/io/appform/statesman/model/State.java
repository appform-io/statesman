package io.appform.statesman.model;

import lombok.*;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

/**
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class State {

    @NotNull
    @NotEmpty
    String name;
    boolean terminal;
}
