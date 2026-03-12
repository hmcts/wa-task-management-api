package uk.gov.hmcts.reform.wataskmanagementapi.poc.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.poc.request.GetTaskResponseItem;

@Mapper(componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface GetTasksResponseMapper {

    @Mapping(source = "taskId", target = "id")
    @Mapping(source = "taskName", target = "name")
    @Mapping(source = "taskType", target = "type")
    @Mapping(source = "state", target = "taskState")
    @Mapping(source = "title", target = "taskTitle")
    @Mapping(source = "created", target = "createdDate")
    @Mapping(source = "dueDateTime", target = "dueDate")
    @Mapping(source = "hasWarnings", target = "warnings")
    @Mapping(source = "caseCategory", target = "caseManagementCategory")
    @Mapping(source = "assignee", target = "assignee")
    @Mapping(source = "taskSystem", target = "taskSystem")
    @Mapping(source = "securityClassification", target = "securityClassification")
    @Mapping(source = "locationName", target = "locationName")
    @Mapping(source = "location", target = "location")
    @Mapping(source = "jurisdiction", target = "jurisdiction")
    @Mapping(source = "region", target = "region")
    @Mapping(source = "caseTypeId", target = "caseTypeId")
    @Mapping(source = "caseId", target = "caseId")
    @Mapping(source = "caseCategory", target = "caseCategory")
    @Mapping(source = "caseName", target = "caseName")
    @Mapping(source = "autoAssigned", target = "autoAssigned")
    @Mapping(source = "description", target = "description")
    @Mapping(source = "roleCategory", target = "roleCategory")
    @Mapping(source = "additionalProperties", target = "additionalProperties")
    @Mapping(source = "nextHearingId", target = "nextHearingId")
    @Mapping(source = "nextHearingDate", target = "nextHearingDate")
    @Mapping(source = "minorPriority", target = "minorPriority")
    @Mapping(source = "majorPriority", target = "majorPriority")
    @Mapping(source = "priorityDate", target = "priorityDate")
    @Mapping(source = "reconfigureRequestTime", target = "reconfigureRequestTime")
    @Mapping(source = "lastReconfigurationTime", target = "lastReconfigurationTime")
    @Mapping(source = "terminationProcess", target = "terminationProcess")
    @Mapping(source = "executionTypeCode.executionCode", target = "executionType")
    @Mapping(source = "workTypeResource.id", target = "workTypeId")
    @Mapping(source = "workTypeResource.label", target = "workTypeLabel")
    GetTaskResponseItem mapToResponse(TaskResource taskResource);

    List<GetTaskResponseItem> mapToResponse(List<TaskResource> taskResources);

    default List<GetTaskResponseItem> mapToGetTaskItems(List<TaskResource> taskResources) {
        return mapToResponse(taskResources);
    }

}
