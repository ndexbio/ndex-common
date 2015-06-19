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

import java.util.concurrent.TimeUnit;

import org.ndexbio.common.persistence.orientdb.NdexTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractScheduledService;

/*
 * Represents a scheduled task that runs  on a periodic basis to scan for
 * database Task entries that have been marked for deletion.
 * The scheduler method determines the frequency of invocation
 * This service is invoked by registering an instance of this class with a Google
 * Service Manager
 */

public class TaskDeletionService extends AbstractScheduledService {
	
	private static final Logger logger = LoggerFactory.getLogger(TaskDeletionService.class);
	private  NdexTaskService ndexService; 
	
	protected void startup() {
		logger.info("TaskDeletionService started");
		ndexService = new NdexTaskService();
	}
	
	/*
	 * This task should run on a continuous basis so stopping it is an error
	 */
	protected void shutdown() {
		logger.error("TaskDeletionService stopped");
	}

	/*
	 * the runOneIteration method is what the ServiceManager invokes and represents the
	 * work of the Service
	 * the runOneIteration really means one iteration per time interval
	 * scan the Tasks for those marked for deletion and remove them from the database
	 */
	@Override
	protected void runOneIteration() throws Exception {
		this.ndexService.deleteTasksQueuedForDeletion();

	}
	

	/*
	 * schedule a scan for every minute
	 * TODO: make the time interval a property
	 */
	@Override
	protected Scheduler scheduler() {
		return Scheduler.newFixedRateSchedule(0, 1, TimeUnit.MINUTES);
	}

}
