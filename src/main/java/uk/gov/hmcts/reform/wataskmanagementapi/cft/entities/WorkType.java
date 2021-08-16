package uk.gov.hmcts.reform.wataskmanagementapi.cft.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;


@EqualsAndHashCode
@Getter
@ToString
@Entity(name = "work_types")
public class WorkType implements Serializable {

    private static final long serialVersionUID = -5241589570453132489L;

    @Id
    private String id;

    private String label;

    protected WorkType() {
        // required for runtime proxy generation in Hibernate
    }

    public WorkType(String id, String label) {
        this.id = id;
        this.label = label;
    }

}
