package org.ndexbio.task.event;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import com.google.common.collect.ComparisonChain;

/*
 * A POJO representing properties of an NdexEvent
 */

public class NdexExportTaskEvent extends NdexTaskEvent implements Comparable<NdexExportTaskEvent>{
	
	private final long threadId;
	private  String networkId;
	private String networkName;
	private String taskType;
	private String operation;
	private String metric;
	private long value;
	private String units;
	private int entityCount;
	private String entityValue; // what's being counted
	private final Date date;
	
	// list of event attributes to write out
	// using reflection to obtain getter methods finds too many
	
	private  final String[] attributeList = {"NetworkId","NetworkName", "TaskType",
		"Operation","Metric","Value","Units","EntityCount","EntityValue","Date"};
	
	
	public NdexExportTaskEvent(){
		super();
		this.threadId = Thread.currentThread().getId();
		this.date = new Date();
		this.networkId = NdexNetworkState.INSTANCE.getNetworkId();
		this.networkName = NdexNetworkState.INSTANCE.getNetworkName();
	}

	public String getNetworkId() {
		return networkId;
	}

	public void setNetworkId(String networkId) {
		this.networkId = networkId;
	}

	public String getNetworkName() {
		return networkName;
	}

	public void setNetworkName(String networkName) {
		this.networkName = networkName;
	}

	public String getTaskType() {
		return taskType;
	}

	public void setTaskType(String taskType) {
		this.taskType = taskType;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public String getMetric() {
		return metric;
	}

	public void setMetric(String metric) {
		this.metric = metric;
	}

	public long getValue() {
		return value;
	}

	public void setValue(long value) {
		this.value = value;
	}

	public int getEntityCount() {
		return entityCount;
	}

	public void setEntityCount(int entityCount) {
		this.entityCount = entityCount;
	}

	public long getThreadId() {
		return threadId;
	}

	public Date getDate() {
		return date;
	}
	
	public boolean equals(Object that) {
		return Objects.equals(this, that);
	}
	
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
		
	}

	@Override
	public int compareTo(NdexExportTaskEvent that) {
		return ComparisonChain.start()
				.compare(this.getEntityCount(),that.getEntityCount())
				.compare(this.getEntityValue(),that.getEntityValue())
				.compare(this.getNetworkId(), that.getNetworkId())
				.compare(this.getThreadId(), that.getThreadId())
				.compare(this.getValue(), that.getValue())
				.compare(this.getUnits(), that.getUnits())
				.compare(this.getDate(), that.getDate())
				.compare(this.getMetric(), that.getMetric())
				.compare(this.getNetworkName(),	 that.getNetworkName())
				.compare(this.getOperation(), that.getOperation())
				.compare(this.getTaskType(), that.getTaskType())
				.result();
	}

	public String getUnits() {
		return units;
	}

	public void setUnits(String units) {
		this.units = units;
	}
	
	public List<String> getEventAttributes() {
		return Arrays.asList(this.attributeList);
	}

	public String getEntityValue() {
		return entityValue;
	}

	public void setEntityValue(String entityValue) {
		this.entityValue = entityValue;
	}

	
	}
		

		 

