package uk.gov.hmcts.reform.wataskmanagementapi.provider.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.RoleAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaQueryBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;

import java.text.SimpleDateFormat;
import java.util.Locale;

import static uk.gov.hmcts.reform.wataskmanagementapi.services.SystemDateProvider.DATE_TIME_FORMAT;

public class TaskManagementProviderTestConfiguration {

    @MockBean
    private CamundaService camundaService;
    @MockBean
    private CamundaQueryBuilder camundaQueryBuilder;
    @MockBean
    private PermissionEvaluatorService permissionEvaluatorService;
    @MockBean
    private CFTTaskMapper cftTaskMapper;
    @MockBean
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @MockBean
    private IdamService idamService;
    @MockBean
    private RoleAssignmentService roleAssignmentService;
    @MockBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    @Bean
    @Primary
    public SystemDateProvider systemDateProvider() {
        return new SystemDateProvider();
    }

    @Bean
    @Primary
    public TaskManagementService taskManagementService() {
        return new TaskManagementService(
            camundaService,
            camundaQueryBuilder,
            permissionEvaluatorService,
            cftTaskDatabaseService,
            cftTaskMapper,
            launchDarklyFeatureFlagProvider
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
        return new Jackson2ObjectMapperBuilder()
            .serializationInclusion(JsonInclude.Include.NON_ABSENT)
            .propertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .modules(
                new ParameterNamesModule(),
                new JavaTimeModule(),
                new Jdk8Module()
            );
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true);
        // Set default date to RFC3339 standards
        SimpleDateFormat df = new SimpleDateFormat(DATE_TIME_FORMAT, Locale.ENGLISH);
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        objectMapper.setDateFormat(df);
        objectMapper.registerModule(new Jdk8Module());
        return objectMapper;
    }
}
