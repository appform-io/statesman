package io.appform.statesman.model;

import lombok.*;

import java.util.Date;

/**
 *
 */
@Value
@Builder
@AllArgsConstructor
public class Workflow {
    String id;
    String templateId;
    DataObject dataObject;
    Date created;
    Date updated;
}
