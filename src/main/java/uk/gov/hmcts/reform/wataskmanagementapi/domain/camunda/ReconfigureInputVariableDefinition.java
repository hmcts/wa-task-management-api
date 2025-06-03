package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TerminationProcess;

import java.time.OffsetDateTime;
import java.util.Map;


@AllArgsConstructor
@Builder
@ToString
@Getter
@JsonIgnoreProperties
@SuppressWarnings({"PMD.TooManyFields"})
public class ReconfigureInputVariableDefinition {
    private String taskId;
    private String name;
    private String taskType;

    private OffsetDateTime dueDate;

    private CFTTaskState taskState;
    private String title;
    private String description;
    private Integer majorPriority;
    private Integer minorPriority;
    private String assignee;
    private String workType;

    private String roleCategory;
    private Boolean hasWarnings;
    private String caseId;
    private String caseTypeId;
    private String caseName;
    private String caseManagementCategory;
    private String jurisdiction;
    private String region;
    private String regionName;
    private String location;
    private String locationName;
    private OffsetDateTime created;

    private Map<String, String> additionalProperties;

    private String nextHearingId;

    private OffsetDateTime nextHearingDate;

    private OffsetDateTime priorityDate;
    
    private TerminationProcess terminationProcess;
}
