package io.appform.statesman.model.action.template;

import io.appform.statesman.model.action.ActionType;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
public class RoutedHttpActionTemplate extends ActionTemplate {

    private String useCase;
    private Map<String, String> providerTemplates;

    public RoutedHttpActionTemplate() {
        super(ActionType.ROUTED_HTTP);
    }

    @Builder
    public RoutedHttpActionTemplate(String templateId, String name, boolean active, String useCase, Map<String, String> providerTemplates) {
        super(ActionType.ROUTED_HTTP, templateId, name, active);
        this.providerTemplates = providerTemplates;
        this.useCase = useCase;
    }

    @Override
    public <T> T visit(ActionTemplateVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
