package io.appform.statesman.server.dao.action;

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
@Table(name = "action_templates", uniqueConstraints = {
        @UniqueConstraint(columnNames = "template_id")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StoredActionTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "groupingKey")
    private long id;

    @LookupKey
    @Column(name = "template_id")
    private String templateId;

    @Column(name = "name")
    private String name;

    @Column(name = "action_type")
    private String actionType;

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
    public StoredActionTemplate(String templateId,
                                  String name,
                                  String actionType,
                                  byte[] data,
                                  boolean active) {
        this.templateId = templateId;
        this.name = name;
        this.actionType = actionType;
        this.data = data;
        this.active = active;
    }
}
