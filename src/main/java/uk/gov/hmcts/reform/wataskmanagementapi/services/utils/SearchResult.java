package uk.gov.hmcts.reform.wataskmanagementapi.services.utils;

import java.util.List;

public record SearchResult(List<String> taskIds, long totalRecords) {
}
