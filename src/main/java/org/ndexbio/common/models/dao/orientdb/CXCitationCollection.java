package org.ndexbio.common.models.dao.orientdb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.cx.aspect.CitationElement;
import org.ndexbio.model.cx.CXSupport;
import org.ndexbio.model.object.network.Citation;
import org.ndexbio.model.object.network.Support;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class CXCitationCollection implements Iterable<CitationElement> {

	private Iterable<ODocument> citationDocs;
	private ODatabaseDocumentTx db;
	
	public CXCitationCollection(Iterable<ODocument> citations, ODatabaseDocumentTx odb) {
		this.citationDocs = citations;
		db = odb;
	}

	@Override
	public Iterator<CitationElement> iterator() {
		return new CXCitationIterator(citationDocs);
	}
	
	  private class CXCitationIterator extends NetworkElementIterator<CitationElement> {
	    	public CXCitationIterator (Iterable<ODocument> citeDocs) {
	    		super(citeDocs);
	    	}

			@Override
			public CitationElement next() {
				ODocument doc = this.docs.next();
				if ( doc == null ) return null;
				
				/*
				String SID = doc.field(NdexClasses.Element_SID);
				
				if ( SID ==null) {
					 SID = ((Long)doc.field(NdexClasses.Element_ID)).toString();
				} */
				CitationElement result = new CitationElement();
				
				long citationID = (long)doc.field(NdexClasses.Element_ID);
				result.setTitle((String)doc.field(NdexClasses.Citation_P_title));
				result.setCitationType((String)doc.field(NdexClasses.Citation_p_idType));
				result.setIdentifier((String)doc.field(NdexClasses.Citation_P_identifier));
				
				List<String> o = doc.field(NdexClasses.Citation_P_contributors);
				
				if ( o!=null && !o.isEmpty())
					result.setContributor(o);
				
		    /*	List<NdexPropertyValuePair> props = doc.field(NdexClasses.ndexProperties);
		    	if ( props !=null && props.size() > 0 )
		    		result.setProperties(props);
*/
				// get the supports
				
				OIndex<?> citationIdx = db.getMetadata().getIndexManager().getIndex(NdexClasses.Index_support_citation);
				Collection<OIdentifiable> cIds =  (Collection<OIdentifiable>) citationIdx.get( citationID ); // account to traverse by
				
				if ( !cIds.isEmpty()) {
					Collection<CXSupport> ss = new ArrayList<> (cIds.size());
					for ( OIdentifiable od : cIds ) {
						ODocument sDoc = od.getRecord();
						CXSupport s = new CXSupport();
						s.setText((String)sDoc.field(NdexClasses.Support_P_text));
						ss.add(s);
					}
					result.setSupports(ss);
				}
				
				
		        
			
				return  result;
			}

	    }


}
