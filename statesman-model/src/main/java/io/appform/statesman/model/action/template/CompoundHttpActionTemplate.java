package io.appform.statesman.model.action.template;

import io.appform.statesman.model.action.ActionType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
public class CompoundHttpActionTemplate extends ActionTemplate {

    private List<String> actionTemplates;

    public CompoundHttpActionTemplate() {
        super(ActionType.COMPOUNDED_HTTP);
    }

    @Builder
    public CompoundHttpActionTemplate(String templateId, String name, boolean active, List<String> actionTemplates) {
        super(ActionType.COMPOUNDED_HTTP, templateId, name, active);
        this.actionTemplates = actionTemplates;
    }

    @Override
    public <T> T visit(ActionTemplateVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
