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
