package net.hmcts.taskperf.model;

import lombok.Value;

@Value
public class Pagination
{
	private int firstResult;
	private int maxResults;
}
