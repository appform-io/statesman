package io.appform.statesman.model;

import lombok.*;
import org.hibernate.validator.constraints.NotEmpty;

/**
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvalRule {

    @NotEmpty
    String id;

    @NotEmpty
    String rule;
}
