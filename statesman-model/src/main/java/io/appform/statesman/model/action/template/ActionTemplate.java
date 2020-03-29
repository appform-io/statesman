package io.appform.statesman.model.action.template;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.appform.statesman.model.action.ActionType;
import io.appform.statesman.model.action.data.ActionData;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = HttpActionTemplate.class, name = "HTTP"),
})
public abstract class ActionTemplate {

    protected boolean sync;

    protected final ActionType type;

    public ActionTemplate(ActionType type) {
        this.type = type;
    }

    public ActionTemplate(ActionType type, boolean sync) {
        this.type = type;
        this.sync = sync;
    }
}
