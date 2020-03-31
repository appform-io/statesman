package io.appform.statesman.server.callbacktransformation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.appform.statesman.server.callbacktransformation.impl.OneShotTransformationTemplate;
import io.appform.statesman.server.callbacktransformation.impl.StepByStepTransformationTemplate;
import lombok.Data;

/**
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(
                name = TransformationTemplateType.Values.ONE_SHOT,
                value = OneShotTransformationTemplate.class),
        @JsonSubTypes.Type(
                name = TransformationTemplateType.Values.STEP_BY_STEP,
                value = StepByStepTransformationTemplate.class),
})
@Data
public abstract class TransformationTemplate {

    private final TransformationTemplateType type;
    private final String idPath;

    protected TransformationTemplate(TransformationTemplateType type, String idPath) {
        this.type = type;
        this.idPath = idPath;
    }

    public abstract <T> T accept(TransformationTemplateVisitor<T> visitor);
}
