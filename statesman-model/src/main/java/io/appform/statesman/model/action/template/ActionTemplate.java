package io.appform.statesman.model.action.template;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.appform.statesman.model.action.ActionType;
import lombok.Data;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

@Data
@ToString
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = HttpActionTemplate.class, name = "HTTP"),
        @JsonSubTypes.Type(value = RoutedActionTemplate.class, name = "ROUTED"),
        @JsonSubTypes.Type(value = CompoundActionTemplate.class, name = "COMPOUND"),
        @JsonSubTypes.Type(value = TranslatorActionTemplate.class, name = "TRANSLATOR"),
})
public abstract class ActionTemplate {

    protected String templateId;

    @NotNull
    @NotEmpty
    protected String name;

    protected boolean active;

    protected final ActionType type;

    public ActionTemplate(ActionType type) {
        this.type = type;
    }

    public ActionTemplate(ActionType type, String templateId, String name, boolean active) {
        this.type = type;
        this.templateId = templateId;
        this.name = name;
        this.active = active;
    }

    public abstract <T> T visit(ActionTemplateVisitor<T> visitor);

}
