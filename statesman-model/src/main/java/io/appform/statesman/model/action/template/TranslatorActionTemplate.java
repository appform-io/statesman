package io.appform.statesman.model.action.template;

import io.appform.statesman.model.action.ActionType;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

@Data
public class TranslatorActionTemplate extends ActionTemplate {

    @NotNull
    @NotEmpty
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
