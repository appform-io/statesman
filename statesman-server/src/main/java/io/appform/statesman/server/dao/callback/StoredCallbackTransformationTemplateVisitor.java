package io.appform.statesman.server.dao.callback;

import io.appform.statesman.server.dao.callback.impl.StoredOneShotCallbackTransformationTemplate;
import io.appform.statesman.server.dao.callback.impl.StoredStepByStepCallbackTransformationTemplate;

public interface StoredCallbackTransformationTemplateVisitor<T> {

    T visit(StoredOneShotCallbackTransformationTemplate storedOneShotCallbackTransformationTemplate);

    T visit(StoredStepByStepCallbackTransformationTemplate storedStepByStepCallbackTransformationTemplate);
}
