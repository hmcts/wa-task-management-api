package net.hmcts.taskperf.model;

import lombok.Value;

@Value
public class SearchRequest
{
	private ClientQuery query;
	private User user;
}
