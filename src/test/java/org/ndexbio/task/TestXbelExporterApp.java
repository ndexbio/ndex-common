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
