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
			d.reload();
			Long actId = d.field(NdexClasses.Network_P_readOnlyCommitId);
			
			if ( ! actId.equals(taskCommitId)) {
				// stop task
				getTask().setMessage("Network cache not created. readOnlyCommitId="
						+ actId + " in db, but in task we have commitId=" + taskCommitId);
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
				getTask().setMessage("Network cache not created. Second check found network readOnlyCommitId is"+ 
				   actId + ", task has commitId " + taskCommitId);
				return;
			}

			d.field(NdexClasses.Network_P_cacheId,taskCommitId).save();
			logger.info("Cache " + actId + " created.");
			dao.commit();
	    }
	}
	
	
}
