package org.ndexbio.common.persistence;

import java.util.List;
import java.util.NoSuchElementException;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.helpers.IdConverter;
import org.ndexbio.common.models.data.ITask;
import org.ndexbio.common.models.object.Task;
import org.ndexbio.common.persistence.NdexOrientdbConnection;
import org.ndexbio.common.persistence.NdexOrientdbConnectionPool;

import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.id.ORID;

public class TestConnectionPool {
	String testUserId = "C31R3";
	String taskId = "C30R99";
	
	public TestConnectionPool() {
		
	}
	
	private void performTests() {
		try {
			List<NdexOrientdbConnection> connList = this.createMultipleConnections(8);
			System.out.println("Connect pool size " +connList.size());
			this.performTestQueries(connList);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private List<NdexOrientdbConnection> createMultipleConnections(int count) throws NoSuchElementException, IllegalStateException, Exception {
		List<NdexOrientdbConnection> poolList = Lists.newArrayList();
		for (int i=0 ; i < count; i++){
			poolList.add(NdexOrientdbConnectionPool.INSTANCE.getConnectionPool().borrowObject());
			System.out.println("Connection borrowed from pool");
		}
		return poolList;
		
	}
	
	
	private void performTestQueries(List<NdexOrientdbConnection> connList){
		int count = 1;
		for (NdexOrientdbConnection conn : connList){
			System.out.println("Task query");
			ITask task = this.performQuery(conn);
			if (null != task){
				System.out.println(count++ +" Task " + task.getStatus());
			}
			
		}
	}
	
	
	private ITask performQuery(NdexOrientdbConnection conn) {
	try
    {
		System.out.println("Performing query for task id " +taskId);
        final ORID taskRid = IdConverter.toRid(taskId);
      
        final ITask task = conn.getOrientDbGraph().getVertex(taskRid, ITask.class);
        return task;
    }
    
    catch (Exception e)
    {
        System.out.println(e.getMessage());
        
    }
    
    
    return null;
	}
	

	public static void main(String[] args) {
		TestConnectionPool test = new TestConnectionPool();
		test.performTests();

	}

}
