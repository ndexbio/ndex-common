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
