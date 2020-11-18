package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.Builder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.RoleAssignmentService;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PermissionCheckServiceTest {

    public static final String BEARER_USER_TOKEN = "Bearer user token";
    @Mock
    private IdamService idamService;
    @Mock
    private PermissionEvaluatorService permissionEvaluatorService;
    @Mock
    private RoleAssignmentService roleAssignmentService;
    @Mock
    private CamundaService camundaService;

    @InjectMocks
    private PermissionCheckService permissionCheckService;

    @BeforeEach
    void setUp() {
        when(idamService.getUserId(BEARER_USER_TOKEN)).thenReturn("user id");
        when(camundaService.performGetVariablesAction("task id"))
            .thenReturn(Collections.emptyMap()); // any map is valid since we are not testing this service here
        when(roleAssignmentService.getRolesForUser("user id", BEARER_USER_TOKEN))
            .thenReturn(Collections.emptyList()); // idem as per above comment
        when(roleAssignmentService.getRolesForUser("assignee id", BEARER_USER_TOKEN))
            .thenReturn(Collections.emptyList()); // idem as per above comment
    }

    @ParameterizedTest(name = "Scenario {argumentsWithNames}")
    @MethodSource("provideScenario")
    void validate(Scenario scenario) {
        when(permissionEvaluatorService.hasAccess(
            anyMap(),
            anyList(),
            eq(Collections.singletonList(PermissionTypes.MANAGE))
        )).thenReturn(scenario.userHasAccess);

        when(permissionEvaluatorService.hasAccess(
            anyMap(),
            anyList(),
            eq(List.of(PermissionTypes.OWN, PermissionTypes.EXECUTE))
        )).thenReturn(scenario.assigneeHasAccess);

        boolean actualValidation = permissionCheckService.validate(
            BEARER_USER_TOKEN,
            "task id",
            "assignee id"
        );

        assertEquals(scenario.expectedValidation, actualValidation);
    }

    private static Stream<Scenario> provideScenario() {
        Scenario validateIsTrue = Scenario.builder()
            .userHasAccess(true)
            .assigneeHasAccess(true)
            .expectedValidation(true)
            .build();

        Scenario userValidationIsFalse = Scenario.builder()
            .userHasAccess(false)
            .assigneeHasAccess(true)
            .expectedValidation(false)
            .build();

        Scenario assigneeValidationIsFalse = Scenario.builder()
            .userHasAccess(true)
            .assigneeHasAccess(false)
            .expectedValidation(false)
            .build();

        Scenario bothValidationsAreFalse = Scenario.builder()
            .userHasAccess(false)
            .assigneeHasAccess(false)
            .expectedValidation(false)
            .build();

        return Stream.of(validateIsTrue, userValidationIsFalse, assigneeValidationIsFalse, bothValidationsAreFalse);
    }

    @Builder
    static class Scenario {
        boolean userHasAccess;
        boolean assigneeHasAccess;
        boolean expectedValidation;
    }
}
