package org.ndexbio.common.models.object.network;

public class RawEdge implements Comparable<RawEdge> {

	private long subjectId;
	private long predicateId;
	private long objectId;
	
	public RawEdge( long subject, long predicate, long object) {
		this.subjectId 		= subject;
		this.predicateId 	= predicate;
		this.objectId 		= object;
	}
	
	@Override
	public int compareTo(RawEdge o) {
		long c = subjectId - o.getSubjectId();
		if (c>0) return 1;
		if ( c<0 ) return -1;
		
		c = objectId - o.getObjectId();
		if (c>0) return 1;
		if ( c<0 ) return -1;

		c = predicateId - o.getPredicateId();
		if (c>0) return 1;
		if ( c<0 ) return -1;
		
		return 0;
	}

	public long getSubjectId() {
		return subjectId;
	}


	public long getPredicateId() {
		return predicateId;
	}

	public long getObjectId() {
		return objectId;
	}

	
	@Override
	public int hashCode() {
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + (int) (subjectId ^ (subjectId >>> 32));
	    result = prime * result + (int) (predicateId ^ (predicateId >>> 32));
	    result = prime * result + (int) (objectId ^ (objectId >>> 32));
	    return result;
	}
	
	@Override
	public	boolean equals (Object obj) {
		if ( obj instanceof RawEdge) {
			return compareTo((RawEdge)obj) == 0;
		}
		return false;
	}
}
