package uk.gov.hmcts.reform.wataskmanagementapi.entity;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;

@EqualsAndHashCode
@Getter
@ToString
@Entity(name = "execution_types")
@TypeDef(
    name = "pgsql_enum",
    typeClass = PostgreSQLEnumType.class
)
@Builder
public class ExecutionTypeResource implements Serializable {

    private static final long serialVersionUID = -5241589570453132436L;

    @Id
    @Column(name = "execution_code")
    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    private ExecutionType executionCode;

    @Column(name = "execution_name")
    private String executionName;

    private String description;

    protected ExecutionTypeResource() {
        // required for runtime proxy generation in Hibernate
    }

    public ExecutionTypeResource(ExecutionType executionCode, String executionName, String description) {
        this.executionCode = executionCode;
        this.executionName = executionName;
        this.description = description;
    }

}
