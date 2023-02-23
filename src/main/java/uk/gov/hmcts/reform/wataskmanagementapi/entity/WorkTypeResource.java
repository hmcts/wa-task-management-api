package uk.gov.hmcts.reform.wataskmanagementapi.entity;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;


@EqualsAndHashCode
@ToString
@Entity(name = "work_types")
public class WorkTypeResource implements Serializable {

    private static final long serialVersionUID = -5241589570453132489L;

    @Id
    @Column(name = "work_type_id")
    private String id;

    private String label;

    protected WorkTypeResource() {
        // required for runtime proxy generation in Hibernate
    }

    public WorkTypeResource(String id) {
        this(id, "");
    }

    public WorkTypeResource(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }
}
