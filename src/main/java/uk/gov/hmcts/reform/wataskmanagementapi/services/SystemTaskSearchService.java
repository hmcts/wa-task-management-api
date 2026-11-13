package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wataskmanagementapi.config.TaskSearchConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericForbiddenException;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptySet;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.USER_HAS_NO_JURISDICTION;

@Service
@RequiredArgsConstructor
public class SystemTaskSearchService {

    private final TaskSearchConfiguration taskSearchConfiguration;
    private final TaskResourceRepository taskResourceRepository;
    private final CFTTaskMapper taskMapper;

    @Transactional(readOnly = true)
    public List<Task> searchTasks(String username, String caseId) {
        String jurisdiction = getJurisdictionForUser(username);
        List<TaskResource> taskResources = taskResourceRepository.findByCaseIdAndJurisdictionIgnoreCase(
            caseId, jurisdiction);
        List<Task> tasks = new ArrayList<>();
        for (TaskResource taskResource : taskResources) {
            tasks.add(taskMapper.mapToTaskWithPermissions(taskResource, emptySet()));
        }
        return tasks;
    }

    private String getJurisdictionForUser(String username) {
        for (Map.Entry<String, String> jurisdictionUser : taskSearchConfiguration.getJurisdictionUsers().entrySet()) {
            if (username.equals(jurisdictionUser.getValue())) {
                return jurisdictionUser.getKey();
            }
        }
        throw new GenericForbiddenException(USER_HAS_NO_JURISDICTION);
    }
}
