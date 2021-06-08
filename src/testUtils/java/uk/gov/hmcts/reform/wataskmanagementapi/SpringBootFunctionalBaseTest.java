package uk.gov.hmcts.reform.wataskmanagementapi;

import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamService;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.GivensBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.config.RestApiActions;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationHeadersProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.Assertions;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.Common;

import static com.fasterxml.jackson.databind.PropertyNamingStrategy.LOWER_CAMEL_CASE;
import static com.fasterxml.jackson.databind.PropertyNamingStrategy.SNAKE_CASE;

@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
public abstract class SpringBootFunctionalBaseTest {

    public static final String LOG_MSG_THERE_WAS_A_PROBLEM_FETCHING_THE_VARIABLES_FOR_TASK =
        "There was a problem fetching the variables for task with id: %s";
    protected GivensBuilder given;
    protected Assertions assertions;
    protected Common common;
    protected RestApiActions restApiActions;
    protected RestApiActions camundaApiActions;
    @Autowired
    protected AuthorizationHeadersProvider authorizationHeadersProvider;

    @Autowired
    protected CoreCaseDataApi coreCaseDataApi;

    @Autowired
    protected IdamService idamService;

    @Autowired
    protected RoleAssignmentServiceApi roleAssignmentServiceApi;

    @Value("${targets.camunda}")
    private String camundaUrl;
    @Value("${targets.instance}")
    private String testUrl;
    @Value("${targets.documentStoreUrl}")
    private String documentStoreUrl;

    @Before
    public void setUpGivens() {
        restApiActions = new RestApiActions(testUrl, SNAKE_CASE).setUp();
        camundaApiActions = new RestApiActions(camundaUrl, LOWER_CAMEL_CASE).setUp();
        assertions = new Assertions(camundaApiActions, authorizationHeadersProvider);
        given = new GivensBuilder(
            documentStoreUrl,
            camundaApiActions,
            restApiActions,
            authorizationHeadersProvider,
            coreCaseDataApi
        );
        common = new Common(
            given,
            camundaApiActions,
            authorizationHeadersProvider,
            idamService,
            roleAssignmentServiceApi
        );

    }

}
