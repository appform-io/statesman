package io.appform.statesman.server.dao.workflow;

import io.appform.dropwizard.sharding.sharding.LookupKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "workflow_instances", uniqueConstraints = {
        @UniqueConstraint(columnNames = "workflow_id")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StoredWorkflowInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grouping_key")
    private long id;

    @LookupKey
    @Column(name = "workflow_id")
    private String workflowId;

    @Column(name = "template_id")
    private String templateId;

    @Column(name = "data", columnDefinition = "blob")
    private byte[] data;

    @Column(name = "current_state")
    private String currentState;

    @Column(name = "completed")
    private boolean completed;

    @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.ALWAYS)
    private Date updated;

    @Builder
    public StoredWorkflowInstance(String templateId,
                                  String workflowId,
                                  String currentState,
                                  boolean completed,
                                  byte[] data) {
        this.templateId = templateId;
        this.currentState = currentState;
        this.completed = completed;
        this.workflowId = workflowId;
        this.data = data;
    }
}
