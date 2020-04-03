package io.appform.statesman.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.appform.statesman.model.DataObject;
import io.appform.statesman.model.DataUpdate;
import io.appform.statesman.model.dataaction.DataActionVisitor;
import io.appform.statesman.model.dataaction.impl.MergeDataAction;
import io.appform.statesman.model.dataaction.impl.MergeSelectedDataAction;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 *
 */
@Singleton
public class DataActionExecutor {
    private final ObjectMapper mapper;

    @Inject
    public DataActionExecutor(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public JsonNode apply(final DataObject dataObject, final DataUpdate dataUpdate) {
        return dataUpdate.getDataAction().accept(new DataActionVisitor<JsonNode>() {
            @Override
            public JsonNode visit(MergeDataAction mergeDataAction) {
                ObjectNode objectNode = mapper.createObjectNode();
                objectNode.setAll((ObjectNode) dataObject.getData());
                objectNode.setAll((ObjectNode)dataUpdate.getData());
                return objectNode;
            }

            @Override
            public JsonNode visit(MergeSelectedDataAction mergeSelectedDataAction) {
                ObjectNode objectNode = mapper.createObjectNode();
                objectNode.setAll((ObjectNode) dataObject.getData());
                val updateNode = dataUpdate.getData();
                mergeSelectedDataAction.getFields()
                        .forEach(field -> {
                            if (!updateNode.has(field)) {
                                throw new IllegalStateException(
                                        "Required field " + field + " not present in update for: " + dataUpdate.getWorkflowId());
                            }
                            objectNode.put(field, updateNode.get(field));
                        });
                return objectNode;
            }
        });
    }
}
