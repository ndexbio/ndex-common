package org.ndexbio.common;

@Deprecated
public enum JdexIdService
{
    INSTANCE;
    
    /*
     * placeholder for component to query OrientDB database
     * for the next JDex ID value
     */
    private static Long maxJdexId = 0L;
    
    public Long getNextJdexId()
    {
            return ++maxJdexId;
    }

	public void reset() {
		maxJdexId = 0L;
		
	}
}