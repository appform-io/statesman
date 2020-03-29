package io.appform.statesman.model;

import lombok.Value;

/**
 *
 */
@Value
public class Workflow {
    String id;
    String templateId;
    DataObject dataObject;
}
