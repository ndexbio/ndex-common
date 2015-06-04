package org.ndexbio.task;

import java.io.File;

import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Task;
import org.ndexbio.model.object.TaskAttribute;


public class RemoveNetworkFromCacheTask extends NdexTask {

	public RemoveNetworkFromCacheTask(Task itask) throws NdexException {
		super(itask);
	}

	@Override
	public Task call() throws NdexException {
		this.deleteNetworkCache();
		return this.getTask();
		
	}

	private void deleteNetworkCache() throws NdexException {
		
//		String networkIdStr = this.getTask().getResource();
		
	//	try ( NetworkDAO dao = new NetworkDAO(NdexDatabase.getInstance().getAConnection())) {
			
			Long commitId = (Long)getTask().getAttribute(TaskAttribute.readOnlyCommitId);
			String fullpath = Configuration.getInstance().getNdexNetworkCachePath() + commitId +".gz";
			
			File file = new File(fullpath);

			file.delete();
			
/*			ODocument d = dao.getNetworkDocByUUIDString(networkIdStr);
			
			Long cacheId = d.field(NdexClasses.Network_P_cacheId);
			
			
			if ( cacheId !=null && cacheId.equals(commitId) ) {
				d.field(NdexClasses.Network_P_cacheId, -1).save();
			} else {
				getTask().setMessage("Network CacheId not cleared because unmatched CacheId.");
			}
			
			dao.commit(); */
	//    }
	}
	

	
}
