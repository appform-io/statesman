package io.appform.statesman.server.dao.transition;

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

    @Column(name = "transition_id", unique = true)
    private String transitionId;

    @Column(name = "workflow_template_id")
    private String workflowTemplateId;

    @Column(name = "from_state")
    private String fromState;

    //Keeping it as blob to avoid alters
    @Column(name = "data", columnDefinition = "blob")
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
