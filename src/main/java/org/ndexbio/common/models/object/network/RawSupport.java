package org.ndexbio.common.models.object.network;

public class RawSupport implements Comparable <RawSupport>{
	private final static int nary = 500000; 
	private String text;
	private long citationId;
	
	public RawSupport (String text, long citationId) {
		this.text = text;
		this.citationId = citationId;
	}

	public String getSupportText() {return text;}
	public long getCitationId() { return citationId;}
	
	@Override
	public int hashCode () {
		return (int) (text.hashCode() + citationId * nary);
	}

	@Override
	public int compareTo(RawSupport o) {
		long c = (int) (citationId - o.getCitationId());
		
		if (c!=0 ) return c>0 ? 1 : -1;
		
		return text.compareTo(o.getSupportText());
	}
	
	@Override
	public boolean equals (Object o) {
		if (o instanceof RawSupport) {
			return compareTo((RawSupport)o) == 0;
		}
		return false;
	}
	
}
