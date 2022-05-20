package net.hmcts.taskperf.model;

import lombok.Value;

@Value
public class Pagination
{
	private int pageNumber;
	private int pageSize;
}
