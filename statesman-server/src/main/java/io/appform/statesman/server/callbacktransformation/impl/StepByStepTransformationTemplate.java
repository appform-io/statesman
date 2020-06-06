package io.appform.statesman.server.callbacktransformation.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.appform.statesman.server.callbacktransformation.TransformationTemplate;
import io.appform.statesman.server.callbacktransformation.TransformationTemplateType;
import io.appform.statesman.server.callbacktransformation.TransformationTemplateVisitor;
import io.appform.statesman.server.callbacktransformation.TranslationTemplateType;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.*;
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class StepSelection {
        String selectionRule;
        String template;
    }

    @NotNull
    @NotEmpty
    List<StepSelection> templates;

    @Builder
    public StepByStepTransformationTemplate(
            @JsonProperty("provider") String provider,
            @JsonProperty("idPath") String idPath,
            @JsonProperty("fqlPath") String fqlPath,
            @JsonProperty("translationTemplateType") TranslationTemplateType translationTemplateType,
            @JsonProperty("templates") List<StepSelection> templates,
            @JsonProperty("dropDetectionRule") String dropDetectionRule) {
        super(TransformationTemplateType.STEP_BY_STEP, idPath, fqlPath, translationTemplateType, provider, dropDetectionRule);
        this.templates = templates;
    }

    @Override
    public <T> T accept(TransformationTemplateVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
