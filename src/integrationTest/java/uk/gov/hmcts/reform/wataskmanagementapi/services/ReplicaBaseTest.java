package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.JdbcDatabaseContainer;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.ReportableTaskRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.SubscriptionCreator;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskAssignmentsRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskHistoryResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.db.TCExtendedContainerDatabaseDriver;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@Slf4j
public abstract class ReplicaBaseTest extends SpringBootIntegrationBaseTest {
    protected static final String TEST_REPLICA_DB_USER = "repl_user";
    protected static final String TEST_REPLICA_DB_PASS = "repl_user";

    protected static final String TEST_PRIMARY_DB_USER = "wa_user";
    protected static final String TEST_PRIMARY_DB_PASS = "wa_password";
    protected static final String TEST_PUBLICATION_URL = "postgresql://cft_task_db:5432";
    protected static final String ENVIRONMENT = "local-arm-arch";

    @Autowired
    protected TaskResourceRepository taskResourceRepository;

    @Autowired
    protected TaskHistoryResourceRepository taskHistoryResourceRepository;
    @Autowired
    protected ReportableTaskRepository reportableTaskRepository;
    @Autowired
    protected TaskAssignmentsRepository taskAssignmentsRepository;

    @Value("${spring.datasource.jdbcUrl}")
    protected String primaryJdbcUrl;

    @Value("${spring.datasource-replica.jdbcUrl}")
    private String replicaJdbcUrl;

    protected MIReportingService miReportingServiceForTest;

    protected SubscriptionCreator subscriptionCreatorForTest;

    @Autowired
    protected MIReportingService miReportingService;


    protected JdbcDatabaseContainer container;
    protected JdbcDatabaseContainer containerReplica;

    @BeforeEach
    void setUp() {
        //Logical Replication is a pre-requisite for all tests here
        waitForReplication();

        subscriptionCreatorForTest = new SubscriptionCreator(
            TEST_REPLICA_DB_USER,
            TEST_REPLICA_DB_PASS,
            TEST_PRIMARY_DB_USER,
            TEST_PRIMARY_DB_PASS,
            TEST_PUBLICATION_URL,
            ENVIRONMENT);

        miReportingServiceForTest = new MIReportingService(
            taskHistoryResourceRepository,
            taskResourceRepository,
            reportableTaskRepository,
            taskAssignmentsRepository,
            subscriptionCreatorForTest);

        container = TCExtendedContainerDatabaseDriver.getContainer(primaryJdbcUrl);
        containerReplica = TCExtendedContainerDatabaseDriver.getContainer(replicaJdbcUrl);

        Testcontainers.exposeHostPorts(container.getFirstMappedPort(), containerReplica.getFirstMappedPort());

        log.info("Primary DB port: {}, Replica DB port: {}",
            container.getFirstMappedPort(),
            containerReplica.getFirstMappedPort());

    }

    private boolean waitForReplication() {

        await().ignoreException(AssertionFailedError.class)
            .atLeast(1, SECONDS)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(() -> miReportingService.hasReplicationStarted());
        return true;
    }

    @AfterAll
    void tearDown() {
        container.stop();
        containerReplica.stop();
    }

    void callMarkReportTasksForRefresh(List<String> caseIdList, List<String> taskIdList, String jurisdiction,
                                               String caseTypeId, List<String> stateList,
                                               OffsetDateTime createdBefore) {

        log.info(String.valueOf(Timestamp.valueOf(createdBefore.toLocalDateTime())));
        String runFunction = " call cft_task_db.mark_report_tasks_for_refresh( ?,?,?,?,?,? ) ";

        try (Connection conn = DriverManager.getConnection(
            containerReplica.getJdbcUrl(), containerReplica.getUsername(), containerReplica.getPassword());
             PreparedStatement preparedStatement = conn.prepareStatement(runFunction)) {

            preparedStatement.setArray(1, conn.createArrayOf("TEXT",
                                                             Objects.isNull(taskIdList) ? null : taskIdList.toArray()));
            preparedStatement.setArray(2, conn.createArrayOf("TEXT",
                                                             Objects.isNull(caseIdList) ? null : caseIdList.toArray()));
            preparedStatement.setString(3, jurisdiction);
            preparedStatement.setString(4, caseTypeId);
            preparedStatement.setArray(5, conn.createArrayOf("TEXT",
                                                             Objects.isNull(stateList) ? null : stateList.toArray()));
            preparedStatement.setTimestamp(6, Timestamp.valueOf(createdBefore.toLocalDateTime()));

            preparedStatement.execute();

        } catch (SQLException e) {
            log.error("Procedure call callMarkReportTasksForRefresh failed with SQL State : {}, {} ",
                      e.getSQLState(), e.getMessage());
        } catch (Exception e) {
            log.error("Procedure call callMarkReportTasksForRefresh failed with SQL State : {}, {} ",
                      e.getCause(), e.getMessage());
        }
    }

    List<Timestamp> callGetReportRefreshRequestTimes(List<String> taskIdList) {

        String runFunction = "{ ? = call cft_task_db.get_report_refresh_request_times( ? ) }";

        try (Connection conn = DriverManager.getConnection(
            containerReplica.getJdbcUrl(), containerReplica.getUsername(), containerReplica.getPassword());
             CallableStatement callableStatement = conn.prepareCall(runFunction)) {

            Array taskIds = conn.createArrayOf("TEXT", taskIdList.toArray());

            callableStatement.registerOutParameter(1, Types.ARRAY);
            callableStatement.setArray(2, taskIds);

            callableStatement.execute();
            Array reportRefreshRequestTimes = callableStatement.getArray(1);
            log.info(reportRefreshRequestTimes.toString());
            Timestamp[] stringReportRequestTimes = (Timestamp[])reportRefreshRequestTimes.getArray();
            return Arrays.stream(stringReportRequestTimes).filter(Objects::nonNull).toList();

        } catch (SQLException e) {
            log.error("Procedure call callGetReportRefreshRequestTimes failed with SQL State : {}, {} ",
                      e.getSQLState(), e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Procedure call callGetReportRefreshRequestTimes failed with SQL State : {}, {} ",
                      e.getCause(), e.getMessage());
            return Collections.emptyList();
        }
    }

    void callRefreshReportTasks(Integer refreshRecordsCount) {
        log.info(String.format("callRefreshReportTasks with maxRecordsCount : %s", refreshRecordsCount));

        String runFunction = " call cft_task_db.refresh_report_tasks( ? ) ";

        try (Connection conn = DriverManager.getConnection(
            containerReplica.getJdbcUrl(), containerReplica.getUsername(), containerReplica.getPassword());
             PreparedStatement preparedStatement = conn.prepareStatement(runFunction)) {

            preparedStatement.setInt(1, Objects.isNull(refreshRecordsCount) ? null : refreshRecordsCount);
            preparedStatement.execute();

        } catch (SQLException e) {
            log.error("Procedure call refresh_report_tasks failed with SQL State : {}, {} ",
                      e.getSQLState(), e.getMessage());
        } catch (Exception e) {
            log.error("Procedure call refresh_report_tasks failed with SQL State : {}, {} ",
                      e.getCause(), e.getMessage());
        }

    }

}
