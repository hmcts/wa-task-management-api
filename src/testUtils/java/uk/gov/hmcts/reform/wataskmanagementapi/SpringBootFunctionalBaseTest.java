package uk.gov.hmcts.reform.wataskmanagementapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.CamundaObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.config.GivensBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.config.RestApiActions;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationHeadersProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CaseIdGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.Assertions;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.Common;

@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
public abstract class SpringBootFunctionalBaseTest {

    protected GivensBuilder given;
    protected CamundaObjectMapper camundaObjectMapper;
    protected Assertions assertions;
    protected Common common;
    protected CaseIdGenerator caseIdGenerator;
    protected RestApiActions restApiActions;

    @Autowired
    protected IdamServiceApi idamServiceApi;
    @Autowired
    protected RoleAssignmentServiceApi roleAssignmentServiceApi;

    @Autowired
    protected AuthorizationHeadersProvider authorizationHeadersProvider;

    @Value("${targets.camunda}")
    private String camundaUrl;
    @Value("${targets.instance}")
    private String testUrl;

    @Before
    public void setUpGivens() {
        restApiActions = new RestApiActions(testUrl).setUp();
        caseIdGenerator = new CaseIdGenerator();
        assertions = new Assertions(camundaUrl);
        camundaObjectMapper = new CamundaObjectMapper(getDefaultObjectMapper(), getCamundaObjectMapper());
        given = new GivensBuilder(
            camundaUrl,
            camundaObjectMapper,
            idamServiceApi,
            roleAssignmentServiceApi,
            authorizationHeadersProvider
        );
        common = new Common(caseIdGenerator, given);

    }

    private ObjectMapper getCamundaObjectMapper() {
        return new ObjectMapper();
    }

    private ObjectMapper getDefaultObjectMapper() {
        return new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    }
}
