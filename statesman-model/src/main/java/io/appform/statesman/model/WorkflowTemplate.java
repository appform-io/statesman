package io.appform.statesman.model;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.util.List;

/**
 *
 */
@Value
@Data
@Builder
public class WorkflowTemplate {
    String id;
    String name;
    List<String> attributes;
}
