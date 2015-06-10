/**
 *   Copyright (c) 2013, 2015
 *  	The Regents of the University of California
 *  	The Cytoscape Consortium
 *
 *   Permission to use, copy, modify, and distribute this software for any
 *   purpose with or without fee is hereby granted, provided that the above
 *   copyright notice and this permission notice appear in all copies.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *   WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *   MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *   ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *   WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *   ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *   OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
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
