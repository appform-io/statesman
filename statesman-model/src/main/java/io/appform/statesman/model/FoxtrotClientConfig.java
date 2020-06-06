package io.appform.statesman.model;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

/**
 *
 */
@Data
public class FoxtrotClientConfig {

    @NotEmpty
    private final String endpoint;

    @NotEmpty
    private final String accessToken;
}
