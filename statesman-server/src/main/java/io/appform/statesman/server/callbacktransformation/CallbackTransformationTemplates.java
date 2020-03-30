package io.appform.statesman.server.callbacktransformation;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 *
 */
@Data
public class CallbackTransformationTemplates {
    @NotNull
    @NotEmpty
    private Map<String, String> templates;
}
