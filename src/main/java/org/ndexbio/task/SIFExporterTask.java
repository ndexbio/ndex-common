package org.ndexbio.task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.UUID;

import org.ndexbio.common.exporter.SIFNetworkExporter;
import org.ndexbio.common.models.dao.orientdb.NetworkDocDAO;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Status;
import org.ndexbio.model.object.Task;
import org.ndexbio.model.object.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SIFExporterTask extends NdexTask {

	private static final String NETWORK_EXPORT_PATH = "/opt/ndex/exported-networks/";
	private static final String XGMML_FILE_EXTENSION = ".sif";

	
	private static final Logger logger = LoggerFactory
			.getLogger(SIFExporterTask.class);
	
	private Status taskStatus;

	public SIFExporterTask(Task itask) throws NdexException {
		super(itask);
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
	
	private void exportNetwork() throws Exception{

			this.taskStatus = Status.PROCESSING;
			this.startTask();
			String exportFilename = this.resolveFilename(NETWORK_EXPORT_PATH, XGMML_FILE_EXTENSION);

			Network network = null;
			try (NetworkDocDAO dao = new NetworkDocDAO()) {
				network = dao.getNetworkById(UUID.fromString(getTask().getResource()));
			
			}
		
			if ( network == null) throw new NdexException("Failed to get network from db.");
		
			try (FileOutputStream out = new FileOutputStream (exportFilename)) {

				OutputStreamWriter writer = new OutputStreamWriter(out);
				SIFNetworkExporter exporter = new SIFNetworkExporter (network);	
				exporter.exportNetwork( writer );
				this.taskStatus = Status.COMPLETED;
				this.updateTaskStatus(this.taskStatus);
				writer.close();
			} 
	}
	
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
