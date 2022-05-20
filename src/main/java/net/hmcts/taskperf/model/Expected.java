package net.hmcts.taskperf.model;

import java.util.List;

import lombok.Value;

@Value
public class Expected
{
	private int totalCount;
	private List<String> taskIds;
}
