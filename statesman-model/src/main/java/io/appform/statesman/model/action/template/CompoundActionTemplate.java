package io.appform.statesman.model.action.template;

import io.appform.statesman.model.action.ActionType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
public class CompoundActionTemplate extends ActionTemplate {

    private List<String> actionTemplates;

    public CompoundActionTemplate() {
        super(ActionType.COMPOUND);
    }

    @Builder
    public CompoundActionTemplate(String templateId, String name, boolean active, List<String> actionTemplates) {
        super(ActionType.COMPOUND, templateId, name, active);
        this.actionTemplates = actionTemplates;
    }

    @Override
    public <T> T visit(ActionTemplateVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
