package org.ndexbio.service.monitor;

import org.hyperic.sigar.Sigar;

public enum SigarUtils {
	INSTANCE;
	private Sigar sigar = new Sigar();
	
	public long getPID() {
		return sigar.getPid();
	}
	

}
