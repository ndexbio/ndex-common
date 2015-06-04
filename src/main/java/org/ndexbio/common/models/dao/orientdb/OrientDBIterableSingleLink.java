package org.ndexbio.common.models.dao.orientdb;

import java.util.Iterator;

import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class OrientDBIterableSingleLink implements Iterable<ODocument> {
	private OrientDBLinkIterator iterator;
	public OrientDBIterableSingleLink (ODocument doc) {
		iterator = new OrientDBLinkIterator(doc);		
	}
	
	@Override
	public Iterator<ODocument> iterator() {
		return iterator;
	}

}
