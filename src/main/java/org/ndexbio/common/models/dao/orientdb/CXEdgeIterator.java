package org.ndexbio.common.models.dao.orientdb;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.model.object.network.CXEdge;

import com.orientechnologies.orient.core.record.impl.ODocument;

public class CXEdgeIterator extends NetworkElementIterator<CXEdge> {

	public CXEdgeIterator(Iterable<ODocument> edgeDocs) {
		super(edgeDocs);
	}

	@Override
	public CXEdge next() {
		ODocument doc = this.docs.next();
		if ( doc == null ) return null;
		
		
		String SID = doc.field(NdexClasses.Element_SID);
		
		if ( SID ==null) {
			 SID = ((Long)doc.field(NdexClasses.Element_ID)).toString();
		}
		
		ODocument srcDoc = doc.field("in_"+ NdexClasses.Edge_E_subject);
		ODocument tgtDoc = doc.field("out_"+NdexClasses.Edge_E_object);
		
		String srcId = srcDoc.field(NdexClasses.Element_SID);
		if ( srcId == null )
			srcId = ( (Long)srcDoc.field(NdexClasses.Element_ID)).toString();
		
		String tgtId = tgtDoc.field(NdexClasses.Element_SID);
		if ( tgtId == null)
			tgtId = ((Long)tgtDoc.field(NdexClasses.Element_ID)).toString();
		return  new CXEdge(SID, srcId,tgtId);
	}

}
