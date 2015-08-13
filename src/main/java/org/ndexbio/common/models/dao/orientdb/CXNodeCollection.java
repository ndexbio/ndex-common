package org.ndexbio.common.models.dao.orientdb;

import java.util.Iterator;

import org.cxio.aspects.datamodels.NodesElement;
import org.ndexbio.common.NdexClasses;

import com.orientechnologies.orient.core.record.impl.ODocument;

public class CXNodeCollection implements Iterable<NodesElement> {
	
	Iterable<ODocument> nodeDocs;
	public CXNodeCollection(Iterable<ODocument> nsDocs) {
		nodeDocs = nsDocs;
	}



	@Override
	public Iterator<NodesElement> iterator() {
	   return new NodeSIDIterator(nodeDocs);
    }
	
	
	 
	private class NodeSIDIterator extends NetworkElementIterator<NodesElement> {
	
		public NodeSIDIterator(Iterable<ODocument> nsDocs) {
		super(nsDocs);
	   }

	@Override
	public NodesElement next() {
		ODocument doc = this.docs.next();
		if ( doc == null ) return null;
		
		String SID = doc.field(NdexClasses.Element_SID);
		
		if ( SID !=null)
			return new NodesElement(SID);
		
		long id = doc.field(NdexClasses.Element_ID);
		return  new NodesElement(id);
	}
	  
	} 
}
