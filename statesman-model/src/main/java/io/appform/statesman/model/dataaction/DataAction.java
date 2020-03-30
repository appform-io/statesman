package io.appform.statesman.model.dataaction;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@Data
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class DataAction {
    private final DataActionType type;

    public abstract <T> T accept(DataActionVisitor<T> visitor);
}
