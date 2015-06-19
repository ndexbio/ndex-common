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
package org.ndexbio.task;

import java.io.File;
import java.io.IOException;

import org.ndexbio.common.access.NdexAOrientDBConnectionPool;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.exporter.XbelNetworkExporter;
import org.ndexbio.common.exporter.XbelNetworkExporter.XbelMarshaller;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.task.event.NdexNetworkState;
import org.ndexbio.task.event.NdexTaskEventHandler;
import org.ndexbio.task.service.NdexJVMDataModelService;
import org.ndexbio.task.service.NdexTaskModelService;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

public class TestXbelExporterApp {

	private static final String NETWORK_EXPORT_PATH = "/opt/ndex/exported-networks/";
	private static final String XBEL_FILE_EXTENSION = ".xbel";
	public static void main(String[] args) throws IOException, NdexException {

		Configuration configuration = Configuration.getInstance();
    	
    	//and initialize the db connections
		NdexDatabase.createNdexDatabase("http://localhost", configuration.getDBURL(), configuration.getDBUser(), configuration.getDBPasswd(), 10);
		
		
//		String networkId = "5c5fa4a7-6376-11e4-98cb-90b11c72aefa"; // is for small corpus
		String networkId = "02221e14-6ae6-11e4-b14b-000c29873918";
		String userId =    "29969f4f-5e02-11e4-bac2-000c29873918"; // dbowner
		//add shutdown hook
/*		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.out.println("TextXbelExporter completed.");
			}
		}); */
		
		ODatabaseDocumentTx db = null;
		try {
			
			db = NdexDatabase.getInstance().getAConnection();
			NdexTaskModelService  modelService = new NdexJVMDataModelService(db);
			// initiate the network state
			initiateStateForMonitoring(modelService, userId, networkId);
//			NdexTaskEventHandler eventHandler = new NdexTaskEventHandler("/opt/ndex/exported-networks/ndextaskevents.csv");
			XbelNetworkExporter exporter = new XbelNetworkExporter(userId, networkId, 
				modelService,
				resolveExportFile(modelService, userId, networkId));
		//
			exporter.exportNetwork();
//			eventHandler.shutdown();
		} finally { 
			if ( db != null) db.close();
		}
		
	}

	private static String resolveExportFile(NdexTaskModelService  modelService, 
			String userId, String networkId) {
		StringBuilder sb = new StringBuilder(NETWORK_EXPORT_PATH);
		
		sb.append(userId);
//		if (! new File(sb.toString()).exists()) {
			new File(sb.toString()).mkdirs();
//		}
		sb.append(File.separator);
		sb.append(modelService.getNetworkById(networkId).getExternalId().toString());
		sb.append(XBEL_FILE_EXTENSION);
		System.out.println("Export file: " +sb.toString());
		return sb.toString();
	
	}
	
	private static void initiateStateForMonitoring(NdexTaskModelService  modelService, 
			String userId,
			String networkId) {
		NdexNetworkState.INSTANCE.setNetworkId(networkId);
		NdexNetworkState.INSTANCE.setNetworkName(modelService.getNetworkById( networkId).getName());
		
		
	}
	
}
