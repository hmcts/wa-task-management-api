package uk.gov.hmcts.reform.wataskmanagementapi.cft.cleanupsensitivedata;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.SensitiveTaskEventLogsRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTSensitiveTaskEventLogsDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;

import java.time.LocalDateTime;

@ActiveProfiles("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Sql("/scripts/cleanup/data.sql")
@Slf4j
public class CleanUpSensitiveLogsDataTest {

    @Autowired
    TaskResourceRepository taskResourceRepository;

    @Autowired
    private SensitiveTaskEventLogsRepository sensitiveTaskEventLogsRepository;


    private CFTTaskDatabaseService cftTaskDatabaseService;

    private CFTSensitiveTaskEventLogsDatabaseService cftSensitiveTaskEventLogsDatabaseService;

    @BeforeEach
    void setUp() {
        CFTTaskMapper cftTaskMapper = new CFTTaskMapper(new ObjectMapper());
        cftTaskDatabaseService = new CFTTaskDatabaseService(taskResourceRepository, cftTaskMapper);

        cftSensitiveTaskEventLogsDatabaseService = new CFTSensitiveTaskEventLogsDatabaseService(
            sensitiveTaskEventLogsRepository,
            cftTaskDatabaseService
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

}
