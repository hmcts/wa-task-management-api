package uk.gov.hmcts.reform.wataskmanagementapi;

import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.config.GivensBuilder;

@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@ActiveProfiles("functional")
public abstract class SpringBootFunctionalBaseTest {

    protected GivensBuilder given;

    @Value("${targets.camunda}")
    private String camundaUrl;

    @Before
    public void setUpGivens() {
        given = new GivensBuilder(camundaUrl);
    }
}
