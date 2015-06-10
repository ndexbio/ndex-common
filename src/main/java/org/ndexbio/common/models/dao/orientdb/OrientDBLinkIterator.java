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
package org.ndexbio.common.models.dao.orientdb;

import java.util.Iterator;

import com.orientechnologies.orient.core.record.impl.ODocument;

public class OrientDBLinkIterator implements Iterator<ODocument> {

	private ODocument odoc;
	private boolean hasBeenRead;
	
	public OrientDBLinkIterator(ODocument doc) {
		odoc = doc;
		hasBeenRead=false;
	}
	
	@Override
	public boolean hasNext() {
		return ! hasBeenRead;
	}

	@Override
	public ODocument next() {
		if ( hasBeenRead)
			return null;
		hasBeenRead=true;
		return odoc;
	}

	@Override
	public void remove() {
	}

}
