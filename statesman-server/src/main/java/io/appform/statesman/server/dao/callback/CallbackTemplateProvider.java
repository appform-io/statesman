package io.appform.statesman.server.dao.callback;

import io.appform.statesman.server.callbacktransformation.TransformationTemplate;
import io.appform.statesman.server.callbacktransformation.TranslationTemplateType;

import java.util.Optional;
import java.util.Set;

public interface CallbackTemplateProvider {

    Optional<TransformationTemplate> createTemplate(TransformationTemplate workflowTemplate);

    Optional<TransformationTemplate> updateTemplate(TransformationTemplate workflowTemplate);

    Optional<TransformationTemplate> getTemplate(String provider, TranslationTemplateType translationTemplateType);

    Set<TransformationTemplate> getAll();
}
