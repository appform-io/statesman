package io.appform.statesman.server.dao.callback;

import io.appform.dropwizard.sharding.sharding.LookupKey;
import io.appform.statesman.server.callbacktransformation.TransformationTemplateType;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "callback_templates", uniqueConstraints = {
        @UniqueConstraint(columnNames = "provider")
})
@Data
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", length = 45)
@NoArgsConstructor
public abstract class StoredCallbackTransformationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "type", insertable = false, updatable = false)
    private TransformationTemplateType type;

    @LookupKey
    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "id_path")
    private String idPath;

    @Column(name = "template", columnDefinition = "blob")
    private byte[] template;

    @Column(name = "created", columnDefinition = "datetime default current_timestamp", updatable = false, insertable = false)
    @Generated(value = GenerationTime.INSERT)
    private Date created;

    @Column(name = "updated", columnDefinition = "datetime", updatable = false, insertable = false)
    @Generated(value = GenerationTime.ALWAYS)
    private Date updated;

    protected StoredCallbackTransformationTemplate(TransformationTemplateType type,
                                                   String provider,
                                                   String idPath,
                                                   byte[] template) {
        this.type = type;
        this.provider = provider;
        this.idPath = idPath;
        this.template = template;
    }

    public abstract <T> T visit(StoredCallbackTransformationTemplateVisitor<T> visitor);
}


