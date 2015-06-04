package org.ndexbio.task;

import java.util.List;

import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Task;
import org.ndexbio.common.persistence.orientdb.NdexTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orientechnologies.orient.core.id.ORID;

public class TestTaskQueries {
	private final NdexTaskService taskService ;
	private static final Logger logger = LoggerFactory.getLogger(TestTaskQueries.class);
	
	public TestTaskQueries() {
		taskService = new NdexTaskService();
	}
	
	private void runTests() {
		this.determineInProgressTasks();
//			this.determineQueuedTasks();
//			this.determineCompletedTasks();
		
	}
	
	public static void main(String[] args) throws NdexException {
		TestTaskQueries test = new TestTaskQueries();
		test.runTests();

	}

	private  Integer determineInProgressTasks() {
/*		List<Task> taskList = taskService.getInProgressTasks();
		Integer activeTasks = taskList.size();
		logger.info("There are " +taskList.size() +" in progress tasks");
		for ( Task task : taskList){
			
			logger.info("Task id " +resolveVertexId(task) +" is in progress owner = " +task.getOwner().getUsername());
		}
		return activeTasks; */
		return null;
	}
/*	
	private  Integer determineQueuedTasks() throws NdexException {
		List<ITask> taskList = taskService.getQueuedITasks();
		Integer activeTasks = taskList.size();
		logger.info("There are " +taskList.size() +" in queued tasks");
		for ( ITask task : taskList){
			 
			logger.info("Task id " +resolveVertexId(task) +" is queued owner "
					+task.getOwner().getUsername());
		}
		return activeTasks;
	}
	
	private  Integer determineCompletedTasks() throws NdexException {
		List<ITask> taskList = taskService.getAllCompletedITasks();
		Integer activeTasks = taskList.size();
		logger.info("There are " +taskList.size() +" completed tasks");
		for ( ITask task : taskList){
			 
			logger.info("Task id " +resolveVertexId(task) +" is completed owner "
					+task.getOwner().getUsername() +" completion status = " +task.getStatus().toString());
		}
		return activeTasks;
	}
	
	protected String resolveVertexId(VertexFrame vf)
    {
        if (null == vf)
            return null;

        return IdConverter.toJid((ORID)vf.asVertex().getId());
    }
	*/
}
