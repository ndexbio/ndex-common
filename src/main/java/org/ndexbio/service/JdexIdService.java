package org.ndexbio.service;

public enum JdexIdService {
	INSTANCE;
	
	/*
	 * placeholder for component to query OrientDB database
	 * for the next JDex ID value
	 */
	private static Long maxJdexId = 0L;
	
	public Long getNextJdexId(){
		return ++maxJdexId;
	}
	

}
