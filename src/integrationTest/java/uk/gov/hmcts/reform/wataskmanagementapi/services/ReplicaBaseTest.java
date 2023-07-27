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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@Slf4j
public abstract class ReplicaBaseTest extends SpringBootIntegrationBaseTest {
    protected static final String TEST_REPLICA_DB_USER = "repl_user";
    protected static final String TEST_REPLICA_DB_PASS = "repl_user";

    protected static final String TEST_PRIMARY_DB_USER = "wa_user";
    protected static final String TEST_PRIMARY_DB_PASS = "wa_password";


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
            TEST_PRIMARY_DB_PASS);

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

}
