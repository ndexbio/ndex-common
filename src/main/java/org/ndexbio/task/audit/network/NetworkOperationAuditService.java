package org.ndexbio.task.audit.network;



import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ndexbio.task.audit.NdexAuditService;
import org.ndexbio.task.audit.NdexAuditUtils;
import org.ndexbio.task.audit.NdexOperationMetrics;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
//import com.google.common.collect.Sets;

/*
 * Represents an NDEx service that an application performing a major operation on
 * an NDEx network can use to track that operation.
 * This class encapsulates a NetworkOperationAudit instance to persist operation metrics
 * 
 */
public class NetworkOperationAuditService extends NdexAuditService{
	
	
	private static final Log logger = LogFactory
			.getLog(NetworkOperationAuditService.class);
	
	private Set<NetworkProvenanceRecord> provenanceSet;
	private String networkFileName; // import or export file name
	protected final NetworkIdentifier networkId;
	
//	private Set<String> edgeIdSet = Sets.newConcurrentHashSet();
	
	public NetworkOperationAuditService(String  networkName, NdexAuditUtils.AuditOperation oper){		
		super( oper);	
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkName),
				"A network name is required");
		
		Optional<NetworkIdentifier> optId =  NdexAuditUtils.generateNetworkIdentifier(networkName);
		   
		   if( optId.isPresent() ){
			   this.networkId = optId.get();
			   this.metrics = new NdexOperationMetrics(oper);
			  	this.initializeMetrics(); 
		   }  else {
			   this.networkId = null;
			   logger.fatal("Unable to create NetworkIdentifier for " +networkName);
		   }   
	}
	
	
	public NetworkIdentifier getNetworkId() {
		return networkId;
	}
	
	@Override
	protected void initializeMetrics(){
		//for(String metric : NdexAuditUtils.getNetworkMetricsList){
		//	this.metrics.incrementMeasurement(metric);		 
		//}
	}

	public Set<NetworkProvenanceRecord> getProvenanceSet() {
		return provenanceSet;
	}

	public synchronized void addProvenanceRecord(NetworkProvenanceRecord record){
		Preconditions.checkArgument(null != record, 
				"A NetworkProvenanceRecord is required");
		this.provenanceSet.add(record);
	}
	
	public String getNetworkFileName() {
		return networkFileName;
	}

	/* 
	 * we should only be calling this method once, so synchronization
	 * isn't going to cause a big impact
	 */
	public synchronized void setNetworkFileName(String networkFileName) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkFileName), 
				"A network file name is required");
		this.networkFileName = networkFileName;
	}

	

	@Override
	public String displayObservedValues() {
		Set<Entry<String, Long> > observedSet = this.metrics.getObservedDataMap();
		StringBuffer sb = new StringBuffer();
		sb.append("\n Network Operation Audit Record");
		sb.append("\nNetwork: " +this.networkId.getNetworkURI() +" operation:" );
		sb.append("\n\n+++Observed Metrics:");
		for (Map.Entry<String ,Long> entry : observedSet) {
			sb.append("\n" +entry.getKey() +" = " +entry.getValue());
		}
		// display comments
		sb.append("\nComments:");
		sb.append(metrics.getComments());
		
		return sb.toString();
	}

	@Override
	public String displayExpectedValues() {
		Set<Entry<String, Long> > expectedSet = this.metrics.getExpectedDataMap();
		StringBuffer sb = new StringBuffer();
		sb.append("\n\n+++Expected Metrics:");
		for (Map.Entry<String ,Long> entry : expectedSet) {
			sb.append("\n" +entry.getKey() +" = " +entry.getValue());
		}
		return sb.toString();
	}

	@Override
	public String displayDeltaValues() {
		Map<String,Long> deltaMap = this.metrics.generateDifferenceMap();
		StringBuffer sb = new StringBuffer();
		sb.append("\n\n+++Delta (ovserved minus expected):");
		for (Map.Entry<String ,Long> entry : deltaMap.entrySet()) {
			sb.append("\n" +entry.getKey() +" = " +entry.getValue());
		}
		return sb.toString();
	}

	

}
