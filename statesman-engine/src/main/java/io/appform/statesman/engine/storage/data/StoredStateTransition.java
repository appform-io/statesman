package io.appform.statesman.engine.storage.data;

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
@Table(name = "state_transitions", uniqueConstraints = {
        @UniqueConstraint(columnNames = "transition_id")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StoredStateTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @LookupKey
    @Column(name = "transition_id")
    private String transitionId;

    @Column(name = "workflow_template_id")
    private String workflowTemplateId;

    @Column(name = "from_state")
    private String fromState;

    //Keeping it as blob to avoid alters
    @Column(name = "data")
    private byte[] data;

    @Column(name = "active")
    private boolean active;

    @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.ALWAYS)
    private Date updated;

    @Builder
    public StoredStateTransition(String transitionId,
                                 String workflowTemplateId,
                                 String fromState,
                                 byte[] data,
                                 boolean active) {
        this.transitionId = transitionId;
        this.workflowTemplateId = workflowTemplateId;
        this.fromState = fromState;
        this.data = data;
        this.active = active;
    }
}
