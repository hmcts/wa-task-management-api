package uk.gov.hmcts.reform.wataskmanagementapi.auth.permission;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@ExtendWith(MockitoExtension.class)
class PermissionEvaluatorServiceTest {

    private PermissionEvaluatorService permissionEvaluatorService;

    @BeforeEach
    public void setUp() {
        permissionEvaluatorService = new PermissionEvaluatorService(new CamundaObjectMapper());
    }

    @Test
    void hasAccess_should_succeed_when_looking_for_read_permission_and_return_true() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);

        List<String> rolesToTest = asList("tribunal-caseworker", "senior-tribunal-caseworker");

        Map<String, CamundaVariable> mockedVariables = mockVariables();
        mockedVariables.put(
            "tribunal-caseworker", new CamundaVariable("Read,Refer,Own,Manage,Cancel", "String"));
        mockedVariables.put(
            "senior-tribunal-caseworker", new CamundaVariable("Read,Refer,Own,Manage,Cancel", "String")
        );

        rolesToTest.forEach(role -> {
            boolean result = permissionEvaluatorService.hasAccess(
                mockedVariables,
                new HashSet<>(singletonList(role)),
                permissionsRequired
            );

            assertTrue(result);
        });
    }

    @Test
    void hasAccess_should_fail_when_looking_for_read_permission_and_return_false() {

        List<PermissionTypes> permissionsRequired = singletonList(PermissionTypes.READ);

        List<String> rolesToTest = asList("tribunal-caseworker", "senior-tribunal-caseworker");

        Map<String, CamundaVariable> mockedVariables = mockVariables();
        mockedVariables.put(
            "tribunal-caseworker", new CamundaVariable("Refer,Own,Manage,Cancel", "String"));
        mockedVariables.put(
            "senior-tribunal-caseworker", new CamundaVariable("Refer,Own,Manage,Cancel", "String")
        );

        rolesToTest.forEach(role -> {
            boolean result = permissionEvaluatorService.hasAccess(
                mockedVariables,
                new HashSet<>(singletonList(role)),
                permissionsRequired
            );

            assertFalse(result);
        });
    }


    private Map<String, CamundaVariable> mockVariables() {

        Map<String, CamundaVariable> variables = new HashMap<>();
        variables.put("ccdId", new CamundaVariable("00000", "String"));
        variables.put("caseName", new CamundaVariable("someCaseName", "String"));
        variables.put("caseType", new CamundaVariable("someCaseType", "String"));
        variables.put("taskState", new CamundaVariable("configured", "String"));
        variables.put("location", new CamundaVariable("someStaffLocationId", "String"));
        variables.put("locationName", new CamundaVariable("someStaffLocationName", "String"));

        return variables;
    }
}
