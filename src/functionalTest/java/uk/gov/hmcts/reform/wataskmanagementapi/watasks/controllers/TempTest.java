package uk.gov.hmcts.reform.wataskmanagementapi.watasks.controllers;

import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootFunctionalBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.TestAuthenticationCredentials;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TempTest extends SpringBootFunctionalBaseTest {
    private TestAuthenticationCredentials allCredentials;
    private TestAuthenticationCredentials baseCredentials;
    private TestAuthenticationCredentials creditCredentials;
    private TestAuthenticationCredentials doubleCredentials;


    @Before
    public void setUp() {
        allCredentials = authorizationProvider
            .getNewTribunalCaseworker("wa-granular-permission-caseofficer@fake.hmcts.net",
                                      "WaCaseOfficerGp",
                                      "case worker");

        common.setupWAOrganisationalRoleAssignment(allCredentials.getHeaders(), "case-allocator");
        common.setupWAOrganisationalRoleAssignment(allCredentials.getHeaders(), "task-supervisor");
        common.setupWAOrganisationalRoleAssignment(allCredentials.getHeaders(), "tribunal-caseworker");

        baseCredentials = authorizationProvider
            .getNewTribunalCaseworker("wa-granular-permission-tribunal-caseofficer@fake.hmcts.net",
                                      "WaTribunalCaseOfficer",
                                      "case worker");
        common.setupWAOrganisationalRoleAssignment(baseCredentials.getHeaders(), "tribunal-caseworker");

        creditCredentials = authorizationProvider
            .getNewTribunalCaseworker("wa-granular-permission-senior-tribunal-caseofficer@fake.hmcts.net",
                                      "WaSeniorTribunalCaseOfficer",
                                      "case worker");
        common.setupWAOrganisationalRoleAssignment(creditCredentials.getHeaders(), "senior-tribunal-caseworker");

        doubleCredentials = authorizationProvider
            .getNewTribunalCaseworker("wa-granular-permission-task-supervisor@fake.hmcts.net",
                                      "WaTaskSupervisor",
                                      "case worker");
        common.setupWAOrganisationalRoleAssignment(doubleCredentials.getHeaders(), "task-supervisor");

    }

    @Test
    public void should_return_a_200_with_task_and_correct_properties() {
        assertNotNull(allCredentials);
        assertNotNull(baseCredentials);
        assertNotNull(creditCredentials);
        assertNotNull(doubleCredentials);
    }

}
