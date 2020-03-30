package io.appform.statesman.model;

import lombok.Value;

import java.util.List;

/**
 *
 */
@Value
public class AppliedTransitions {
    String workflowId;
    List<AppliedTransition> transitions;
}
