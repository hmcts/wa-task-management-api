package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirements;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.RoleAssignmentVerificationException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.SensitiveTaskEventLogsRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RoleAssignmentVerificationServiceTest extends SpringBootIntegrationBaseTest {

    @Autowired
    SensitiveTaskEventLogsRepository sensitiveTaskEventLogsRepository;
    @MockBean
    CFTTaskDatabaseService cftTaskDatabaseService;
    @MockBean
    CftQueryService cftQueryService;
    @MockBean
    PermissionRequirements permissionsRequired;
    @MockBean
    CFTSensitiveTaskEventLogsDatabaseService cftSensitiveTaskEventLogsDatabaseService;

    RoleAssignmentVerificationService roleAssignmentVerificationService;

    @BeforeEach
    void setUp() {
        roleAssignmentVerificationService = new RoleAssignmentVerificationService(cftTaskDatabaseService,
            cftQueryService,
            cftSensitiveTaskEventLogsDatabaseService);
    }

    @Test
    void should_save_sensitive_task_event_log_and_return_403_wih_no_cancel_permission() {
        String taskId = UUID.randomUUID().toString();
        String caseId = "Some caseId";
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(java.util.Optional.of(caseId));

        assertThatThrownBy(() -> roleAssignmentVerificationService.verifyRoleAssignments(taskId,
            roleAssignments,
            permissionsRequired,
            ErrorMessages.ROLE_ASSIGNMENT_VERIFICATIONS_FAILED_ASSIGNEE)

        ).isInstanceOf(RoleAssignmentVerificationException.class)
            .hasNoCause()
            .hasMessage("Role Assignment Verification: "
                        + "The user being assigned the Task has failed the Role Assignment checks performed.");

        verify(cftSensitiveTaskEventLogsDatabaseService, times(1))
            .processSensitiveTaskEventLog(taskId,
                roleAssignments,
                ErrorMessages.ROLE_ASSIGNMENT_VERIFICATIONS_FAILED_ASSIGNEE);

    }
}
