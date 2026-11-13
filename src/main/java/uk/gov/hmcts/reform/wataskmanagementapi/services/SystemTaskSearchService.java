package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.TaskSearchConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.UnAuthorizedException;
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

    private final IdamWebApi idamWebApi;
    private final TaskSearchConfiguration taskSearchConfiguration;
    private final TaskResourceRepository taskResourceRepository;
    private final CFTTaskMapper taskMapper;

    @Transactional(readOnly = true)
    @SuppressWarnings("PMD.PreserveStackTrace")
    public List<Task> searchTasks(String authToken, String caseId) {
        UserInfo userInfo;
        try {
            userInfo = idamWebApi.userInfo(authToken);
        } catch (FeignException exception) {
            if (exception.status() == 401 || exception.status() == 403 || exception.status() == 404) {
                log.warn("IDAM user lookup rejected with status {}", exception.status(), exception);
                // A Feign cause would be handled as a downstream error by the global controller advice.
                throw new UnAuthorizedException("IDAM user not available");
            }
            throw exception;
        }
        if (userInfo == null || !StringUtils.hasText(userInfo.getUid())
            || !StringUtils.hasText(userInfo.getEmail())) {
            throw new UnAuthorizedException("IDAM user not available");
        }

        String jurisdiction = getJurisdictionForUser(userInfo.getEmail());
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
