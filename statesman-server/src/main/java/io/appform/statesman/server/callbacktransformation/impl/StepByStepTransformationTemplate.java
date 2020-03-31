package io.appform.statesman.server.callbacktransformation.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.appform.statesman.server.callbacktransformation.TransformationTemplate;
import io.appform.statesman.server.callbacktransformation.TransformationTemplateType;
import io.appform.statesman.server.callbacktransformation.TransformationTemplateVisitor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StepByStepTransformationTemplate extends TransformationTemplate  {
    @Value
    public static final class StepSelection {
        String selectionRule;
        String template;
    }

    @NotNull
    @NotEmpty
    List<StepSelection> templates;

    public StepByStepTransformationTemplate(
            @JsonProperty("idPath") String idPath,
            @JsonProperty("templates") List<StepSelection> templates) {
        super(TransformationTemplateType.STEP_BY_STEP, idPath);
        this.templates = templates;
    }

    @Override
    public <T> T accept(TransformationTemplateVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
