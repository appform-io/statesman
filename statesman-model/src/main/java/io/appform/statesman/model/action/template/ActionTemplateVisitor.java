package io.appform.statesman.model.action.template;

public interface ActionTemplateVisitor<T> {
    T visit(HttpActionTemplate httpActionTemplate);

    T visit(RoutedHttpActionTemplate routedHttpActionTemplate);

    T visit(CompoundHttpActionTemplate compoundHttpActionTemplate);
}
