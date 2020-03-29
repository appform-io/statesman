package io.appform.statesman.model;

import lombok.Value;

import java.util.List;

/**
 *
 */
@Value
public class WorkflowTemplate {
    String id;
    String name;
    List<String> attributes;
}
