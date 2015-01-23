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
