package uk.gov.hmcts.reform.wataskmanagementapi.cft.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity(name = "taskAssignments")
@SuppressWarnings({"PMD.TooManyFields"})
public class TaskAssignmentsResource {
    public static final String TIMESTAMP_WITH_TIME_ZONE = "TIMESTAMP WITH TIME ZONE";

    @Id
    @EqualsAndHashCode.Include()
    private String assignmentId;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    private OffsetDateTime assignmentStart;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    private OffsetDateTime assignmentEnd;

    private String assignee;

    @EqualsAndHashCode.Include()
    private String taskId;
    private String service;
    private String location;
    private String roleCategory;
    private String taskName;
    private String assignmentEndReason;
}
