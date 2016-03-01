/**
 * Copyright (c) 2013, 2016, The Regents of the University of California, The Cytoscape Consortium
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

import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.exporter.XbelNetworkExporter;
import org.ndexbio.common.models.dao.orientdb.NetworkDocDAO;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Task;
import org.ndexbio.model.object.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

/*
 * Represents an NdexTask subclass responsible for exporting an XBEL network
 * from the NDEx database to an external file in XML format that adheres to the
 * XBEL schema. If the task type is KAMCOMPILE, the class is also responsible
 * for creating a new Task enrty in the database indicating that the new XML
 * file should be processed by the Kam compiler.
 * 
 */

public class XbelExporterTask extends NdexTask {
	
	private String networkId;

	private static final String XBEL_FILE_EXTENSION = ".xbel";
//	private static final String EVENT_FILE_EXTENSION = ".csv";

	private Status taskStatus;
	
	
	private static final Logger logger = LoggerFactory
			.getLogger(XbelExporterTask.class);
	
	public XbelExporterTask(Task task) throws
		IllegalArgumentException, SecurityException, NdexException{
		
			super(task);
			this.networkId = this.getTask().getResource();
//			this.modelService = new NdexJVMDataModelService();
			
	}

	@Override
	public Task call() throws Exception {
		try {
			//TODO: Event stuff was commented out bj CJ. need to review later.
	/*		String eventFilename = 
					this.resolveFilename(this.NETWORK_EXPORT_EVENT_PATH, this.EVENT_FILE_EXTENSION);
			this.eventHandler = new NdexTaskEventHandler(eventFilename); */
			this.exportNetwork();
			return this.getTask();
		} catch (InterruptedException e) {
			logger.info(this.getClass().getName() +" interupted");
			return null;
		} 
	}
	
	/*
	 * private method to invoke the xbel network exporter
	 */
	private void exportNetwork() throws Exception{
		this.taskStatus = Status.PROCESSING;
		this.startTask();
		String exportFilename = this.resolveFilename(
				Configuration.getInstance().getNdexRoot() + "/exported-networks/", 
				XbelExporterTask.XBEL_FILE_EXTENSION);
	
		ODatabaseDocumentTx db = null; 
		try {
			try ( NetworkDocDAO dao = new NetworkDocDAO (NdexDatabase.getInstance().getAConnection())) {
				XbelNetworkExporter exporter = new XbelNetworkExporter(this.getTask().getTaskOwnerId().toString(),
						this.networkId,dao, exportFilename);
				exporter.exportNetwork();
				this.taskStatus = Status.COMPLETED;
				this.updateTaskStatus(this.taskStatus);
			}
		} finally { 
			if ( db !=null ) db.close();
		}
	}
	
	/*
	 * private method to resolve the filename for the exported file
	 * Current convention is to use a fixed based directory under /opt/ndex
	 * add a subdriectory based on the username and use the network name plus the
	 * xbel extension as a filename
	 */
	private String resolveFilename(String path, String extension) {
		// create the directory if not exists
		if (! new File(path).exists()) {
			new File(path).mkdir();
		}
		
		StringBuilder sb = new StringBuilder(path);
		sb.append(File.separator);
		sb.append(this.getTask().getExternalId());
		sb.append(extension);
		return sb.toString();		
	}
	
	

	protected String getNetworkId() {
		return networkId;
	}
/*
	private void setNetworkId(String networkId) {
		this.networkId = networkId;
	}  */

}
