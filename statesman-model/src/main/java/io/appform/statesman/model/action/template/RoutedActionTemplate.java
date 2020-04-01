package io.appform.statesman.model.action.template;

import io.appform.statesman.model.action.ActionType;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
public class RoutedActionTemplate extends ActionTemplate {

    private String useCase;
    private Map<String, String> providerTemplates;

    public RoutedActionTemplate() {
        super(ActionType.ROUTED);
    }

    @Builder
    public RoutedActionTemplate(String templateId, String name, boolean active, String useCase, Map<String, String> providerTemplates) {
        super(ActionType.ROUTED, templateId, name, active);
        this.providerTemplates = providerTemplates;
        this.useCase = useCase;
    }

    @Override
    public <T> T visit(ActionTemplateVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
