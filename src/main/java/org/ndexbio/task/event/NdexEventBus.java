package org.ndexbio.task.event;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;


/*
 * Singleton implemented as a enum provides global access to a
 * thread safe event bus. 
 * 
 */
public enum NdexEventBus {
	
	INSTANCE;
	
	private final ExecutorService exec = Executors.newCachedThreadPool();
	private  EventBus eventBus = new AsyncEventBus(exec);
	//private  EventBus eventBus = new EventBus();
	
	public EventBus getEventBus() {
		System.out.println("*****getEventBus");
		return eventBus;
	}
	
	public void shutdown() {
		System.out.println(this.getClass().getSimpleName() +" shutdown received");
		
		this.exec.shutdown();
	}

}
