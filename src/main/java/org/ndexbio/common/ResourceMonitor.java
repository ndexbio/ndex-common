package org.ndexbio.common;

public enum ResourceMonitor {
	INSTANCE;
	 private static final Double MEGABYTE = 1024D * 1024D;
	 
	 public Double getMemoryMbUsage() {
		 // Get the Java runtime
		    Runtime runtime = Runtime.getRuntime();
		    
		    // Calculate the used memory
		    Double memory = (double) (runtime.totalMemory() - runtime.freeMemory());
		    return memory / MEGABYTE;
	 }
}
