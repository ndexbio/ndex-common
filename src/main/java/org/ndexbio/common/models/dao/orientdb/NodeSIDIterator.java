package org.ndexbio.common.models.dao.orientdb;

import org.ndexbio.common.NdexClasses;

import com.orientechnologies.orient.core.record.impl.ODocument;

public class NodeSIDIterator extends NetworkElementIterator<String> {
	public NodeSIDIterator(Iterable<ODocument> nsDocs) {
		super(nsDocs);
	}

	@Override
	public String next() {
		ODocument doc = this.docs.next();
		if ( doc == null ) return null;
		
		String SID = doc.field(NdexClasses.Element_SID);
		
		if ( SID !=null)
			return SID;
		
		long id = doc.field(NdexClasses.Element_ID);
		return  Long.toString(id);
	}
}
