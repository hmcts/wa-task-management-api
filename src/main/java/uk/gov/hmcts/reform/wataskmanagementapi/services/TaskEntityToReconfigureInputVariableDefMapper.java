package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ReconfigureInputVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TaskEntityToReconfigureInputVariableDefMapper {
    TaskEntityToReconfigureInputVariableDefMapper INSTANCE =
        Mappers.getMapper(TaskEntityToReconfigureInputVariableDefMapper.class);

    @Mapping(source = "taskEntity.taskName", target = "name")
    @Mapping(source = "taskEntity.dueDateTime", target = "dueDate")
    @Mapping(source = "taskEntity.state", target = "taskState")
    @Mapping(source = "taskEntity.caseCategory", target = "caseManagementCategory")
    @Mapping(source = "taskEntity.workTypeResource.id", target = "workType")
    ReconfigureInputVariableDefinition map(TaskResource taskEntity);
}
