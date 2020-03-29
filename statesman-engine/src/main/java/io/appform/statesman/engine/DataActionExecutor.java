package io.appform.statesman.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.appform.statesman.model.DataActionVisitor;
import io.appform.statesman.model.DataObject;
import io.appform.statesman.model.DataUpdate;
import io.appform.statesman.model.MergeDataAction;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DataActionExecutor {
    public static JsonNode apply(final DataObject dataObject, final DataUpdate dataUpdate) {
        return dataUpdate.getDataAction().accept(new DataActionVisitor<JsonNode>() {
            @Override
            public JsonNode visit(MergeDataAction mergeDataAction) {
                return ((ObjectNode)dataObject.getData()).setAll((ObjectNode)dataUpdate.getData());
            }
        });
    }
}
