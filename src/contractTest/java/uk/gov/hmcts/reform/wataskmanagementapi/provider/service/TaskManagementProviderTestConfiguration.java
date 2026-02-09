package uk.gov.hmcts.reform.wataskmanagementapi.provider.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.jackson.JsonComponentModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.zalando.problem.jackson.ProblemModule;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.RoleAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchRequestCustomDeserializer;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTSensitiveTaskEventLogsDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.RoleAssignmentVerificationService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskAutoAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TerminationProcessHelper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.operation.TaskOperationPerformService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.utils.TaskMandatoryFieldsValidator;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.fasterxml.jackson.databind.DeserializationFeature.READ_ENUMS_USING_TO_STRING;
import static com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider.DATE_TIME_FORMAT;

@Configuration
public class TaskManagementProviderTestConfiguration {

    @MockitoBean
    private EntityManager entityManager;
    @MockitoBean
    private EntityManagerFactory entityManagerFactory;
    @MockitoBean
    private CamundaService camundaService;
    @MockitoBean
    private CFTTaskMapper cftTaskMapper;
    @MockitoBean
    private CftQueryService cftQueryService;
    @MockitoBean
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @MockitoBean
    private CFTSensitiveTaskEventLogsDatabaseService cftSensitiveTaskEventLogsDatabaseService;
    @MockitoBean
    private IdamService idamService;
    @MockitoBean
    private RoleAssignmentService roleAssignmentService;
    @MockitoBean
    private TerminationProcessHelper terminationProcessHelper;
    @MockitoBean
    private ConfigureTaskService configureTaskService;
    @MockitoBean
    private TaskAutoAssignmentService taskAutoAssignmentService;
    @MockitoBean
    private List<TaskOperationPerformService> taskOperationPerformServices;
    @MockitoBean
    private IdamTokenGenerator idamTokenGenerator;
    @MockitoBean
    private AllowedJurisdictionConfiguration allowedJurisdictionConfiguration;
    @MockitoBean
    TaskMandatoryFieldsValidator taskMandatoryFieldsValidator;
    private RoleAssignmentVerificationService roleAssignmentVerificationService;

    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        return token -> Jwt.withTokenValue(token)
            .header("alg", "none")
            .claim("sub", "contract-test")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .claims(claims -> claims.putAll(new HashMap<>()))
            .build();
    }

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
            cftQueryService,
            cftSensitiveTaskEventLogsDatabaseService);
        return new TaskManagementService(
            camundaService,
            cftTaskDatabaseService,
            cftTaskMapper,
            configureTaskService,
            taskAutoAssignmentService,
            roleAssignmentVerificationService,
            entityManager,
            idamTokenGenerator,
            cftSensitiveTaskEventLogsDatabaseService,
            taskMandatoryFieldsValidator,
            terminationProcessHelper);
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
