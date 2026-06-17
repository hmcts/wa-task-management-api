package uk.gov.hmcts.reform.wataskmanagementapi.poc.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CreateTaskRequestTask {

    @NotNull
    private UUID externalTaskId;
    private UUID id;
    @NotBlank
    private String type;
    @NotBlank
    private String name;
    private String title;
    private String assignee;
    @NotNull
    private OffsetDateTime created;
    @NotNull
    private ExecutionType executionType;
    @NotBlank
    private String caseId;
    @NotBlank
    private String caseTypeId;
    @NotBlank
    private String caseCategory;
    @NotBlank
    private String caseName;
    @NotBlank
    private String jurisdiction;
    @NotBlank
    private String region;
    @NotBlank
    private String location;
    @NotBlank
    private String workType;
    @NotBlank
    private String roleCategory;
    @NotNull
    private SecurityClassification securityClassification;
    private String description;
    @NotNull
    private OffsetDateTime dueDateTime;
    private OffsetDateTime priorityDate;
    private Integer majorPriority = 5000;
    private Integer minorPriority = 500;
    private String locationName;
    private String regionName;
    private TaskSystem taskSystem = TaskSystem.SELF;
    private OffsetDateTime nextHearingDate;
    private String nextHearingId;
    private Map<String, Object> additionalProperties;
    @Valid
    @NotEmpty
    private List<TaskPermission> permissions;
}
