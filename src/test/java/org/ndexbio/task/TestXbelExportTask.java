/**
 * Copyright (c) 2013, 2015, The Regents of the University of California, The Cytoscape Consortium
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

import java.io.IOException;
import java.util.Date;

import org.ndexbio.common.models.dao.orientdb.CommonDAOValues;
import org.ndexbio.common.models.dao.orientdb.TaskDAO;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Status;
import org.ndexbio.model.object.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Test application to insert network export tasks into the Task type
 * mod 04April2014 - switch to new TaskDAO interface to database
 */
public class TestXbelExportTask {

	
	private static final Logger logger = LoggerFactory.getLogger(TestXbelExportTask.class);
	private final String[] networkIds;
	
	private final TaskDAO dao;
	
	private final String testUserId = "C31R3";
	
	public TestXbelExportTask (String[] ids){
		this.networkIds = ids;
		this.dao = null;
	/*			DAOFactorySupplier.INSTANCE.resolveDAOFactoryByType(CommonDAOValues.ORIENTDB_DAO_TYPE)
				.get().getTaskDAO();
		this.insertExportTasks(); */
		
	}
	
	
	public static void main(String[] args) throws IOException {
		//String networkId = "C25R732"; // is for large corpus
//		String[] ids = new String[]{"C25R1308"}; // is for small corpus
		String[] ids = new String[]{"C2R2"}; // is for small corpus
//		TestXbelExportTask test = new TestXbelExportTask(ids);
		//add shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.out.println("TextXbelExportTask completed.");
			}
		});
	}
	
	private void insertExportTasks(){
		for (String id : this.networkIds){
			
			try {
				Task task = this.generateTask(id);
	//			task = this.dao.createXBELExportNetworkTask(id, testUserId);
				logger.info("netwok upload task " +task.getExternalId() +" queued in database");
			} catch (IllegalArgumentException  e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			  
		}
	}
	
	private Task generateTask(String networkId) {
		Task task = new Task();
		task.setResource(networkId);
//		task.setCreatedDate(new Date());
		task.setStatus(Status.QUEUED);
		
		return task;
	}
	

}
