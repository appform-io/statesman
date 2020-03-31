package io.appform.statesman.server.callbacktransformation;

import io.appform.statesman.server.callbacktransformation.impl.OneShotTransformationTemplate;
import io.appform.statesman.server.callbacktransformation.impl.StepByStepTransformationTemplate;

/**
 *
 */
public interface TransformationTemplateVisitor<T> {

    T visit(OneShotTransformationTemplate oneShotTransformationTemplate);

    T visit(StepByStepTransformationTemplate stepByStepTransformationTemplate);
}
