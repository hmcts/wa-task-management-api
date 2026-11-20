package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
@Slf4j
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
        Map<String, String> jurisdictionUsers = taskSearchConfiguration.getJurisdictionUsers();
        log.info("Resolving task search jurisdiction: username={}, usernameLength={}, configuredJurisdictions={}",
                 StringUtils.overlay(username, "***", 1, StringUtils.length(username)),
                 StringUtils.length(username), jurisdictionUsers.keySet());

        for (Map.Entry<String, String> jurisdictionUser : jurisdictionUsers.entrySet()) {
            String configuredUsername = jurisdictionUser.getValue();
            boolean matches = username.equals(configuredUsername);
            log.debug("Task search mapping: jurisdiction={}, configuredUsername={}, configuredUsernamePresent={}, "
                          + "configuredUsernameLength={}, matches={}",
                      jurisdictionUser.getKey(),
                      StringUtils.overlay(configuredUsername, "***", 1, StringUtils.length(configuredUsername)),
                      StringUtils.isNotBlank(configuredUsername), StringUtils.length(configuredUsername), matches);

            if (matches) {
                log.info("Task search user matched jurisdiction={}", jurisdictionUser.getKey());
                return jurisdictionUser.getKey();
            }
        }
        log.warn("Task search user has no matching jurisdiction; configuredJurisdictions={}",
                 jurisdictionUsers.keySet());
        throw new GenericForbiddenException(USER_HAS_NO_JURISDICTION);
    }
}
