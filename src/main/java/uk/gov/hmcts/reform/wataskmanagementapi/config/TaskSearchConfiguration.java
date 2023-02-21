package uk.gov.hmcts.reform.wataskmanagementapi.config;

import net.hmcts.taskperf.service.TaskSearchAdaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;

import javax.sql.DataSource;

@Configuration
public class TaskSearchConfiguration {

    @Autowired
    private CFTTaskMapper cftTaskMapper;
    @Autowired
    private TaskResourceRepository taskResourceRepository;
    @Autowired
    private DataSource dataSource;

    @Bean
    public TaskSearchAdaptor taskSearchAdaptor() {
        return new TaskSearchAdaptor(cftTaskMapper, taskResourceRepository, dataSource);
    }

}
