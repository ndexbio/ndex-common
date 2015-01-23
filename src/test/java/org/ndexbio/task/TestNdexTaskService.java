package org.ndexbio.task;
import org.ndexbio.common.persistence.orientdb.NdexTaskService;
import org.ndexbio.model.exceptions.NdexException;

public class TestNdexTaskService {

	private final NdexTaskService service;
	
	public TestNdexTaskService() {
		this.service = new NdexTaskService();
	}
	
	private void performTests() {
		this.processTasksQueuedForDeletion();
		
	}
	private void processTasksQueuedForDeletion(){
		try {
			service.deleteTasksQueuedForDeletion();
		} catch (NdexException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		TestNdexTaskService test = new TestNdexTaskService();
		test.performTests();

	}

}
