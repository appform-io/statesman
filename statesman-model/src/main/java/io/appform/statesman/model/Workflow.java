package io.appform.statesman.model;

import lombok.*;

/**
 *
 */
@Data
@Value
@Builder
@AllArgsConstructor
public class Workflow {
    String id;
    String templateId;
    DataObject dataObject;
}
