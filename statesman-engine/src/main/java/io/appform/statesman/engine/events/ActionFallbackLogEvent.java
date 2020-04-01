package io.appform.statesman.engine.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionFallbackLogEvent {
    String workflow;
    String workflowId;
    String workflowTemplateId;
    String actionType;
    String actionTemplateId;

}