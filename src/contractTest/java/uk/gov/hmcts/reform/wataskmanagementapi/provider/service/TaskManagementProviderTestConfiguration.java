package uk.gov.hmcts.reform.wataskmanagementapi.provider.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.boot.jackson.JsonComponentModule;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.zalando.problem.ProblemModule;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.RoleAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchRequestCustomDeserializer;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.RoleAssignmentVerificationService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskAutoAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskOperationService;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import static com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING;
import static com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider.DATE_TIME_FORMAT;

public class TaskManagementProviderTestConfiguration {

    @MockBean
    private EntityManager entityManager;
    @MockBean
    private EntityManagerFactory entityManagerFactory;
    @MockBean
    private CamundaService camundaService;
    @MockBean
    private CFTTaskMapper cftTaskMapper;
    @MockBean
    private CftQueryService cftQueryService;
    @MockBean
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @MockBean
    private IdamService idamService;
    @MockBean
    private RoleAssignmentService roleAssignmentService;
    @MockBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    @MockBean
    private ConfigureTaskService configureTaskService;
    @MockBean
    private TaskAutoAssignmentService taskAutoAssignmentService;
    @MockBean
    private List<TaskOperationService> taskOperationServices;
    @MockBean
    private IdamTokenGenerator idamTokenGenerator;
    @MockBean
    private AllowedJurisdictionConfiguration allowedJurisdictionConfiguration;
    private RoleAssignmentVerificationService roleAssignmentVerificationService;

    @Bean
    @Primary
    public SearchRequestCustomDeserializer searchRequestCustomDeserializer() {
        return new SearchRequestCustomDeserializer();
    }

    @Bean
    @Primary
    public SystemDateProvider systemDateProvider() {
        return new SystemDateProvider();
    }

    @Bean
    @Primary
    public TaskManagementService taskManagementService() {
        roleAssignmentVerificationService = new RoleAssignmentVerificationService(
            cftTaskDatabaseService,
            cftQueryService
        );
        return new TaskManagementService(
            camundaService,
            cftTaskDatabaseService,
            cftTaskMapper,
            launchDarklyFeatureFlagProvider,
            configureTaskService,
            taskAutoAssignmentService,
            roleAssignmentVerificationService,
            taskOperationServices,
            entityManager,
            idamTokenGenerator
        );
    }

    @Bean
    @Primary
    public AccessControlService accessControlService() {
        return new AccessControlService(
            idamService,
            roleAssignmentService
        );
    }

    @Bean
    @Primary
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        SearchRequestCustomDeserializer searchRequestCustomDeserializer = new SearchRequestCustomDeserializer();
        // Set default date to RFC3339 standards
        SimpleDateFormat df = new SimpleDateFormat(DATE_TIME_FORMAT, Locale.ENGLISH);
        SimpleModule module = new JsonComponentModule();
        module.addDeserializer(SearchParameter.class, searchRequestCustomDeserializer);

        return new Jackson2ObjectMapperBuilder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .serializationInclusion(JsonInclude.Include.NON_ABSENT)
            .featuresToEnable(READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .featuresToEnable(READ_ENUMS_USING_TO_STRING)
            .featuresToEnable(WRITE_ENUMS_USING_TO_STRING)
            .dateFormat(df)
            .modules(
                new ParameterNamesModule(),
                new JavaTimeModule(),
                new Jdk8Module(),
                new ProblemModule(),
                module
            );
    }

    @Bean
    @Primary
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        Jackson2ObjectMapperBuilder builder = jackson2ObjectMapperBuilder();
        return new MappingJackson2HttpMessageConverter(builder.build());
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        Jackson2ObjectMapperBuilder builder = jackson2ObjectMapperBuilder();
        return builder.createXmlMapper(false).build();
    }
}
