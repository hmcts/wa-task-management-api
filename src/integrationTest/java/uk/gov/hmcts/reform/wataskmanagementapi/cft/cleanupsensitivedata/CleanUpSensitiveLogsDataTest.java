package uk.gov.hmcts.reform.wataskmanagementapi.cft.cleanupsensitivedata;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.wataskmanagementapi.config.executors.ExecutorServiceConfig;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.SensitiveTaskEventLog;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.SensitiveTaskEventLogsRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTSensitiveTaskEventLogsDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;

@ActiveProfiles("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ExecutorServiceConfig.class)
@Testcontainers
@Sql("/scripts/cleanup/data.sql")
@Transactional
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Slf4j
public class CleanUpSensitiveLogsDataTest {

    @Autowired
    TaskResourceRepository taskResourceRepository;

    @Autowired
    private ExecutorService sensitiveTaskEventLogsExecutorService;

    @Autowired
    private SensitiveTaskEventLogsRepository sensitiveTaskEventLogsRepository;

    private CFTTaskDatabaseService cftTaskDatabaseService;

    private CFTSensitiveTaskEventLogsDatabaseService cftSensitiveTaskEventLogsDatabaseService;

    @AfterEach
    void tearDown() {
        sensitiveTaskEventLogsRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        CFTTaskMapper cftTaskMapper = new CFTTaskMapper(new ObjectMapper());
        cftTaskDatabaseService = new CFTTaskDatabaseService(taskResourceRepository, cftTaskMapper);

        cftSensitiveTaskEventLogsDatabaseService = new CFTSensitiveTaskEventLogsDatabaseService(
            sensitiveTaskEventLogsRepository,
            cftTaskDatabaseService,
            sensitiveTaskEventLogsExecutorService
        );
    }

    @Test
    void should_clean_tasks_according_to_expiry_date() {

        long totalCount = sensitiveTaskEventLogsRepository.count();
        Assertions.assertThat(totalCount).isEqualTo(4);

        int removedRecords = cftSensitiveTaskEventLogsDatabaseService.cleanUpSensitiveLogs(LocalDateTime.now());
        Assertions.assertThat(removedRecords).isEqualTo(3);

        long remainingCount = sensitiveTaskEventLogsRepository.count();
        Assertions.assertThat(remainingCount).isEqualTo(1);
    }

    @Test
    void should_not_clean_up_tasks_where_expiry_time_is_greater_than_current_time() {

        LocalDateTime jobStartTime = LocalDateTime.now();
        cftSensitiveTaskEventLogsDatabaseService.cleanUpSensitiveLogs(jobStartTime);

        List<SensitiveTaskEventLog> sensitiveTaskEventLogList =
            (List<SensitiveTaskEventLog>) sensitiveTaskEventLogsRepository.findAll();

        Assertions.assertThat(sensitiveTaskEventLogList).isNotEmpty();

        Assertions.assertThat(sensitiveTaskEventLogList.get(0).getExpiryTime().toLocalDateTime())
            .isAfter(jobStartTime);

    }

}
