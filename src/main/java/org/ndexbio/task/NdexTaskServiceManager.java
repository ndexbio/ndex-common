package org.ndexbio.task;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.Service.State;
import com.google.common.util.concurrent.ServiceManager;


/*
 * Represents a standalone application that will invoke asynchronous tasks that 
 * implement the Google Guava Service interface
 * This application is designed to run continuously, preferably as a 
 * UNIX/Linux daemon and to be automatically started by an /etc/init.d script
 * 
 * Re: registerServices method for instructions on how to add a new Service
 * 
 */
public class NdexTaskServiceManager {
	
	
	private final ServiceManager manager;
	private static final Logger logger = LoggerFactory.getLogger(NdexTaskServiceManager.class);
	
	public NdexTaskServiceManager() {
	    this.manager = new ServiceManager(this.registerServices());
	}
	
	private void startTasks() {
		this.manager.startAsync();
		logger.info("NDEx service tasks started");
		
	}
	
	private void stopTasks() {
		this.manager.stopAsync();
		logger.info("Service tasks stopped");
	}
	
	private boolean isHealthy() { 
		return this.manager.isHealthy();
	}
	
	/*
	 * Private method to log the status of all registered tasks
	 */
	
	private String displayServiceStatus() {
		StringBuilder sb = new StringBuilder();
		for (java.util.Map.Entry<State, Service> entry : this.manager.servicesByState().entries()) {
			sb.append("\nService: " +entry.getValue().getClass().getName() +" state " +entry.getKey().toString());
		}
				
		return sb.toString();
	}

	/*
	 * The application's main method; intended to run continuously
	 * Periodically, the application will check the status of all registered tasks.
	 * If any task isn't running, the application will log the status of all tasks
	 */
	public static void main(String[] args) {
		NdexTaskServiceManager taskManager = new NdexTaskServiceManager();
		taskManager.startTasks();
		// run an infinite loop
		while (true){
			try {
				Thread.sleep(600000L);  // 
				if(taskManager.isHealthy()){
					logger.info("All service tasks are running");
				} else {
					logger.info(taskManager.displayServiceStatus());
				}
				
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
				taskManager.stopTasks();
				return;
			} 
		
		}
		
	}
	
	/*
	 * Private method to register a new Service implementation
	 * The invocation frequency is determined by the Service's scheduler
	 * To add a new Service, simply add an new instance to the list
	 * eg.  list.add( new myNewservice());
	 */
	private List<Service> registerServices() {
		List<Service> list = Lists.newArrayList();
		// register the task deletion service 
		list.add(new TaskDeletionService());
		return list;
	}

}
