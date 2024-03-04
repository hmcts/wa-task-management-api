package uk.gov.hmcts.reform.wataskmanagementapi.entity.replica;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import com.vladmihalcea.hibernate.type.json.JsonType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.context.annotation.Profile;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.BusinessContext;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.NoteResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.WorkTypeResource;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@ToString
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Profile("replica | preview")
@Entity(name = "replica_tasks")
@Table(name = "tasks")
@TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class)
@TypeDef(name = ReplicaTaskResource.JSONB, typeClass = JsonType.class)
@SuppressWarnings({"PMD.ExcessiveParameterList", "PMD.TooManyFields",
    "PMD.UnnecessaryFullyQualifiedName", "PMD.ExcessiveImports"})
public class ReplicaTaskResource implements Serializable {

    private static final long serialVersionUID = -4550112481797873963L;

    private static final String PGSQL_ENUM = "pgsql_enum";
    public static final String JSONB = "jsonb";
    public static final String TIMESTAMP_WITH_TIME_ZONE = "TIMESTAMP WITH TIME ZONE";

    @Id
    @EqualsAndHashCode.Include()
    @Schema(name = "task_id")
    private String taskId;
    @Schema(name = "task_name")
    private String taskName;
    @Schema(name = "task_type")
    private String taskType;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    @Schema(name = "due_date_time")
    private OffsetDateTime dueDateTime;

    @Enumerated(EnumType.STRING)
    @Type(type = PGSQL_ENUM)
    @Column(columnDefinition = "task_state_enum")
    private CFTTaskState state;

    @Enumerated(EnumType.STRING)
    @Type(type = PGSQL_ENUM)
    @Column(columnDefinition = "task_system_enum")
    @Schema(name = "task_system")
    private TaskSystem taskSystem;

    @Enumerated(EnumType.STRING)
    @Type(type = PGSQL_ENUM)
    @Column(columnDefinition = "security_classification_enum")
    @Schema(name = "security_classification")
    private SecurityClassification securityClassification;

    private String title;
    private String description;

    @Type(type = "jsonb")
    @Column(columnDefinition = JSONB)
    private List<NoteResource> notes;

    @Schema(name = "major_priority")
    private Integer majorPriority;
    @Schema(name = "minor_priority")
    private Integer minorPriority;
    private String assignee;
    @Schema(name = "auto_assigned")
    private Boolean autoAssigned = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "work_type", referencedColumnName = "work_type_id")
    @Schema(name = "work_type_resource")
    private WorkTypeResource workTypeResource;

    @Schema(name = "role_category")
    private String roleCategory;
    @Schema(name = "has_warnings")
    private Boolean hasWarnings = false;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    @Schema(name = "assignment_expiry")
    private OffsetDateTime assignmentExpiry;
    @EqualsAndHashCode.Include()
    @Schema(name = "case_id")
    private String caseId;
    @Schema(name = "case_type_id")
    private String caseTypeId;
    @Schema(name = "case_name")
    private String caseName;
    @Schema(name = "case_category")
    private String caseCategory;
    private String jurisdiction;
    private String region;
    @Schema(name = "region_name")
    private String regionName;
    private String location;
    @Schema(name = "location_name")
    private String locationName;

    @Enumerated(EnumType.STRING)
    @Type(type = PGSQL_ENUM)
    @Column(columnDefinition = "business_context_enum")
    @Schema(name = "business_context")
    private BusinessContext businessContext;

    @Schema(name = "termination_reason")
    private String terminationReason;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    private OffsetDateTime created;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "executionTypeCode", referencedColumnName = "execution_code")
    @Schema(name = "execution_type_code")
    private ExecutionTypeResource executionTypeCode;

    @JsonManagedReference
    @ToString.Exclude
    @OneToMany(mappedBy = "taskResource", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Schema(name = "task_role_resources")
    private Set<TaskRoleResource> taskRoleResources;

    @Type(type = "jsonb")
    @Column(columnDefinition = JSONB)
    @Schema(name = "additional_properties")
    private Map<String, String> additionalProperties;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    @Schema(name = "reconfigure_request_time")
    private OffsetDateTime reconfigureRequestTime;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    @Schema(name = "last_reconfiguration_time")
    private OffsetDateTime lastReconfigurationTime;

    @Schema(name = "next_hearing_id")
    private String nextHearingId;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    @Schema(name = "next_hearing_date")
    private OffsetDateTime nextHearingDate;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    @Schema(name = "priority_date")
    private OffsetDateTime priorityDate;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    @Schema(name = "last_updated_timestamp")
    private OffsetDateTime lastUpdatedTimestamp;

    @Schema(name = "last_updated_user")
    private String lastUpdatedUser;

    @Schema(name = "last_updated_action")
    private String lastUpdatedAction;

    private Boolean indexed = false;

    @Column(columnDefinition = TIMESTAMP_WITH_TIME_ZONE)
    @Schema(name = "report_refresh_request_time")
    private OffsetDateTime reportRefreshRequestTime;

    protected ReplicaTaskResource() {
        // required for runtime proxy generation in Hibernate
    }

}
