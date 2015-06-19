/**
 * Copyright (c) 2013, 2015, The Regents of the University of California, The Cytoscape Consortium
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
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
		

		 

