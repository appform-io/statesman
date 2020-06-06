package io.appform.statesman.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

/**
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoxtrotClientConfig {

    @NotEmpty
    private String endpoint;

    @NotEmpty
    private String accessToken;
}
