package io.appform.statesman.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.appform.statesman.model.DataObject;
import io.appform.statesman.model.DataUpdate;
import io.appform.statesman.model.dataaction.DataActionVisitor;
import io.appform.statesman.model.dataaction.impl.MergeDataAction;
import io.appform.statesman.model.dataaction.impl.MergeSelectedDataAction;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

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
                objectNode.setAll((ObjectNode)dataObject.getData());
                final ObjectNode updateData = (ObjectNode) dataUpdate.getData();
                StreamSupport.stream(Spliterators.spliteratorUnknownSize(updateData.fields(), Spliterator.ORDERED), false)
                        .forEach(e -> {
                            if(objectNode.has(e.getKey())) {
                                val existingNode = objectNode.get(e.getKey());
                                if(existingNode.isArray()) {
                                    ((ArrayNode)existingNode).add(e.getValue());
                                }
                                else {
                                    objectNode.set(e.getKey(), e.getValue());
                                }
                            } else {
                                objectNode.set(e.getKey(), e.getValue());
                            }
                        });
                return objectNode;
            }

            @Override
            public JsonNode visit(MergeSelectedDataAction mergeSelectedDataAction) {
                ObjectNode objectNode = mapper.createObjectNode();
                objectNode.setAll((ObjectNode)dataObject.getData());
                val updateNode = dataUpdate.getData();
                mergeSelectedDataAction.getFields()
                        .forEach(field -> {
                            if(!updateNode.has(field)) {
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
