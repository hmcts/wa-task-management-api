package uk.gov.hmcts.reform.wataskmanagementapi.entity.replica;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.springframework.context.annotation.Profile;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.BusinessContext;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.NoteResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.WorkTypeResource;

import java.io.Serial;
import java.io.Serializable;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ToString
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Profile("replica | preview")
@Entity(name = "replica_tasks")
@Table(name = "tasks")
@SuppressWarnings({"PMD.ExcessiveParameterList", "PMD.TooManyFields",
    "PMD.UnnecessaryFullyQualifiedName", "PMD.ExcessiveImports"})
public class ReplicaTaskResource implements Serializable {

    @Serial
    private static final long serialVersionUID = -4548149206960026543L;

    private static final String PGSQL_ENUM = "pgsql_enum";
    public static final String JSONB = "jsonb";
    public static final String TIMESTAMP_WITH_TIME_ZONE = "TIMESTAMP WITH TIME ZONE";
    public static final String TIMESTAMP = "TIMESTAMP";

    @Id
    @EqualsAndHashCode.Include()
    @Schema(name = "task_id")
    private String taskId;

    @Schema(name = "task_type")
    private String taskType;

    @Schema(name = "task_name")
    private String taskName;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(columnDefinition = "task_system_enum")
    @Schema(name = "task_system")
    private TaskSystem taskSystem;

    @Column(columnDefinition = TIMESTAMP)
    @JdbcTypeCode(Types.TIMESTAMP)
    @Schema(name = "due_date_time")
    private OffsetDateTime dueDateTime;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(columnDefinition = "security_classification_enum")
    @Schema(name = "security_classification")
    private SecurityClassification securityClassification;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(columnDefinition = "task_state_enum")
    private CFTTaskState state;

    @Schema(name = "role_category")
    private String roleCategory;

    private String jurisdiction;

    private String region;

    @Schema(name = "region_name")
    private String regionName;

    private String location;

    @Schema(name = "location_name")
    private String locationName;

    private String title;

    private String description;

    @Column(columnDefinition = TIMESTAMP)
    @JdbcTypeCode(Types.TIMESTAMP)
    private OffsetDateTime created;

    @Type(JsonType.class)
    @Column(columnDefinition = JSONB)
    private List<NoteResource> notes;

    @Schema(name = "minor_priority")
    private Integer minorPriority;

    @Schema(name = "major_priority")
    private Integer majorPriority;

    private String assignee;
    @Schema(name = "auto_assigned")
    private Boolean autoAssigned = false;

    @Column(columnDefinition = TIMESTAMP)
    @JdbcTypeCode(Types.TIMESTAMP)
    @Schema(name = "assignment_expiry")
    private OffsetDateTime assignmentExpiry;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "work_type", referencedColumnName = "work_type_id")
    @Schema(name = "work_type_resource")
    private WorkTypeResource workTypeResource;

    @Schema(name = "case_name")
    private String caseName;

    @EqualsAndHashCode.Include()
    @Schema(name = "case_id")
    private String caseId;

    @Schema(name = "case_category")
    private String caseCategory;

    @Schema(name = "case_type_id")
    private String caseTypeId;

    @Schema(name = "has_warnings")
    private Boolean hasWarnings = false;

    private Boolean indexed = false;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(columnDefinition = "business_context_enum")
    @Schema(name = "business_context")
    private BusinessContext businessContext;

    @Schema(name = "termination_reason")
    private String terminationReason;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "executionTypeCode", referencedColumnName = "execution_code")
    @Schema(name = "execution_type_code")
    private ExecutionTypeResource executionTypeCode;

    @JsonManagedReference
    @ToString.Exclude
    @OneToMany(mappedBy = "taskResource", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Schema(name = "task_role_resources")
    private Set<TaskRoleResource> taskRoleResources;

    @Column(columnDefinition = TIMESTAMP)
    @JdbcTypeCode(Types.TIMESTAMP)
    @Schema(name = "reconfigure_request_time")
    private OffsetDateTime reconfigureRequestTime;

    @Column(columnDefinition = TIMESTAMP)
    @JdbcTypeCode(Types.TIMESTAMP)
    @Schema(name = "last_reconfiguration_time")
    private OffsetDateTime lastReconfigurationTime;

    @Type(JsonType.class)
    @Column(columnDefinition = JSONB)
    @Schema(name = "additional_properties")
    private Map<String, String> additionalProperties;

    @Schema(name = "next_hearing_id")
    private String nextHearingId;

    @Column(columnDefinition = TIMESTAMP)
    @JdbcTypeCode(Types.TIMESTAMP)
    @Schema(name = "last_updated_timestamp")
    private OffsetDateTime lastUpdatedTimestamp;

    @Schema(name = "last_updated_user")
    private String lastUpdatedUser;

    @Schema(name = "last_updated_action")
    private String lastUpdatedAction;

    @Column(columnDefinition = TIMESTAMP)
    @JdbcTypeCode(Types.TIMESTAMP)
    @Schema(name = "next_hearing_date")
    private OffsetDateTime nextHearingDate;

    @Column(columnDefinition = TIMESTAMP)
    @JdbcTypeCode(Types.TIMESTAMP)
    @Schema(name = "priority_date")
    private OffsetDateTime priorityDate;

    @Column(columnDefinition = TIMESTAMP)
    @JdbcTypeCode(Types.TIMESTAMP)
    @Schema(name = "report_refresh_request_time")
    private OffsetDateTime reportRefreshRequestTime;

    @Schema(name = "termination_process")
    private String terminationProcess;

}
