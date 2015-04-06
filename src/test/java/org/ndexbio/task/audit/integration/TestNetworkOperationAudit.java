package org.ndexbio.task.audit.integration;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ndexbio.task.audit.NdexAuditService;
import org.ndexbio.task.audit.NdexAuditServiceFactory;
import org.ndexbio.task.audit.NdexAuditUtils;

import com.google.common.base.Optional;

/*
 * Represents a Java integration test designed to validate the functionality of the NetworkOperationAuditService
 * class. Implemented as a Java application
 */
public class TestNetworkOperationAudit {
	private static final  Logger logger = LoggerFactory.getLogger(TestNetworkOperationAudit.class);
	 
	private NdexAuditService service;
	private static final NdexAuditUtils.AuditOperation operation = NdexAuditUtils.AuditOperation.NETWORK_EXPORT;
	private static final String testNetworkName = "testNetwork";
	
	public TestNetworkOperationAudit () {
		Optional<NdexAuditService> optService =  NdexAuditServiceFactory.INSTANCE.
				getAuditServiceByOperation(testNetworkName, operation);
		if(optService.isPresent()){
			this.service = optService.get();
			logger.info("NdexAuditServiceFactory returned an instance of " + this.service.getClass().getName());
		} else {
			logger.error("NdexAuditServiceFactory failed to return a NdexAuditService subclass");
		}
	}

	private void performTests(int observationCount) {
		this.generateObservedMetrics(observationCount);
		this.displayObservedMetrics();
	}
	
	
	
	private void generateObservedMetrics(int observationCount){
		List<String> supportedMetrics = NdexAuditUtils.getNetworkMetricsList;
		int listSize = supportedMetrics.size();
		for (int i =0 ; i < observationCount ;i++){
			int index = (int) Math.floor(Math.random() * listSize);
			String metric = supportedMetrics.get(index);
			this.service.incrementObservedMetricValue(metric);
		}
		
	}
	
	
	private void displayObservedMetrics() {
		System.out.println(this.service.displayObservedValues());
	}
	
	public static void main(String[] args) {
		
		TestNetworkOperationAudit test = new TestNetworkOperationAudit();
		test.performTests(10000);
				Runtime.getRuntime().addShutdownHook(new Thread() {
					public void run() {
						System.out.println("TestNetworkOperationAudit completed.");
					}
				});

	}

}
