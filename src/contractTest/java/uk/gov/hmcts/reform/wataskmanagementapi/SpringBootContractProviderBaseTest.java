package uk.gov.hmcts.reform.wataskmanagementapi;

import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.provider.service.TaskManagementProviderTestConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskDeletionService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskOperationService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskTypesService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.WorkTypesService;

import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

@ExtendWith(SpringExtension.class)
//Uncomment @PactFolder and comment the @PactBroker line to test local consumer.
//using this, import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
//@PactFolder("target/pacts")
@PactBroker(
    url = "${PACT_BROKER_FULL_URL:http://localhost:9292}",
    consumerVersionSelectors = {
        @VersionSelector(tag = "master")}
)
@ContextConfiguration(classes = {EntityManager.class, EntityManagerFactory.class})
@Import(TaskManagementProviderTestConfiguration.class)
@IgnoreNoPactsToVerify
public class SpringBootContractProviderBaseTest {

    @Mock
    protected AccessControlService accessControlService;

    @Mock
    protected ClientAccessControlService clientAccessControlService;

    @Mock
    protected TaskManagementService taskManagementService;

    @Mock
    protected TaskOperationService taskOperationService;

    @Mock
    protected TaskDeletionService taskDeletionService;

    @Mock
    protected CftQueryService cftQueryService;

    @Mock
    protected LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    @Mock
    protected WorkTypesService workTypesService;

    @Mock
    protected TaskTypesService taskTypesService;

    @Mock
    protected CFTTaskDatabaseService cftTaskDatabaseService;

    @Autowired
    protected MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected SystemDateProvider systemDateProvider;

    protected Map<String, String> getAdditionalProperties() {

        return Map.of(
            "name1", "value1",
            "name2", "value2",
            "name3", "value3"
        );

    }

}
