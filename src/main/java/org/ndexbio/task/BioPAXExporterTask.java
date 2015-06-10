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
import java.io.FileOutputStream;
import java.util.UUID;

import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.exporter.BioPAXNetworkExporter;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Task;
import org.ndexbio.model.object.Status;
import org.ndexbio.task.event.NdexTaskEventHandler;
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

public class BioPAXExporterTask extends NdexTask {
	
	private static final String NETWORK_EXPORT_PATH = "/opt/ndex/exported-networks/";
	private static final String FILE_EXTENSION = ".owl";

	private NdexTaskEventHandler eventHandler;
	private Status taskStatus;
	
	
	private static final Logger logger = LoggerFactory
			.getLogger(XGMMLExporterTask.class);
	
	public BioPAXExporterTask(Task task) throws
		IllegalArgumentException, SecurityException, NdexException{
		
			super(task);
	}

	@Override
	public Task call() throws Exception {
		try {
			this.exportNetwork();
			return this.getTask();
		} catch (InterruptedException e) {
			logger.info(this.getClass().getName() +" interupted");
			return null;
		} finally {
			if (null != this.eventHandler) {
				this.eventHandler.shutdown();
			}
		}
	}
	
	/*
	 * private method to invoke the xbel network exporter
	 */
	private void exportNetwork() throws Exception{
		this.taskStatus = Status.PROCESSING;
		this.startTask();
		String exportFilename = this.resolveFilename(BioPAXExporterTask.NETWORK_EXPORT_PATH, BioPAXExporterTask.FILE_EXTENSION);

		FileOutputStream out = new FileOutputStream (exportFilename);
		ODatabaseDocumentTx db = null; 
		try {
			db = NdexDatabase.getInstance().getAConnection();
			BioPAXNetworkExporter exporter = new BioPAXNetworkExporter(db);
			exporter.exportNetwork( UUID.fromString(getTask().getResource()), out );
			this.taskStatus = Status.COMPLETED;
			this.updateTaskStatus(this.taskStatus);
		} finally { 
			if ( db !=null ) db.close();
		    out.close();
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
	

}
