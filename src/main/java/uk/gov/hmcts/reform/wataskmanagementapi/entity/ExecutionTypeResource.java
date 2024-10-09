package uk.gov.hmcts.reform.wataskmanagementapi.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;

import java.io.Serializable;

@EqualsAndHashCode
@Getter
@ToString
@Entity(name = "execution_types")
@Builder
public class ExecutionTypeResource implements Serializable {

    private static final long serialVersionUID = -5241589570453132436L;

    @Id
    @Column(name = "execution_code")
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Schema(name = "execution_code")
    private ExecutionType executionCode;

    @Column(name = "execution_name")
    @Schema(name = "execution_name")
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
