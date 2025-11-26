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
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.WorkTypesController;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.WorkType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        workTypes.add(new WorkType("upper_tribunal", "Upper Tribunal"));
        workTypes.add(new WorkType("routine_work", "Routine work"));
        workTypes.add(new WorkType("decision_making_work", "Decision-making work"));
        workTypes.add(new WorkType("applications", "Applications"));
        workTypes.add(new WorkType("priority", "Priority"));
        workTypes.add(new WorkType("access_requests", "Access requests"));
        workTypes.add(new WorkType("error_management", "Error management"));
        workTypes.add(new WorkType("review_case", "Review Case"));
        workTypes.add(new WorkType("evidence", "Evidence"));
        workTypes.add(new WorkType("follow_up", "Follow Up"));
        workTypes.add(new WorkType("pre_hearing", "Pre-Hearing"));
        workTypes.add(new WorkType("post_hearing", "Post-Hearing"));
        workTypes.add(new WorkType("intermediate_track_hearing_work", "Intermediate track hearing work"));
        workTypes.add(new WorkType("multi_track_hearing_work", "Multi track hearing work"));
        workTypes.add(new WorkType("intermediate_track_decision_making_work",
                                   "Intermediate track decision making work"));
        workTypes.add(new WorkType("multi_track_decision_making_work", "Multi track decision making work"));
        workTypes.add(new WorkType("query_work", "Query work"));
        workTypes.add(new WorkType("welsh_translation_work", "Welsh translation work"));
        workTypes.add(new WorkType("bail_work", "Bail work"));
        workTypes.add(new WorkType("stf_24w_hearing_work", "Hearing Work - STF"));
        workTypes.add(new WorkType("stf_24w_routine_work", "Routine Work - STF"));
        workTypes.add(new WorkType("stf_24w_decision_making_work", "Decision Making Work - STF"));
        workTypes.add(new WorkType("stf_24w_applications", "Applications - STF"));
        workTypes.add(new WorkType("stf_24w_upper_tribunal", "Upper Tribunal - STF"));
        workTypes.add(new WorkType("stf_24w_access_requests", "Access Requests - STF"));
        workTypes.add(new WorkType("stf_24w_review_case", "Review Case - STF"));

        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));
        UserInfo userInfo = mock((UserInfo.class));
        when(userInfo.getUid()).thenReturn("someUserId");
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
        when(accessControlService.getRoles(anyString())).thenReturn(accessControlResponse);
        when(workTypesService.getAllWorkTypes()).thenReturn(workTypes);
    }

    @State({"retrieve work types by userId"})
    public void workTypesByUserId() {
        List<WorkType> workTypes = new ArrayList<>();
        final String hearingWork = "hearing_work";
        workTypes.add(new WorkType(hearingWork, "Hearing Work"));

        Map<String, String> attributes = Map.of(
            WORK_TYPES.value(), hearingWork
        );

        List<RoleAssignment> roleAssignmentList = List.of(
            new RoleAssignment(ActorIdType.IDAM, "actorId", RoleType.CASE, "roleName",
                Classification.RESTRICTED, GrantType.SPECIFIC, RoleCategory.JUDICIAL, false, attributes)
        );
        AccessControlResponse accessControlResponse = mock((AccessControlResponse.class));
        UserInfo userInfo = mock((UserInfo.class));
        when(userInfo.getUid()).thenReturn("someUserId");
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
        when(accessControlService.getRoles(anyString())).thenReturn(accessControlResponse);
        when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignmentList);
        when(workTypesService.getWorkTypes(any())).thenReturn(workTypes);
    }

}
