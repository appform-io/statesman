package io.appform.statesman.model;

import lombok.*;

import java.util.List;

/**
 *
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowTemplate {
    String id;
    String name;
    List<String> attributes;
}
