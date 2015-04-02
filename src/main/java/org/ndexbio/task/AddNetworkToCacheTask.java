package org.ndexbio.task;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Task;
import org.ndexbio.model.object.TaskAttribute;
import org.ndexbio.model.object.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class AddNetworkToCacheTask extends NdexTask {
	
	static Logger logger = LoggerFactory.getLogger(AddNetworkToCacheTask.class);

	public AddNetworkToCacheTask(Task itask) throws NdexException {
		super(itask);
	}
	
	@Override
	public Task call() throws Exception {
		logger.info("creating cache.");
		this.createNetworkCache();
		logger.info("finished creating cache.");
		return this.getTask();
	}

	private void createNetworkCache() throws NdexException {
		
		String networkIdStr = this.getTask().getResource();
		
		try ( NetworkDAO dao = new NetworkDAO(NdexDatabase.getInstance().getAConnection())) {
			Long taskCommitId = (Long)getTask().getAttribute(TaskAttribute.readOnlyCommitId);

			String fullpath = Configuration.getInstance().getNdexNetworkCachePath() + taskCommitId+".gz";

			ODocument d = dao.getNetworkDocByUUIDString(networkIdStr);
			
			Long actId = d.field(NdexClasses.Network_P_readOnlyCommitId);
			
			if ( ! actId.equals(taskCommitId)) {
				// stop task
				getTask().setMessage("Network Cache not created because unmatched readOnlyCommitId.");
				return;
			}
			
			// create cache.
			
			Network n = dao.getNetworkById(UUID.fromString(networkIdStr));
			
			try (GZIPOutputStream w = new GZIPOutputStream( new FileOutputStream(fullpath), 16384)) {
					//  String s = mapper.writeValueAsString( original);
					ObjectMapper mapper = new ObjectMapper();
					mapper.writeValue(w, n);
			} catch (FileNotFoundException e) {
				throw new NdexException ("Can't create network cache file in server: " + fullpath);
			} catch (IOException e) {
				throw new NdexException ("IO Error when writing cache file: " + fullpath + ". Cause: " + e.getMessage());
			}
			 
			//check again.	
			d.reload();
			actId = d.field(NdexClasses.Network_P_readOnlyCommitId);
			if ( ! actId.equals(taskCommitId)) {
				// stop task
				getTask().setMessage("Network Cache not created because unmatched readOnlyCommitId.");
				return;
			}

			d.field(NdexClasses.Network_P_cacheId,taskCommitId).save();
			dao.commit();
	    }
	}
	
	
}
