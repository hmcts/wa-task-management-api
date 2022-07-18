package uk.gov.hmcts.reform.wataskmanagementapi.provider;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootContractProviderBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.WorkTypesController;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WorkType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition.WORK_TYPES;

@Provider("wa_task_management_api_get_work_types")
public class WorkTypeProviderTest extends SpringBootContractProviderBaseTest {

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    @BeforeEach
    void beforeCreate(PactVerificationContext context) {
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        testTarget.setControllers(new WorkTypesController(
            accessControlService,
            workTypesService
        ));
        if (context != null) {
            context.setTarget(testTarget);
        }
    }

    @State({"retrieve all work types"})
    public void allWorkTypes() {
        List<WorkType> workTypes = new ArrayList<>();
        workTypes.add(new WorkType("hearing_work", "Hearing Work"));

        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));
        when(accessControlService.getRoles(anyString())).thenReturn(accessControlResponse);
        when(workTypesService.getAllWorkTypes()).thenReturn(workTypes);
    }

    @State({"retrieve work types by userId"})
    public void workTypesByUserId() {
        List<WorkType> workTypes = new ArrayList<>();
        workTypes.add(new WorkType("hearing_work", "Hearing Work"));

        Map<String, String> attributes = Map.of(
            WORK_TYPES.value(), "hearing_work"
        );

        List<RoleAssignment> roleAssignmentList = List.of(
            new RoleAssignment(ActorIdType.IDAM, "actorId", RoleType.CASE, "roleName",
                Classification.RESTRICTED, GrantType.SPECIFIC, RoleCategory.JUDICIAL, false, attributes)
        );
        final Optional<WorkType> workType = Optional.of(new WorkType("hearing_work", "Hearing Work"));
        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));
        when(accessControlService.getRoles(anyString())).thenReturn(accessControlResponse);
        when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignmentList);
        when(workTypesService.getWorkTypes(any())).thenReturn(Collections.singletonList(workType.get()));
    }

}
