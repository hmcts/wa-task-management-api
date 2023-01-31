package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.postgresql.Driver;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.ReportableTaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskHistoryResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.ReportableTaskRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskHistoryResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ReplicationException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;


@Service
public class MIReportingService {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MIReportingService.class);
    public static final String MAIN_SLOT_NAME = "main_slot_v1";

    String user;
    String password;

    @Autowired
    private DataSource dataSource;

    @Autowired
    @Qualifier("replicaDataSource")
    private DataSource replicaDataSource;

    private final TaskHistoryResourceRepository taskHistoryRepository;
    private final TaskResourceRepository taskResourceRepository;
    private final ReportableTaskRepository reportableTaskRepository;


    public MIReportingService(TaskHistoryResourceRepository tasksHistoryRepository,
                              TaskResourceRepository taskResourceRepository,
                              ReportableTaskRepository reportableTaskRepository,
                              @Value("${replication.username}") String user,
                              @Value("${replication.password}") String password) {
        this.taskHistoryRepository = tasksHistoryRepository;
        this.taskResourceRepository = taskResourceRepository;
        this.reportableTaskRepository = reportableTaskRepository;
        this.user = user;
        this.password = password;
    }

    public List<TaskHistoryResource> findByTaskId(String taskId) {
        return taskHistoryRepository.findAllByTaskIdOrderByUpdatedAsc(taskId);
    }

    public List<ReportableTaskResource> findByReportingTaskId(String taskId) {
        return reportableTaskRepository.findAllByTaskIdOrderByUpdatedAsc(taskId);
    }

    public void logicalReplicationCheck() {
        LOGGER.debug("Postgresql logical replication check executed");
        if (isReplicationSlotPresent()) {
            if (!isPublicationPresent()) {
                createPublication();
            }
            if (!isSubscriptionPresent()) {
                createSubscription();
            }
        } else {
            LOGGER.info("Creating logical replication slot");
            createReplicationSlot();
        }
    }

    private boolean isReplicationSlotPresent() {
        int count = taskResourceRepository.countReplicationSlots();
        if (count == 0) {
            LOGGER.info("No logical replication slot present for " + MAIN_SLOT_NAME);
            return false;
        } else {
            return true;
        }
    }

    private void createReplicationSlot() {
        taskResourceRepository.createReplicationSlot();
        LOGGER.info("Created logical replication slot " + MAIN_SLOT_NAME);
    }

    protected boolean isPublicationPresent() {
        int count = taskResourceRepository.countPublications();
        if (count == 0) {
            LOGGER.info("No publication present");
            return false;
        } else {
            return true;
        }
    }

    private void createPublication() {
        taskResourceRepository.createPublication();
        LOGGER.info("Created publication");
    }

    private boolean isSubscriptionPresent() {
        int count = taskHistoryRepository.countSubscriptions();
        if (count == 0) {
            LOGGER.info("No subscription present");
            return false;
        } else {
            return true;
        }
    }

    private void createSubscription() {
        try (Connection connection = dataSource.getConnection();
             Connection connection2 = replicaDataSource.getConnection();) {

            LOGGER.info("Primary datasource URL: " + connection.getMetaData().getURL());
            LOGGER.info("Replica datasource URL: " + connection2.getMetaData().getURL());

            Properties properties = Driver.parseURL(connection.getMetaData().getURL(), null);
            Properties replicaProperties = Driver.parseURL(connection2.getMetaData().getURL(), null);

            String host = properties.get("PGHOST").toString();
            String port = properties.get("PGPORT").toString();
            String dbName = properties.get("PGDBNAME").toString();

            String replicaHost = replicaProperties.get("PGHOST").toString();
            String replicaPort = replicaProperties.get("PGPORT").toString();
            String replicaDbName = replicaProperties.get("PGDBNAME").toString();

            createSubscription(host, port, dbName, replicaHost, replicaPort, replicaDbName);
            LOGGER.info("Subscription created for: " + host + ":" + port + "/" + dbName);

        } catch (SQLException ex) {
            LOGGER.error("Primary datasource connection exception.", ex);
        }
    }

    void createSubscription(String host, String port, String dbName,
                            String replicaHost, String replicaPort, String replicaDbName) {

        String replicaUrl = "jdbc:postgresql://" + host + ":" + replicaPort + "/" + replicaDbName
            + "?user=" + user + "&password=" + password;
        LOGGER.info("replicaUrl = " + replicaUrl);

        String subscriptionUrl;
        if ("5432".equals(port)) {
            //hard coded host for local environment, will need fixing when we move to remote environments
            subscriptionUrl = "postgresql://" + host + ":" + port + "/" + dbName
                + "?user=" + user + "&password=" + password;
        } else {
            //this is hard coded for integration test locally
            subscriptionUrl = "postgresql://" + "cft_task_db" + ":" + "5432" + "/" + dbName
                + "?user=" + user + "&password=" + password;
        }

        String sql = "CREATE SUBSCRIPTION task_subscription CONNECTION '" + subscriptionUrl
            + "' PUBLICATION task_publication WITH (slot_name = main_slot_v1, create_slot = FALSE);";

        sendToDatabase(replicaUrl,sql);
    }

    private void sendToDatabase(String replicaUrl, String sql) {
        try (Connection subscriptionConn = DriverManager.getConnection(replicaUrl);
             Statement subscriptionStatement = subscriptionConn.createStatement();) {

            subscriptionStatement.execute(sql);

            LOGGER.info("Subscription created");
        } catch (SQLException e) {
            LOGGER.error("Error setting up replication", e);
            throw new ReplicationException("An error occurred during setting up of replication", e);
        }
    }
}
