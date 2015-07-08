package org.ndexbio.common.models.dao.orientdb;

import java.io.IOException;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Status;
import org.ndexbio.model.object.Task;
import org.ndexbio.model.object.TaskAttribute;
import org.ndexbio.model.object.TaskType;
import org.slf4j.MDC;

import com.orientechnologies.orient.core.record.impl.ODocument;


public class NetworkDAOTx extends OrientdbDAO {

	public NetworkDAOTx () throws NdexException {
	    super(NdexDatabase.getInstance().getAConnection());

	}

	
	/** 
	 * Set a flag of a network. We currently only support setting readOnly flag for download optimization purpose.
	 * @param UUIDstr
	 * @param parameter
	 * @param value
	 * @return
	 * @throws NdexException 
	 * @throws IOException 
	 */
	public long setReadOnlyFlag(String UUIDstr,  boolean value, String userAccountName) throws NdexException {
		
		
		ODocument networkDoc =this.getRecordByUUIDStr(UUIDstr, null);
		Long commitId = networkDoc.field(NdexClasses.Network_P_readOnlyCommitId);

		if ( commitId == null || commitId.longValue() < 0 ) {
		   if ( value ) { // set the flag to true
			    long newCommitId = NdexDatabase.getCommitId();
				networkDoc.fields(NdexClasses.Network_P_readOnlyCommitId, newCommitId).save();
				db.commit();
				Task createCache = new Task();
				createCache.setTaskType(TaskType.CREATE_NETWORK_CACHE);
				createCache.setResource(UUIDstr); 
				createCache.setStatus(Status.QUEUED);
				createCache.setAttribute(TaskAttribute.readOnlyCommitId, Long.valueOf(newCommitId));
                createCache.setAttribute("RequestsUniqueId", MDC.get("RequestsUniqueId"));
                
				TaskDAO taskDAO = new TaskDAO(this.db);
				taskDAO.createTask(userAccountName, createCache);
				db.commit();
			   
		   } 
		   return -1;
			   
		} 
		
		// was readOnly
		if ( !value ) { // unset the flag
			networkDoc.fields(NdexClasses.Network_P_readOnlyCommitId, Long.valueOf(-1),
					          NdexClasses.Network_P_cacheId, Long.valueOf(-1)).save();
			db.commit();
			Task deleteCache = new Task();
			deleteCache.setTaskType(TaskType.DELETE_NETWORK_CACHE);
			deleteCache.setResource(UUIDstr); 
			deleteCache.setStatus(Status.QUEUED);
			deleteCache.setAttribute(TaskAttribute.readOnlyCommitId, commitId);
			
			TaskDAO taskDAO = new TaskDAO(this.db);
			taskDAO.createTask(userAccountName, deleteCache);
			db.commit();
			
		} 
		return commitId.longValue();
		
		
	}

}
