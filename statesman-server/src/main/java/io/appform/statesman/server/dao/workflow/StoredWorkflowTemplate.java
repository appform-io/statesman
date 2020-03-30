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
@Table(name = "workflow_templates", uniqueConstraints = {
        @UniqueConstraint(columnNames = "template_id")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StoredWorkflowTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grouping_key")
    private long id;

    @LookupKey
    @Column(name = "template_id")
    private String templateId;

    @Column(name = "name")
    private String name;


    @Column(name = "start_state", columnDefinition = "blob")
    private byte[] startState;

    @Column(name = "rules", columnDefinition = "blob")
    private byte[] rules;

    @Column(name = "active")
    private boolean active;

    @Column(name = "created", columnDefinition = "timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "timestamp default current_timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.ALWAYS)
    private Date updated;

    @Builder
    public StoredWorkflowTemplate(String templateId,
                                  String name,
                                  byte[] startState,
                                  byte[] rules,
                                  boolean active) {
        this.templateId = templateId;
        this.name = name;
        this.startState = startState;
        this.rules = rules;
        this.active = active;
    }
}
