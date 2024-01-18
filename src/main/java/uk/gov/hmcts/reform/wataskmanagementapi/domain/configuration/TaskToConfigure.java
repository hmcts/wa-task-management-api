package uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Builder(toBuilder = true)
@Data
public class TaskToConfigure {
    private final String id;
    private final String taskTypeId;
    private final String caseId;
    private final String name;
    @Builder.Default
    private Map<String, Object> taskAttributes = Map.of();

    public TaskToConfigure(String id,
                           String taskTypeId,
                           String caseId,
                           String name) {
        this.id = id;
        this.taskTypeId = taskTypeId;
        this.caseId = caseId;
        this.name = name;
    }

    public TaskToConfigure(String id,
                           String taskTypeId,
                           String caseId,
                           String name,
                           Map<String, Object> taskAttributes) {
        this(id, taskTypeId, caseId, name);
        this.taskAttributes = taskAttributes;
    }
}

