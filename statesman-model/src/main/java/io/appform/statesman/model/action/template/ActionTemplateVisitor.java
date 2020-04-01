package io.appform.statesman.model.action.template;

public interface ActionTemplateVisitor<T> {
    T visit(HttpActionTemplate httpActionTemplate);

    T visit(RoutedActionTemplate routedActionTemplate);

    T visit(CompoundActionTemplate compoundActionTemplate);
}
