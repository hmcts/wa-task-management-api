package uk.gov.hmcts.reform.wataskmanagementapi.cft.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.NoteResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WarningValues;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class CftQueryService {

    @Autowired
    private ObjectMapper objectMapper;
    private final TaskResourceRepository taskResourceRepository;

    public CftQueryService(TaskResourceRepository taskResourceRepository) {
        this.taskResourceRepository = taskResourceRepository;
    }

    public GetTasksResponse<Task> getAllTasks(
        int firstResult,
        int maxResults,
        SearchTaskRequest searchTaskRequest,
        AccessControlResponse accessControlResponse,
        List<PermissionTypes> permissionsRequired
    ) {
        Sort sort = SortQuery.sortByFields(searchTaskRequest);
        Pageable page;
        try {
            page = PageRequest.of(firstResult, maxResults, sort);
        } catch (IllegalArgumentException exp) {
            return new GetTasksResponse<>(Collections.emptyList(), 0);
        }

        final Specification<TaskResource> taskResourceSpecification = TaskResourceSpecification
            .buildTaskQuery(searchTaskRequest, accessControlResponse, permissionsRequired);

        final Page<TaskResource> pages = taskResourceRepository.findAll(taskResourceSpecification, page);

        final List<TaskResource> taskResources = pages.toList();

        return mapToTask(taskResources, pages.getTotalElements());
    }

    private GetTasksResponse<Task> mapToTask(List<TaskResource> taskResources, long totalNumberOfTasks) {
        final List<Task> tasks = taskResources.stream().map(taskResource ->
            new Task(taskResource.getTaskId(), taskResource.getTaskName(), taskResource.getTaskType(),
                taskResource.getState().getValue().toLowerCase(Locale.ROOT), taskResource.getTaskSystem().getValue(),
                taskResource.getSecurityClassification().getSecurityClassification(),
                taskResource.getTitle(),
                taskResource.getCreated() == null ? null : taskResource.getCreated().toZonedDateTime(),
                taskResource.getDueDateTime() == null ? null : taskResource.getDueDateTime().toZonedDateTime(),
                taskResource.getAssignee(), taskResource.getAutoAssigned(),
                taskResource.getExecutionTypeCode().getExecutionName(), taskResource.getJurisdiction(),
                taskResource.getRegion(), taskResource.getLocation(), taskResource.getLocationName(),
                taskResource.getCaseTypeId(), taskResource.getCaseId(), taskResource.getCaseCategory(),
                taskResource.getCaseName(), taskResource.getHasWarnings(), getWarnings(taskResource.getNotes()),
                null)
        ).collect(Collectors.toList());

        return new GetTasksResponse<>(tasks, totalNumberOfTasks);
    }

    private WarningValues getWarnings(List<NoteResource> notes) {
        if (notes != null && !notes.isEmpty()) {
            try {
                String warningValuesAsText = objectMapper.writeValueAsString(notes);
                return new WarningValues(warningValuesAsText);
            } catch (JsonProcessingException e) {
                return null;
            }
        }
        return null;
    }

    public Optional<TaskResource> getTask(String taskId,
                                          AccessControlResponse accessControlResponse,
                                          List<PermissionTypes> permissionsRequired
    ) {

        if (permissionsRequired.isEmpty()
            || taskId == null
            || taskId.isBlank()) {
            return Optional.empty();
        }
        final Specification<TaskResource> taskResourceSpecification = TaskResourceSpecification
            .buildSingleTaskQuery(taskId, accessControlResponse, permissionsRequired);

        return taskResourceRepository.findOne(taskResourceSpecification);

    }
}
