package io.appform.statesman.model;

import lombok.Builder;
import lombok.Value;

/**
 *
 */
@Value
@Builder
public class Workflow {
    String id;
    String templateId;
    DataObject dataObject;
}
