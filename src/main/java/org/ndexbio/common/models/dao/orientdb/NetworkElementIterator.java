package org.ndexbio.common.models.dao.orientdb;

import java.util.Iterator;

import com.orientechnologies.orient.core.record.impl.ODocument;

public abstract class NetworkElementIterator<E> implements Iterator<E> {

	protected Iterator<ODocument> docs;
	
	public NetworkElementIterator(Iterable<ODocument> nsDocs){
		docs = nsDocs.iterator();
	}
	
	@Override
	public boolean hasNext() {
		return docs.hasNext();
	}

	@Override
	public void remove() {
		   throw new UnsupportedOperationException();		
	}

}
