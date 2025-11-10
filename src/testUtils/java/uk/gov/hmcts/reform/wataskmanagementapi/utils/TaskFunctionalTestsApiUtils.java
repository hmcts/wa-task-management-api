package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamService;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.CcdRetryableClient;
import uk.gov.hmcts.reform.wataskmanagementapi.config.GivensBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.config.RestApiActions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.TestAuthenticationCredentials;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationProvider;

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.LOWER_CAMEL_CASE;
import static com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE;

@Component
@Profile("functional")
public class TaskFunctionalTestsApiUtils {

    @Getter
    protected GivensBuilder given;

    @Getter
    protected Assertions assertions;

    @Getter
    protected Common common;
    @Getter
    protected RestApiActions restApiActions;
    @Getter
    protected RestApiActions camundaApiActions;

    protected RestApiActions workflowApiActions;
    protected RestApiActions launchDarklyActions;

    @Autowired
    protected CcdRetryableClient ccdRetryableClient;
    @Autowired
    protected IdamService idamService;
    @Autowired
    protected RoleAssignmentServiceApi roleAssignmentServiceApi;
    @Autowired
    protected AuthorizationProvider authorizationProvider;

    @Value("${targets.camunda}")
    private String camundaUrl;
    @Value("${targets.workflow}")
    private String workflowUrl;
    @Value("${targets.instance}")
    private String testUrl;
    @Value("${launch_darkly.url}")
    private String launchDarklyUrl;

    @PostConstruct
    public void setup() {
        restApiActions = new RestApiActions(testUrl, SNAKE_CASE).setUp();
        camundaApiActions = new RestApiActions(camundaUrl, LOWER_CAMEL_CASE).setUp();
        workflowApiActions = new RestApiActions(workflowUrl, LOWER_CAMEL_CASE).setUp();
        assertions = new Assertions(camundaApiActions, restApiActions, authorizationProvider);

        launchDarklyActions = new RestApiActions(launchDarklyUrl, LOWER_CAMEL_CASE).setUp();

        TestAuthenticationCredentials caseCreateCredentials =
            authorizationProvider.getNewWaTribunalCaseworker("wa-ft-r2-");

        given = new GivensBuilder(
            camundaApiActions,
            restApiActions,
            authorizationProvider,
            ccdRetryableClient,
            workflowApiActions,
            caseCreateCredentials
        );

        common = new Common(
            given,
            restApiActions,
            camundaApiActions,
            authorizationProvider,
            idamService,
            roleAssignmentServiceApi,
            workflowApiActions);
    }


}
