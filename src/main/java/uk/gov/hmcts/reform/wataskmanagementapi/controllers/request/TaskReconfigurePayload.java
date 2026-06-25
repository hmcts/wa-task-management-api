package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TaskReconfigurePayload {

    @NotNull
    private UUID id;
    private String title;
    private String caseCategory;
    private String caseName;
    private String region;
    private String location;
    private String workType;
    private String roleCategory;
    private String description;
    private OffsetDateTime dueDateTime;
    private OffsetDateTime priorityDate;
    private Integer majorPriority;
    private Integer minorPriority;
    private String locationName;
    private OffsetDateTime nextHearingDate;
    private String nextHearingId;
    private Map<String, Object> additionalProperties;
    private List<TaskPermission> permissions;
}
