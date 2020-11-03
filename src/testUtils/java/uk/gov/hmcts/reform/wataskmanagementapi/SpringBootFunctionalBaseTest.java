package uk.gov.hmcts.reform.wataskmanagementapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.config.CamundaObjectMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.config.GivensBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.config.RestApiActions;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CcdIdGenerator;
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
    protected CcdIdGenerator ccdIdGenerator;
    protected RestApiActions restApiActions;
    @Value("${targets.camunda}")
    private String camundaUrl;
    @Value("${targets.instance}")
    private String testUrl;

    @Before
    public void setUpGivens() {
        restApiActions = new RestApiActions(testUrl).setUp();
        ccdIdGenerator = new CcdIdGenerator();
        assertions = new Assertions(camundaUrl);
        camundaObjectMapper = new CamundaObjectMapper(getDefaultObjectMapper(), getCamundaObjectMapper());
        given = new GivensBuilder(camundaUrl, camundaObjectMapper);
        common = new Common(ccdIdGenerator, given);

    }

    private ObjectMapper getCamundaObjectMapper() {
        return new ObjectMapper();
    }

    private ObjectMapper getDefaultObjectMapper() {
        return new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    }
}
