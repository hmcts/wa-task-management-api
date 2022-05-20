package net.hmcts.taskperf.model;

import lombok.Value;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SortOrder;

@Value
public class SortBy
{
	private SortField column;
	private SortOrder direction;
}
