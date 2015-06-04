package org.ndexbio.task;

public abstract class NdexTaskProcessor implements Runnable {

	protected boolean shutdown;
	
	public NdexTaskProcessor () {
		shutdown = false;
	}
	
	public void shutdown() {
		shutdown = true;
	}

	
}
