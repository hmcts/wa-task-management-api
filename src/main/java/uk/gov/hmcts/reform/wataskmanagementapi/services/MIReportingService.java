package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskHistoryResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskHistoryResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.schedulers.LogicalReplicationCreatorScheduler;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;

import static uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskHistoryResourceRepository.CREATE_SUBSCRIPTION;

@Service
public class MIReportingService {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(LogicalReplicationCreatorScheduler.class);
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
            createPublicationSlot();
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
//            LOGGER.info("Found logical replication slot with name " + MAIN_SLOT_NAME);
            return true;
        }
    }

    private void createPublicationSlot() {
        taskResourceRepository.createReplicationSlot();
        LOGGER.info("Created logical replication slot " + MAIN_SLOT_NAME);
    }

    private boolean isPublicationPresent() {
        int count = taskResourceRepository.isPublicationPresent();
        if (count == 0) {
            LOGGER.info("No publication present");
            return false;
        } else {
//            LOGGER.info("Found publication");
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

            Matcher urlMatcher = Patterns.URL_MATCHING_PATTERN.matcher(connection.getMetaData().getURL());
            if (urlMatcher.matches()) {
                String host = urlMatcher.group("hostString");
                String port = urlMatcher.group("port");
                String dbName = urlMatcher.group("dbname");

                createSubscription(host, port, dbName);
                LOGGER.info("Subscription created for: " + host + ":" + port + "/" + dbName);
            } else {
                LOGGER.error("Cannot extract publication URL from the datasource");
            }

            URL url = new URL(connection.getMetaData().getURL());
        } catch (SQLException ex) {
            LOGGER.error("Primary datasource connection exception.", ex);
        } catch (MalformedURLException mue) {
            LOGGER.error("Malformed primary datasource URL.", mue);
        }
    }

    void createSubscription(String host, String port, String dbName) {
        String user = "repl_user";
        String password = "repl_password";

        String logQuery = CREATE_SUBSCRIPTION
            .replace(":slotname", "main_slot_v1")
            .replace(":username", user)
            .replace(":password", password)
            .replace(":host", host)
            .replace(":port", port)
            .replace(":dbname", dbName);
        LOGGER.info("Query: " + logQuery);

        taskHistoryRepository.createSubscription("main_slot_v1", host, port, dbName, user, password);
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
