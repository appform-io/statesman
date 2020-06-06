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
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

/**
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OneShotTransformationTemplate extends TransformationTemplate  {
    @NotNull
    @NotEmpty
    String template;

    @Builder
    public OneShotTransformationTemplate(
            @JsonProperty("provider") String provider,
            @JsonProperty("idPath") String idPath,
            @JsonProperty("fqlPath") String fqlPath,
            @JsonProperty("translationTemplateType") TranslationTemplateType translationTemplateType,
            @JsonProperty("template") String template,
            @JsonProperty("dropDetectionRule") String dropDetectionRule) {
        super(TransformationTemplateType.ONE_SHOT, idPath, fqlPath, translationTemplateType, provider, dropDetectionRule);
        this.template = template;
    }

    @Override
    public <T> T accept(TransformationTemplateVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
