/**
 *   Copyright (c) 2013, 2015
 *  	The Regents of the University of California
 *  	The Cytoscape Consortium
 *
 *   Permission to use, copy, modify, and distribute this software for any
 *   purpose with or without fee is hereby granted, provided that the above
 *   copyright notice and this permission notice appear in all copies.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *   WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *   MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *   ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *   WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *   ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *   OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
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
