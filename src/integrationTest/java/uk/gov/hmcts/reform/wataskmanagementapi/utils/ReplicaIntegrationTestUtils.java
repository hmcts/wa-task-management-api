package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.opentest4j.AssertionFailedError;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.JdbcDatabaseContainer;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.ReportableTaskRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.SubscriptionCreator;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskAssignmentsRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskHistoryResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.db.TCExtendedContainerDatabaseDriver;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.services.MIReportingService;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@Slf4j
public class ReplicaIntegrationTestUtils {
    protected static final String TEST_REPLICA_DB_USER = "repl_user";
    protected static final String TEST_REPLICA_DB_PASS = "repl_user";

    protected static final String TEST_PRIMARY_DB_USER = "wa_user";
    protected static final String TEST_PRIMARY_DB_PASS = "wa_password";
    protected static final String TEST_PUBLICATION_URL = "postgresql://cft_task_db:5432";
    protected static final String ENVIRONMENT = "local-arm-arch";

    protected TaskResourceRepository taskResourceRepository;
    protected TaskHistoryResourceRepository taskHistoryResourceRepository;
    protected ReportableTaskRepository reportableTaskRepository;
    protected TaskAssignmentsRepository taskAssignmentsRepository;
    protected MIReportingService miReportingService;

    protected String primaryJdbcUrl;

    private String replicaJdbcUrl;

    @Getter
    protected MIReportingService miReportingServiceForTest;

    @Getter
    protected SubscriptionCreator subscriptionCreatorForTest;

    @Getter
    protected JdbcDatabaseContainer container;
    @Getter
    protected JdbcDatabaseContainer containerReplica;

    public ReplicaIntegrationTestUtils(
        TaskResourceRepository taskResourceRepository,
        TaskHistoryResourceRepository taskHistoryResourceRepository,
        ReportableTaskRepository reportableTaskRepository,
        TaskAssignmentsRepository taskAssignmentsRepository,
        MIReportingService miReportingService,
        String primaryJdbcUrl,
        String replicaJdbcUrl
    ) {
        this.taskResourceRepository = taskResourceRepository;
        this.taskHistoryResourceRepository = taskHistoryResourceRepository;
        this.reportableTaskRepository = reportableTaskRepository;
        this.taskAssignmentsRepository = taskAssignmentsRepository;
        this.miReportingService = miReportingService;
        this.primaryJdbcUrl = primaryJdbcUrl;
        this.replicaJdbcUrl = replicaJdbcUrl;
    }

    public void setUp() {
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

        await().until(() -> miReportingService.hasReplicationStarted());
        return true;
    }

    public void tearDown() {
        container.stop();
        containerReplica.stop();
    }
}
