package io.appform.statesman.server.callbacktransformation;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

/**
 *
 */
@Data
public class TransformationTemplate {
    private String idPath;
    @NotNull
    @NotEmpty
    private String template;
}
