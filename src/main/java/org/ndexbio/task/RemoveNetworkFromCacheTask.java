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
