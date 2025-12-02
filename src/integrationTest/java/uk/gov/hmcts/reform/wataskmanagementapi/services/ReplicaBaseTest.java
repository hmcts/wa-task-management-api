package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.JdbcDatabaseContainer;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.ReplicaTaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.ReportableTaskRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.SubscriptionCreator;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskAssignmentsRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskHistoryResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.db.TCExtendedContainerDatabaseDriver;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.AwaitilityIntegrationTestConfig;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@SpringBootTest
@ActiveProfiles({"integration"})
@AutoConfigureMockMvc(addFilters = false)
@TestInstance(PER_CLASS)
@Slf4j
@Import(AwaitilityIntegrationTestConfig.class)
public abstract class ReplicaBaseTest {
    protected static final String TEST_REPLICA_DB_USER = "repl_user";
    protected static final String TEST_REPLICA_DB_PASS = "repl_user";

    protected static final String TEST_PRIMARY_DB_USER = "wa_user";
    protected static final String TEST_PRIMARY_DB_PASS = "wa_password";
    protected static final String TEST_PUBLICATION_URL = "postgresql://cft_task_db:5432";
    protected static final String ENVIRONMENT = "local-arm-arch";

    @Autowired
    protected TaskResourceRepository taskResourceRepository;

    @Autowired
    protected ReplicaTaskResourceRepository replicaTaskResourceRepository;

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

    @BeforeAll
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

        await()
            .until(() -> miReportingService.hasReplicationStarted());
        return true;
    }

    @AfterAll
    void tearDown() {
        container.stop();
        containerReplica.stop();
    }

}
