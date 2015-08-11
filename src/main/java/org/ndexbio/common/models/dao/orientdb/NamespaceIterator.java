package org.ndexbio.common.models.dao.orientdb;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.model.object.network.Namespace;

import com.orientechnologies.orient.core.record.impl.ODocument;

public class NamespaceIterator extends NetworkElementIterator<Namespace> {

	public NamespaceIterator(Iterable<ODocument> nsDocs) {
		super(nsDocs);
	}

	@Override
	public Namespace next() {
		ODocument doc = this.docs.next();
		if ( doc == null ) return null;
		
		return getNamespace(doc);
	}

    private  Namespace getNamespace(ODocument ns)  {
        Namespace rns = new Namespace();
        rns.setId((long)ns.field(NdexClasses.Element_ID));
        rns.setPrefix((String)ns.field(NdexClasses.ns_P_prefix));
        rns.setUri((String)ns.field(NdexClasses.ns_P_uri));
        
        SingleNetworkDAO.getPropertiesFromDoc(ns, rns);
        
        return rns;
     } 
}
