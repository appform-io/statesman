package io.appform.statesman.server.dao.callback.impl;

import io.appform.statesman.server.callbacktransformation.TransformationTemplateType;
import io.appform.statesman.server.callbacktransformation.TranslationTemplateType;
import io.appform.statesman.server.dao.callback.StoredCallbackTransformationTemplate;
import io.appform.statesman.server.dao.callback.StoredCallbackTransformationTemplateVisitor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@DiscriminatorValue(value = "STEP_BY_STEP")
public class StoredStepByStepCallbackTransformationTemplate extends StoredCallbackTransformationTemplate {

    @Builder
    public StoredStepByStepCallbackTransformationTemplate(String provider,
                                                          String idPath,
                                                          TranslationTemplateType translationTemplateType,
                                                          byte[] template) {
        super(TransformationTemplateType.STEP_BY_STEP, provider, idPath, translationTemplateType,  template);
    }

    @Override
    public <T> T visit(StoredCallbackTransformationTemplateVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
