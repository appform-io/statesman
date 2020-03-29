package io.appform.statesman.model.action.data;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.appform.statesman.model.action.ActionType;
import io.appform.statesman.model.action.data.impl.HttpActionData;
import lombok.Data;
import lombok.ToString;


@Data
@ToString
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = HttpActionData.class, name = "HTTP"),
})
public abstract class ActionData {

    protected final ActionType type;

    public ActionData(ActionType type) {
        this.type = type;
    }
}
