package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskHistoryResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskHistoryResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;


@Service
public class MIReportingService {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MIReportingService.class);
    public static final String MAIN_SLOT_NAME = "main_slot_v1";

    @Autowired
    private DataSource dataSource;

    @Autowired
    @Qualifier("replicaDataSource")
    private DataSource replicaDataSource;

    private final TaskHistoryResourceRepository taskHistoryRepository;
    private final TaskResourceRepository taskResourceRepository;

    public MIReportingService(TaskHistoryResourceRepository tasksHistoryRepository,
                              TaskResourceRepository taskResourceRepository) {
        this.taskHistoryRepository = tasksHistoryRepository;
        this.taskResourceRepository = taskResourceRepository;
    }

    public List<TaskHistoryResource> findByTaskId(String taskId) {
        return taskHistoryRepository.getByTaskId(taskId);
    }

    public void logicalReplicationCheck() {
        LOGGER.debug("Postgresql logical replication check executed");
        if (!isReplicationSlotPresent()) {
            LOGGER.info("Creating logical replication slot");
            createReplicationSlot();
        } else {
            if (!isPublicationPresent()) {
                createPublication();
            }
            if (!isSubscriptionPresent()) {
                createSubscription();
            }
        }
    }

    private boolean isReplicationSlotPresent() {
        int count = taskResourceRepository.isReplicationSlotPresent();
        if (count == 0) {
            LOGGER.info("No logical replication slot present for " + MAIN_SLOT_NAME);
            return false;
        } else {
            LOGGER.info("Found logical replication slot with name " + MAIN_SLOT_NAME);
            return true;
        }
    }

    private void createReplicationSlot() {
        taskResourceRepository.createReplicationSlot();
        LOGGER.info("Created logical replication slot " + MAIN_SLOT_NAME);
    }

    private boolean isPublicationPresent() {
        int count = taskResourceRepository.isPublicationPresent();
        if (count == 0) {
            LOGGER.info("No publication present");
            return false;
        } else {
            LOGGER.info("Found publication");
            return true;
        }
    }

    private void createPublication() {
        taskResourceRepository.createPublication();
        LOGGER.info("Created publication");
    }

    private boolean isSubscriptionPresent() {
        int count = taskHistoryRepository.isSubscriptionPresent();
        if (count == 0) {
            LOGGER.info("No subscription present");
            return false;
        } else {
            LOGGER.info("Found subscription");
            return true;
        }
    }

    private void createSubscription() {
        try {
            Connection connection = dataSource.getConnection();
            LOGGER.info("Primary datasource URL: " + connection.getMetaData().getURL());

            Connection connection2 = replicaDataSource.getConnection();
            LOGGER.info("Replica datasource URL: " + connection2.getMetaData().getURL());

            Matcher urlMatcher = Patterns.URL_MATCHING_PATTERN.matcher(connection.getMetaData().getURL());
            Matcher replicaUrlMatcher = Patterns.URL_MATCHING_PATTERN.matcher(connection2.getMetaData().getURL());
            if (urlMatcher.matches() && replicaUrlMatcher.matches()) {
                String host = urlMatcher.group("hostString");
                String port = urlMatcher.group("port");
                String dbName = urlMatcher.group("dbname");

                String replicaHost = replicaUrlMatcher.group("hostString");
                String replicaPort = replicaUrlMatcher.group("port");
                String replicaDbName = replicaUrlMatcher.group("dbname");

                createSubscription(host, port, dbName, replicaHost, replicaPort, replicaDbName);
                LOGGER.info("Subscription created for: " + host + ":" + port + "/" + dbName);
            } else {
                LOGGER.error("Cannot extract publication URL from the datasource");
            }

        } catch (SQLException ex) {
            LOGGER.error("Primary datasource connection exception.", ex);
        }
    }

    void createSubscription(String host, String port, String dbName, String replicaHost, String replicaPort, String rDbName) {
        String user = "repl_user";
        String password = "repl_password";

        String replicaUrl = "jdbc:postgresql://" + host + ":" + replicaPort + "/" + rDbName + "?user=" + user + "&password=" + password;
        LOGGER.info("replicaUrl = " + replicaUrl);

        String subscriptionUrl;
        if ("5432".equals(port)) {
            //hard coded host for local environment, will need fixing when we move to remote environments
            subscriptionUrl = "postgresql://" + "ccd-shared-database-0" + ":" + port + "/" + dbName + "?user=" + user + "&password=" + password;
        } else {
            //this is hard coded for integration test locally
            subscriptionUrl = "postgresql://" + "cft_task_db" + ":" + "5432" + "/" + dbName + "?user=" + user + "&password=" + password;
        }

        try (Connection subscriptionConn = DriverManager.getConnection(replicaUrl);
             Statement subscriptionStatement = subscriptionConn.createStatement();) {

            String sql = "CREATE SUBSCRIPTION task_subscription CONNECTION '" + subscriptionUrl + "' PUBLICATION task_publication WITH (slot_name = main_slot_v1, create_slot = FALSE);";
            LOGGER.info("CREATE SUBSCRIPTION SQL = " + sql);
            subscriptionStatement.execute(sql);

            LOGGER.info("Subscription created");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public interface Patterns {
        Pattern URL_MATCHING_PATTERN = Pattern.compile(
            "jdbc:(tc:)?" +
                "(?<databaseType>[a-z0-9]+)" +
                "(:(?<imageTag>[^:]+))?" +
                "://" +
                "(?<hostString>[^?]+)" +
                ":" +
                "(?<port>[^?]+)" +
                "/" +
                "(?<dbname>[^?]+)" +
                "(?<queryParameters>\\?.*)?"
        );
    }
}
