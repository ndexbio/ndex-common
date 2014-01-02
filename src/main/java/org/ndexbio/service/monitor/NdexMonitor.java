package org.ndexbio.service.monitor;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarLog;

public class NdexMonitor implements Runnable {
	
	 private final Sigar sigar;
     private Logger log;
     
	
	private final long pid;

	public NdexMonitor(long pid){
		this.pid = pid;
		this.sigar = new Sigar();
		SigarLog.enable(sigar);
		log = SigarLog.getLogger("NdexMonitor");
	    PropertyConfigurator.configure("log4j.properties");
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

}
