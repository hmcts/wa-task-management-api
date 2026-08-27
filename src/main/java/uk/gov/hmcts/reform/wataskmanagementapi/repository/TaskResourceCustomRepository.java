package uk.gov.hmcts.reform.wataskmanagementapi.repository;

import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.TaskSearchRoleCriteria;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface TaskResourceCustomRepository {

    @Transactional
    List<String> searchTasksIds(int firstResult,
                                int maxResults,
                                Collection<TaskSearchRoleCriteria> roleCriteria,
                                List<String> excludeCaseIds,
                                SearchRequest searchRequest);

    @Transactional
    List<String> searchTasksIdsOld(int firstResult,
                                   int maxResults,
                                   Set<String> filterSignature,
                                   Set<String> roleSignature,
                                   List<String> excludeCaseIds,
                                   SearchRequest searchRequest);

    @Transactional
    Long searchTasksCount(Collection<TaskSearchRoleCriteria> roleCriteria,
                          List<String> excludeCaseIds,
                          SearchRequest searchRequest);

    @Transactional
    Long searchTasksCountOld(Set<String> filterSignature,
                             Set<String> roleSignature,
                             List<String> excludeCaseIds,
                             SearchRequest searchRequest);

}
