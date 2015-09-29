package org.ndexbio.task;

import java.io.File;
import java.io.FileOutputStream;

import org.ndexbio.common.models.dao.orientdb.SingleNetworkDAO;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Status;
import org.ndexbio.model.object.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CXExporterTask extends NdexTask {

	private static final String NETWORK_EXPORT_PATH = BioPAXExporterTask.NETWORK_EXPORT_PATH;
	private static final String FILE_EXTENSION = ".CX";

	private Status taskStatus;
	
	
	private static final Logger logger = LoggerFactory
			.getLogger(CXExporterTask.class);
	
	public CXExporterTask(Task itask) throws NdexException {
		super(itask);
		// TODO Auto-generated constructor stub
	}


	@Override
	public Task call() throws Exception {
		try {
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
		String exportFilename = this.resolveFilename(NETWORK_EXPORT_PATH, FILE_EXTENSION);

		FileOutputStream out = new FileOutputStream (exportFilename);
		try {
			
			try (SingleNetworkDAO snetworkDAO = new SingleNetworkDAO(getTask().getResource())) {
				snetworkDAO.writeNetworkInCX(out, true);
			}
			
			this.taskStatus = Status.COMPLETED;
			this.updateTaskStatus(this.taskStatus);
		} finally { 
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
