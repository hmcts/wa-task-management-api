package uk.gov.hmcts.reform.wataskmanagementapi.repository;

import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.TaskSearchRoleCriteria;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface TaskResourceCustomRepository {

    @Transactional
    List<String> searchTasksIdsUsingRoleCriteria(int firstResult,
                                                 int maxResults,
                                                 Collection<TaskSearchRoleCriteria> roleCriteria,
                                                 List<String> excludeCaseIds,
                                                 SearchRequest searchRequest);

    @Transactional
    List<String> searchTasksIdsUsingSearchIndex(int firstResult,
                                                int maxResults,
                                                Set<String> filterSignature,
                                                Set<String> roleSignature,
                                                List<String> excludeCaseIds,
                                                SearchRequest searchRequest);

    @Transactional
    Long searchTasksCountUsingRoleCriteria(Collection<TaskSearchRoleCriteria> roleCriteria,
                                           List<String> excludeCaseIds,
                                           SearchRequest searchRequest);

    @Transactional
    Long searchTasksCountUsingSearchIndex(Set<String> filterSignature,
                                          Set<String> roleSignature,
                                          List<String> excludeCaseIds,
                                          SearchRequest searchRequest);

}
