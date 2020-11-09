package uk.gov.hmcts.reform.wataskmanagementapi;

import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.config.GivensBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.config.RestApiActions;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationHeadersProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CaseIdGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.Assertions;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.Common;

import static com.fasterxml.jackson.databind.PropertyNamingStrategy.LOWER_CAMEL_CASE;
import static com.fasterxml.jackson.databind.PropertyNamingStrategy.SNAKE_CASE;

@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
public abstract class SpringBootFunctionalBaseTest {

    protected GivensBuilder given;
    protected Assertions assertions;
    protected Common common;
    protected CaseIdGenerator caseIdGenerator;
    protected RestApiActions restApiActions;
    protected RestApiActions camundaApiActions;
    protected static String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    @Autowired
    protected AuthorizationHeadersProvider authorizationHeadersProvider;

    @Value("${targets.camunda}")
    private String camundaUrl;
    @Value("${targets.instance}")
    private String testUrl;

    @Before
    public void setUpGivens() {
        restApiActions = new RestApiActions(testUrl, SNAKE_CASE).setUp();
        camundaApiActions = new RestApiActions(camundaUrl, LOWER_CAMEL_CASE).setUp();
        caseIdGenerator = new CaseIdGenerator();
        assertions = new Assertions(camundaApiActions, authorizationHeadersProvider);
        given = new GivensBuilder(
            camundaApiActions,
            restApiActions,
            authorizationHeadersProvider
        );
        common = new Common(caseIdGenerator, given);

    }

}
