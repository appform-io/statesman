package io.appform.statesman.model.testing;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

/**
 *
 */
@Value
public class IngressTemplateTestingRequest {
    String queryParams;
    JsonNode body;
    @NotNull
    @NotEmpty
    String template;
}
