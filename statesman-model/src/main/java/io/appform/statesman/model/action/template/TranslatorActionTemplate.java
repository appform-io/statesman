package io.appform.statesman.model.action.template;

import io.appform.statesman.model.action.ActionType;
import lombok.Builder;
import lombok.Data;

@Data
public class TranslatorActionTemplate extends ActionTemplate {
    private String translator;

    public TranslatorActionTemplate() {
        super(ActionType.TRANSLATOR);
    }

    @Builder
    public TranslatorActionTemplate(String templateId, String name, boolean active, String translator) {
        super(ActionType.TRANSLATOR, templateId, name, active);
        this.translator = translator;
    }

    @Override
    public <T> T visit(ActionTemplateVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
