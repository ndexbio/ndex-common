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
