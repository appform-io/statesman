package io.appform.statesman.server.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import io.appform.statesman.server.callbacktransformation.TransformationTemplate;
import io.appform.statesman.server.callbacktransformation.TransformationTemplateVisitor;
import io.appform.statesman.server.callbacktransformation.impl.OneShotTransformationTemplate;
import io.appform.statesman.server.callbacktransformation.impl.StepByStepTransformationTemplate;
import io.appform.statesman.server.dao.callback.StoredCallbackTransformationTemplate;
import io.appform.statesman.server.dao.callback.StoredCallbackTransformationTemplateVisitor;
import io.appform.statesman.server.dao.callback.impl.StoredOneShotCallbackTransformationTemplate;
import io.appform.statesman.server.dao.callback.impl.StoredStepByStepCallbackTransformationTemplate;

import java.util.List;

public class CallbackTemplateUtils {

    public static StoredCallbackTransformationTemplate toDao(TransformationTemplate transformationTemplate) {

        return transformationTemplate.accept(new TransformationTemplateVisitor<StoredCallbackTransformationTemplate>() {
            @Override
            public StoredCallbackTransformationTemplate visit(
                    OneShotTransformationTemplate oneShotTransformationTemplate) {
                return StoredOneShotCallbackTransformationTemplate.builder()
                        .provider(oneShotTransformationTemplate.getProvider())
                        .idPath(oneShotTransformationTemplate.getIdPath())
                        .translationTemplateType(oneShotTransformationTemplate.getTranslationTemplateType())
                        .template(MapperUtils.serialize(oneShotTransformationTemplate.getTemplate()))
                        .dropDetectionRule(transformationTemplate.getDropDetectionRule())
                        .build();
            }

            @Override
            public StoredCallbackTransformationTemplate visit(
                    StepByStepTransformationTemplate stepByStepTransformationTemplate) {
                return StoredStepByStepCallbackTransformationTemplate.builder()
                        .provider(stepByStepTransformationTemplate.getProvider())
                        .idPath(stepByStepTransformationTemplate.getIdPath())
                        .translationTemplateType(stepByStepTransformationTemplate.getTranslationTemplateType())
                        .template(MapperUtils.serialize(stepByStepTransformationTemplate.getTemplates()))
                        .dropDetectionRule(transformationTemplate.getDropDetectionRule())
                        .build();
            }
        });
    }

    public static TransformationTemplate toDto(StoredCallbackTransformationTemplate callbackTransformationTemplate) {
        return callbackTransformationTemplate.visit(
                new StoredCallbackTransformationTemplateVisitor<TransformationTemplate>() {
                    @Override
                    public TransformationTemplate visit(
                            StoredOneShotCallbackTransformationTemplate template) {
                        return OneShotTransformationTemplate.builder()
                                .provider(template.getProvider())
                                .idPath(template.getIdPath())
                                .translationTemplateType(template.getTranslationTemplateType())
                                .template(MapperUtils.deserialize(template.getTemplate(),String.class))
                                .dropDetectionRule(template.getDropDetectionRule())
                                .build();

                    }

                    @Override
                    public TransformationTemplate visit(
                            StoredStepByStepCallbackTransformationTemplate template) {
                        return StepByStepTransformationTemplate.builder()
                                .provider(template.getProvider())
                                .idPath(template.getIdPath())
                                .translationTemplateType(template.getTranslationTemplateType())
                                .templates(MapperUtils.deserialize(template.getTemplate(),new TypeReference<List<StepByStepTransformationTemplate.StepSelection>>() {}))
                                .dropDetectionRule(template.getDropDetectionRule())
                                .build();
                    }
                });
    }

}
