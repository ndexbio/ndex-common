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
package org.ndexbio.task.audit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.google.common.base.Preconditions;


/*
 * Abstract class represents common audit service functionality
 */
public abstract class NdexAuditService {
	
	protected NdexOperationMetrics metrics;
	protected static final Log logger = LogFactory
			.getLog(NdexAuditService.class);
	protected final NdexAuditUtils.AuditOperation operation;
	
	public NdexAuditService(NdexAuditUtils.AuditOperation oper){
		Preconditions.checkArgument(null != oper, "A valid operation is required");
		this.operation = oper;
	}

	protected abstract void initializeMetrics();
	
	public  void incrementObservedMetricValue(String metric){
		this.metrics.incrementObservedMetric(metric);
	}
	
	public  void incrementExpectedMetricValue(String metric){
		this.metrics.incrementExpectedMetric(metric);
	}
	
	public  void increaseExpectedMetricValue(String metric, Long amount){
		this.metrics.incrementExpectedMetricByAmount(metric, amount);
	}
	
	public Long getObservedMetricValue(String metric){
		return this.metrics.getObservedValue(metric);
	}
	
	
	public Long getExpectedMetricValue(String metric){
		return this.metrics.getExpectedValue(metric);
	}
	
	
	public  void setExpectedMetricValue(String metric, Long value){
		this.metrics.setExpectedValue(metric, value);
	}
	
	public void registerComment(String comment){
		this.metrics.appendComment(comment);
	}
	
	public abstract String displayObservedValues();
	public abstract String displayExpectedValues();
	public abstract String displayDeltaValues();
	
	
	

}
