package uk.gov.hmcts.reform.wataskmanagementapi.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity(name = "taskAssignments")
@SuppressWarnings({"PMD.TooManyFields"})
public class TaskAssignmentsResource {
    public static final String TIMESTAMP = "TIMESTAMP";

    @Id
    @EqualsAndHashCode.Include()
    private String assignmentId;

    @Column(columnDefinition = TIMESTAMP)
    @JdbcTypeCode(Types.TIMESTAMP)
    private OffsetDateTime assignmentStart;

    @Column(columnDefinition = TIMESTAMP)
    @JdbcTypeCode(Types.TIMESTAMP)
    private OffsetDateTime assignmentEnd;

    private String assignee;

    @EqualsAndHashCode.Include()
    private String taskId;
    private String service;
    private String location;
    private String roleCategory;
    private String taskName;
    private String assignmentEndReason;

    @Column(columnDefinition = TIMESTAMP)
    private OffsetDateTime reportRefreshTime;

}
