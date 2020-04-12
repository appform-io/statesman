package io.appform.statesman.engine.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInitEvent {

    String workflowId;

    String workflowTemplateId;

    String currentState;
}
