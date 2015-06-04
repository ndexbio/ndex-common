package org.ndexbio.common.models.dao.orientdb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.NetworkSourceFormat;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.exceptions.ObjectNotFoundException;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.Permissions;
import org.ndexbio.model.object.PropertiedObject;
import org.ndexbio.model.object.ProvenanceEntity;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.Citation;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.FunctionTerm;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.Node;
import org.ndexbio.model.object.network.ReifiedEdgeTerm;
import org.ndexbio.model.object.network.Support;
import org.ndexbio.model.object.network.VisibilityType;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientechnologies.orient.core.command.traverse.OTraverse;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.filter.OSQLPredicate;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

public class NetworkDocDAO extends OrientdbDAO {

	private static Logger logger = Logger.getLogger(NetworkDocDAO.class.getName());
	
	
	public NetworkDocDAO (ODatabaseDocumentTx db) {
	    super(db);

	}

	public NetworkDocDAO () throws NdexException {
	    this(NdexDatabase.getInstance().getAConnection());
	}

	public ProvenanceEntity getProvenance(UUID networkId) throws JsonParseException, JsonMappingException, IOException {
		// get the network document
		ODocument nDoc = getNetworkDocByUUIDString(networkId.toString());
		// get the provenance string
		String provenanceString = nDoc.field("provenance");
		// deserialize it to create a ProvenanceEntity object
		if (provenanceString != null && provenanceString.length() > 0){
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(provenanceString, ProvenanceEntity.class);
		} 
		
		return new ProvenanceEntity();
		
	}
    
	public int setProvenance(UUID networkId, ProvenanceEntity provenance) throws JsonProcessingException {
		// get the network document
		ODocument nDoc = getNetworkDocByUUIDString(networkId.toString());	
		// serialize the ProvenanceEntity
		ObjectMapper mapper = new ObjectMapper();
		String provenanceString = mapper.writeValueAsString(provenance);
		// store provenance string
		nDoc.field(NdexClasses.Network_P_provenance, provenanceString);
    //    nDoc.field(NdexClasses.ExternalObj_mTime, Calendar.getInstance().getTime());
		nDoc.save();
				
		return 1;
	}
	
	public ODocument getNetworkDocByUUIDString(String id) {
	     String query = "select from " + NdexClasses.Network + " where UUID='"
                +id+"' and (not isDeleted)";
        final List<ODocument> networks = db.query(new OSQLSynchQuery<ODocument>(query));
 
        if (networks.isEmpty())
	        return null;
        
        return networks.get(0);
   }


	public  Edge getEdgeFromDocument(ODocument doc, Network network) throws NdexException {
		Edge e = new Edge();
		e.setId((long)doc.field(NdexClasses.Element_ID));
		
		ODocument s =  doc.field("in_"+NdexClasses.Edge_E_subject);
		Long subjectId = s.field(NdexClasses.Element_ID);
		e.setSubjectId( subjectId );
		
		if ( network !=null && 
				!network.getNodes().containsKey(subjectId)) {
			Node node = getNode (s,network);
			network.getNodes().put(subjectId, node);
		}
		
		ODocument predicateDoc = (ODocument)doc.field("out_"+NdexClasses.Edge_E_predicate);
		Long predicateId = predicateDoc.field(NdexClasses.Element_ID);
		e.setPredicateId(predicateId);
		
		if ( network != null && !network.getBaseTerms().containsKey(predicateId)) {
    		   BaseTerm t = getBaseTerm(predicateDoc,network);
    		   network.getBaseTerms().put(t.getId(), t);
    	   }
		
		ODocument o = doc.field("out_"+NdexClasses.Edge_E_object);
		Long objectId = o.field(NdexClasses.Element_ID);
		e.setObjectId(objectId);
		
		if ( network !=null && 
				!network.getNodes().containsKey(objectId)) {
			Node node = getNode (o,network);
			network.getNodes().put(objectId, node);
		}

		getPropertiesFromDocument(e,doc,network);
		
		//populate citations
    	for (OIdentifiable citationRec : new OTraverse()
 				.field("out_"+ NdexClasses.Edge_E_citations )
 				.target(doc)
 				.predicate( new OSQLPredicate("$depth <= 1"))) {
    		ODocument citationDoc = (ODocument) citationRec;
    		if ( citationDoc.getClassName().equals(NdexClasses.Citation)) {
        		Long citationId = citationDoc.field(NdexClasses.Element_ID);
				if ( network != null &&
						!network.getCitations().containsKey(citationId)) {
					Citation citation = getCitationFromDoc(citationDoc, network);
					network.getCitations().put(citationId, citation);
				}
				e.getCitationIds().add(citationId);
    		}
    	}
   		
		//populate support
    	for (OIdentifiable supportRec : new OTraverse()
 				.field("out_"+ NdexClasses.Edge_E_supports )
 				.target(doc)
 				.predicate( new OSQLPredicate("$depth <= 1"))) {
    		ODocument supportDoc = (ODocument) supportRec;
    		if ( supportDoc.getClassName().equals(NdexClasses.Support)) {
        		Long supportId = supportDoc.field(NdexClasses.Element_ID);
				if ( network != null &&
						!network.getSupports().containsKey(supportId)) {
					Support support = getSupportFromDoc(supportDoc, network);
					network.getSupports().put(supportId, support);
				}
				e.getSupportIds().add(supportId);
    		}
    	}

		return e;
	}

    /**
     *  Create a node object from a document. If network is not null, also  
     *  create dependent objects (term, namespace, citation etc) in the network object. 
     * @param nodeDoc
     * @param network
     * @return
     * @throws NdexException 
     */
    public Node getNode(ODocument nodeDoc, Network network) throws NdexException {
    	Node n = new Node();

    	n.setId((long)nodeDoc.field(NdexClasses.Element_ID));
    	n.setName((String)nodeDoc.field(NdexClasses.Node_P_name));

    	// Populate properties
     	getPropertiesFromDocument(n, nodeDoc, network);

     	// populate baseterm
    	ODocument o = nodeDoc.field("out_" + NdexClasses.Node_E_represents);
    	if ( o != null) {
    		Long termId = o.field(NdexClasses.Element_ID);
    		String termType = o.getClassName();
            n.setRepresents(termId);    		
    		n.setRepresentsTermType(termType);
    		
    		if ( network != null) {
    			// populate objects in network
    			if ( termType.equals(NdexClasses.BaseTerm)) {
    				if ( !network.getBaseTerms().containsKey(termId) ) {
    					BaseTerm bTerm = getBaseTerm(o, network);
    					network.getBaseTerms().put(termId, bTerm);
    				}
    			} else if (termType.equals(NdexClasses.ReifiedEdgeTerm)) {
    				if ( !network.getReifiedEdgeTerms().containsKey(termId)) {
    					ReifiedEdgeTerm reTerm = getReifiedEdgeTermFromDoc(o,network);
    					network.getReifiedEdgeTerms().put(termId, reTerm);
    				}
    			} else if (termType.equals(NdexClasses.FunctionTerm)) {
    				if ( !network.getFunctionTerms().containsKey(termId)) {
    					FunctionTerm funcTerm = getFunctionTermfromDoc(o, network);
    					network.getFunctionTerms().put(termId, funcTerm);
    				}
    			} else 
    				throw new NdexException ("Unsupported term type '" + termType + 
    						"' found for term Id:" + termId);
    			
    		}
    	}
		
    	//populate aliases
    	for (OIdentifiable aliasRec : new OTraverse()
 				.field("out_"+ NdexClasses.Node_E_alias )
 				.target(nodeDoc)
 				.predicate( new OSQLPredicate("$depth <= 1"))) {
    		ODocument doc = (ODocument) aliasRec;
    		if ( doc.getClassName().equals(NdexClasses.BaseTerm)) {
        		Long termId = doc.field(NdexClasses.Element_ID);
        		if ( network != null &&
        				! network.getBaseTerms().containsKey(termId)) {
        			BaseTerm t = getBaseTerm(doc,network);
        			network.getBaseTerms().put(termId, t);
        		}
				n.getAliases().add(termId);
    		}
    	}
    	
    	//populate related terms
    	for (OIdentifiable relateToRec : new OTraverse()
 				.field("out_"+ NdexClasses.Node_E_relateTo )
 				.target(nodeDoc)
 				.predicate( new OSQLPredicate("$depth <= 1"))) {
    		ODocument doc = (ODocument) relateToRec;
    		if ( doc.getClassName().equals(NdexClasses.BaseTerm)) {
        		Long termId = doc.field(NdexClasses.Element_ID);
        		if ( network != null &&
        				! network.getBaseTerms().containsKey(termId)) {
        			BaseTerm t = getBaseTerm(doc,network);
        			network.getBaseTerms().put(termId, t);
        		}
				n.getRelatedTerms().add(termId);
    		}
    	}
    	
		//populate citations
    	for (OIdentifiable citationRec : new OTraverse()
 				.field("out_"+ NdexClasses.Node_E_citations )
 				.target(nodeDoc)
 				.predicate( new OSQLPredicate("$depth <= 1"))) {
    		ODocument doc = (ODocument) citationRec;
    		if ( doc.getClassName().equals(NdexClasses.Citation)) {
        		Long citationId = doc.field(NdexClasses.Element_ID);
				if ( network != null &&
						!network.getCitations().containsKey(citationId)) {
					Citation citation = getCitationFromDoc(doc,network);
					network.getCitations().put(citationId, citation);
				}
				n.getCitationIds().add(citationId);
    		}
    	}
   		
		//populate support
    	for (OIdentifiable supportRec : new OTraverse()
 				.field("out_"+ NdexClasses.Node_E_supports )
 				.target(nodeDoc)
 				.predicate( new OSQLPredicate("$depth <= 1"))) {
    		ODocument doc = (ODocument) supportRec;
    		if ( doc.getClassName().equals(NdexClasses.Support)) {
        		Long supportId = doc.field(NdexClasses.Element_ID);
				if ( network != null &&
						!network.getSupports().containsKey(supportId)) {
					Support support = getSupportFromDoc(doc, network);
					network.getSupports().put(supportId, support);
				}
				n.getSupportIds().add(supportId);
    		}
    	}

    	return n;
    }

    
	/**
	 *  This function returns the citations in this network.
	 * @param networkUUID
	 * @return
	 */
	public Collection<Citation> getNetworkCitations(String networkUUID) {
		ArrayList<Citation> citations = new ArrayList<>();
		
		ODocument networkDoc = getNetworkDocByUUIDString(networkUUID);
		
    	for (OIdentifiable reifiedTRec : new OTraverse()
 			.field("out_"+ NdexClasses.Network_E_Citations )
 			.target(networkDoc)
 			.predicate( new OSQLPredicate("$depth <= 1"))) {

    		ODocument doc = (ODocument) reifiedTRec;

    		if ( doc.getClassName().equals(NdexClasses.Citation)) {
    			citations.add(getCitationFromDoc(doc,null));
    		}
    	}
    	return citations;
	}


	public Namespace getNamespace(String prefix, String URI, UUID networkID ) {
		String query = "select from (traverse out_" +
	    		  NdexClasses.Network_E_Namespace +" from (select from "
	    		  + NdexClasses.Network + " where " +
	    		  NdexClasses.Network_P_UUID + "='" + networkID + 
	    		  "')) where @class='"+  NdexClasses.Namespace + "' and ";
		if ( prefix != null) {
	      query = query + NdexClasses.ns_P_prefix + "='"+ prefix +"'";
		}   else {
		  query = query + NdexClasses.ns_P_uri + "='"+ URI +"'";	
		}	
	    final List<ODocument> nss = db.query(new OSQLSynchQuery<ODocument>(query));
	     
	     if (nss.isEmpty())
	    	 return null;
         Namespace result = getNamespace(nss.get(0), null);
         return result;
	}


	public static Collection<Namespace> getNamespacesFromNetworkDoc(ODocument networkDoc,Network network) {
		ArrayList<Namespace> namespaces = new ArrayList<>();
		
		for (OIdentifiable reifiedTRec : new OTraverse()
 			.field("out_"+ NdexClasses.Network_E_Namespace)
 			.target(networkDoc)
 			.predicate( new OSQLPredicate("$depth <= 1"))) {

    		ODocument doc = (ODocument) reifiedTRec;

    		if ( doc.getClassName().equals(NdexClasses.Namespace)) {
    			namespaces.add(getNamespace(doc,network));
    		}
    	}
    	return namespaces;
	}
	

    public Support getSupport(String text, long citationId) {
		String query = "select from " + NdexClasses.Citation + " where " + 
		        NdexClasses.Element_ID + "=" + citationId;
	    final List<ODocument> citations = db.query(new OSQLSynchQuery<ODocument>(query));
	  
	    if ( !citations.isEmpty()) {
   			for (OIdentifiable supportRec : new OTraverse()
       	       	    .field("in_"+ NdexClasses.Support_E_citation )
       	            .target(citations.get(0))
       	            .predicate( new OSQLPredicate("$depth <= 1"))) {

   	              ODocument doc = (ODocument) supportRec;
        	          
        	      if ( doc.getClassName().equals(NdexClasses.Support)) {
        	         if ( doc.field(NdexClasses.Support_P_text).equals(text) ) {
        	             return getSupportFromDoc(doc,null);
        	         }
       	          }
        	}
	    }
        
    	return null;
    }
    
    public Collection<BaseTerm> getBaseTermsByPrefix(String networkUUID, String nsPrefix) {
		ArrayList<BaseTerm> baseTerms = new ArrayList<>();
		
		ODocument networkDoc = getNetworkDocByUUIDString(networkUUID);
		
		ODocument nsdoc = null;
    	for (OIdentifiable reifiedTRec : new OTraverse()
 			.field("out_"+ NdexClasses.Network_E_Namespace)
 			.target(networkDoc)
 			.predicate( new OSQLPredicate("$depth <= 1"))) {

    		ODocument doc = (ODocument) reifiedTRec;

    		if ( doc.getClassName().equals(NdexClasses.Namespace)) {
    			Namespace ns1 = getNamespace(doc,null);
    			if ( ns1.getPrefix()!=null && ns1.getPrefix().equals(nsPrefix)) {
    				nsdoc = doc;
    				break;
    			}
    		}
    	}
    	if (nsdoc != null) {
        	for (OIdentifiable reifiedTRec : new OTraverse()
     			.field("in_"+ NdexClasses.BTerm_E_Namespace)
     			.target(networkDoc)
     			.predicate( new OSQLPredicate("$depth <= 1"))) {

        		ODocument doc = (ODocument) reifiedTRec;

        		if ( doc.getClassName().equals(NdexClasses.BaseTerm)) {
        			BaseTerm t = getBaseTerm(doc,null);
        			baseTerms.add(t);
        		}
        	}
    	  	
    	}
    	return baseTerms;
    	
    }

    /**
     * This funciton return a self-contained sub network from a given citation. It is mainly for the XBel exporter.
     * No networkSummary values are populated from the db in the result.
     * @param networkUUID
     * @param citationId
     * @return
     * @throws NdexException
     */
    public Network getSubnetworkByCitation(String networkUUID, Long citationId) throws NdexException {
//    	ODocument networkDoc = this.getRecordById(UUID.fromString(networkUUID), NdexClasses.Network);
    	Network result = new Network();
    	
    	ODocument citationDoc = this.getDocumentByElementId(NdexClasses.Citation, citationId);
    	Citation c = getCitationFromDoc(citationDoc,result);
    	result.getCitations().put(c.getId(), c);
    	
    	for (OIdentifiable reifiedTRec : new OTraverse()
 			.field("in_"+ NdexClasses.Edge_E_citations)
 			.target(citationDoc)
 			.predicate( new OSQLPredicate("$depth <= 1"))) {

    		ODocument doc = (ODocument) reifiedTRec;

    		if ( doc.getClassName().equals(NdexClasses.Edge)) {
    			Edge e = getEdgeFromDocument(doc, result);
    			result.getEdges().put(e.getId(), e);
    		}
    	}
    	
    	// get orphan nodes and support
    	
    	for (OIdentifiable nodeRec : new OTraverse()
 			.field("in_"+ NdexClasses.Node_E_citations)
 			.target(citationDoc)
 			.predicate( new OSQLPredicate("$depth <= 1"))) {

    		ODocument nodeDoc = (ODocument) nodeRec;

    		if ( nodeDoc.getClassName().equals(NdexClasses.Node)){
    			Node n = this.getNode(nodeDoc,result);
    		    result.getNodes().put(n.getId(), n);	
    		}
    	}
    	
    	result.setEdgeCount(result.getEdges().size());
    	result.setNodeCount(result.getNodes().size());
    	
    	return result;
    }
    
    // returns a subnetwork that contains all the orphan supports (supports that have no citation links).
    public Network getOrphanSupportsNetwork(String networkUUID) throws NdexException {
//    	ODocument networkDoc = this.getRecordById(UUID.fromString(networkUUID), NdexClasses.Network);
    	Network result = new Network();
    	
    	ODocument netDoc = getNetworkDocByUUIDString(networkUUID);
    	
    	if ( netDoc == null)
    		return null;
    	
    	for (OIdentifiable supportRec : new OTraverse()
 			.field("out_"+ NdexClasses.Network_E_Supports)
 			.target(netDoc)
 			.predicate( new OSQLPredicate("$depth <= 1"))) {

    		ODocument doc = (ODocument) supportRec;

    		if ( doc.getClassName().equals(NdexClasses.Support) && 
    			  doc.field("out_" + NdexClasses.Support_E_citation) == null) {
    			Support s = getSupportFromDoc(doc, result);
    			result.getSupports().put(s.getId(), s);
    			
    			// get all the nodes
    	    	for (OIdentifiable nodeRec : new OTraverse()
    	    		.field("in_"+ NdexClasses.Node_E_supports)
    	    		.target(doc)
    	    		.predicate( new OSQLPredicate("$depth <= 1"))) {
    			
    	    		ODocument nodeDoc = (ODocument) nodeRec;
    	    		if ( nodeDoc.getClassName().equals(NdexClasses.Node)) {
    	    			Node n = getNode ( nodeDoc, result);
    	    			result.getNodes().put(n.getId(), n);
    	    		}
    	    		
    	    	}
    		
    	    	// get all the edges
    	    	for (OIdentifiable edgeRec : new OTraverse()
	    			.field("in_"+ NdexClasses.Edge_E_supports)
	    			.target(doc)
	    			.predicate( new OSQLPredicate("$depth <= 1"))) {
			
    	    		ODocument edgeDoc = (ODocument) edgeRec;
    	    		if ( edgeDoc.getClassName().equals(NdexClasses.Edge)) {
    	    			Edge e = getEdgeFromDocument ( edgeDoc, result);
    	    			result.getEdges().put(e.getId(), e);
    	    		}
	    		
    	    	}
    	    	
    		}
    	}
    	
    	result.setEdgeCount(result.getEdges().size());
    	result.setNodeCount(result.getNodes().size());
    	
    	return result;
    }
    


	private static BaseTerm getBaseTerm(ODocument o, Network network) {
		BaseTerm t = new BaseTerm();
		t.setId((long)o.field(NdexClasses.Element_ID));
		t.setName((String)o.field(NdexClasses.BTerm_P_name));
		
		ODocument nsDoc = o.field("out_"+NdexClasses.BTerm_E_Namespace);
		if ( nsDoc != null) {
			Long nsId = nsDoc.field(NdexClasses.Element_ID);
			t.setNamespaceId(nsId);
			
			if ( network != null &&
				 ! network.getNamespaces().containsKey(nsId)) {
				Namespace ns = getNamespace(nsDoc,network);
				network.getNamespaces().put(nsId, ns);
			}
		} else
			t.setNamespaceId(-1);
		
		return t;
	}
	
	//TODO: make a better implementation for this function.
	public ODocument getDocumentByElementId(long elementID) throws NdexException {
		ODocument result = getDocumentByElementId(NdexClasses.Node, elementID);
		if ( result != null) return result;
		
		result = getDocumentByElementId(NdexClasses.Edge, elementID);
		if ( result != null) return result;
		
		result = getDocumentByElementId(NdexClasses.BaseTerm, elementID);
		if ( result != null) return result;

		result = getDocumentByElementId(NdexClasses.Citation, elementID);
		if ( result != null) return result;
		result = getDocumentByElementId(NdexClasses.FunctionTerm, elementID);
		if ( result != null) return result;
		result = getDocumentByElementId(NdexClasses.Namespace, elementID);
		if ( result != null) return result;
		result = getDocumentByElementId(NdexClasses.ReifiedEdgeTerm, elementID);
		if ( result != null) return result;
		result = getDocumentByElementId(NdexClasses.Support, elementID);
		if ( result != null) return result;
		
		throw new NdexException ("ElementId " + elementID + " was not found in database.");
	}

    /**
     * Check if an account has a certain privilege on a network.
     * @param accountName account name to be checked.
     * @param UUIDStr  id of the network
     * @param permission  permission to be verified.
     * @return true if the account has that privilege.
     * @throws NdexException 
     * @throws ObjectNotFoundException 
     */
	
	public boolean checkPrivilege(String accountName, String UUIDStr, Permissions permission) throws ObjectNotFoundException, NdexException {
		
		ODocument d = this.getRecordByUUID(UUID.fromString(UUIDStr), NdexClasses.Network);
		
		String vstr = d.field(NdexClasses.Network_P_visibility);
		
		VisibilityType v = VisibilityType.valueOf(vstr);
		
		if ( v == VisibilityType.PUBLIC) return true;

		if ( accountName == null ) return false;
		return Helper.checkPermissionOnNetworkByAccountName(db,UUIDStr, accountName, permission);
	}
	
	
	private ODocument getDocumentByElementId(String NdexClassName, long elementID) {
		String query = "select from " + NdexClassName + " where " + 
		        NdexClasses.Element_ID + "=" +elementID;
	        final List<ODocument> nss = db.query(new OSQLSynchQuery<ODocument>(query));
	  
	        if (!nss.isEmpty())
	  	       return nss.get(0);
	         
	   /*      List<ORecordOperation> txOperations = db.getTransaction().getRecordEntriesByClass(NdexClassName);
	         for (ORecordOperation op : txOperations) {
	         	long id = ((ODocument) op.getRecord()).field(NdexClasses.Element_ID);
	         if (id == elementID)
	            return (ODocument) op.getRecord();
	         } */
	         return null;
	}

	
    private static NdexPropertyValuePair getNdexPropertyFromDocument(ODocument propDoc, Network n) {
		NdexPropertyValuePair p = new NdexPropertyValuePair();
		
		ODocument baseTermDoc = propDoc.field("out_" + NdexClasses.ndexProp_E_predicate);
		if ( baseTermDoc == null ) {
			p.setPredicateString((String)propDoc.field(NdexClasses.ndexProp_P_predicateStr));
		} else {
			BaseTerm bterm = getBaseTerm(baseTermDoc, n);
			if ( n!=null) {
				if ( ! n.getBaseTerms().containsKey(bterm.getId()))
					n.getBaseTerms().put(bterm.getId(), bterm);
			}
			p.setPredicateString(getBaseTermStrForBaseTerm(bterm,n));
			p.setPredicateId(bterm.getId());
		}
		
		p.setValue((String)propDoc.field(NdexClasses.ndexProp_P_value)) ;
		String dType = (String)propDoc.field(NdexClasses.ndexProp_P_datatype);
		if ( dType !=null)
			p.setDataType(dType);
		return p;
    	
    }
    
	private static String getBaseTermStrForBaseTerm(BaseTerm bterm, Network n) {
		String localName = bterm.getName();
		
		if ( bterm.getNamespaceId() > 0 && ( n != null )) {
			Namespace ns = n.getNamespaces().get(bterm.getNamespaceId());
			String prefix = ns.getPrefix();
			if ( prefix != null)
				return prefix + ":" + localName;
			return  ns.getUri() + localName;
		}
		return localName;
	}

    private static Namespace getNamespace(ODocument ns, Network network) {
        Namespace rns = new Namespace();
        rns.setId((long)ns.field("id"));
        rns.setPrefix((String)ns.field(NdexClasses.ns_P_prefix));
        rns.setUri((String)ns.field(NdexClasses.ns_P_uri));
        
        getPropertiesFromDocument(rns, ns,network);
        return rns;
     } 
     
    
    // set properties in the passed in object by the information stored in a db document. 
    public static void getPropertiesFromDocument(PropertiedObject obj, ODocument doc, Network n) {
    	for (OIdentifiable ndexPropertyDoc : new OTraverse()
    			.field("out_"+ NdexClasses.E_ndexProperties )
    			.target(doc)
    			.predicate( new OSQLPredicate("$depth <= 1"))) {

    		ODocument propDoc = (ODocument) ndexPropertyDoc;
  
    		if ( propDoc.getClassName().equals(NdexClasses.NdexProperty)) {
				
    			obj.getProperties().add( getNdexPropertyFromDocument(propDoc,n));
    		}
    	}

    	//Populate presentation properties
	
    	for (OIdentifiable ndexPropertyDoc : new OTraverse()
    			.field("out_"+ NdexClasses.E_ndexPresentationProps )
    			.target(doc)
    			.predicate( new OSQLPredicate("$depth <= 1"))) {

    		ODocument propDoc = (ODocument) ndexPropertyDoc;
  
    		if ( propDoc.getClassName().equals(NdexClasses.SimpleProperty)) {
				
    			obj.getPresentationProperties().add( Helper.getSimplePropertyFromDoc(propDoc));
    		}
    	}
    }
    
	public Network getNetworkById(UUID id) throws NdexException {
		ODocument nDoc = getNetworkDocByUUIDString(id.toString());

        if (nDoc==null) return null;
   
        Network network = new Network(); 

        setNetworkSummary(nDoc, network);
        
        for ( Namespace ns : NetworkDAO.getNamespacesFromNetworkDoc(nDoc, network)) {
        	network.getNamespaces().put(ns.getId(),ns);
        }

        // get all baseTerms
        for (OIdentifiable bTermDoc : new OTraverse()
    			.field("out_"+ NdexClasses.Network_E_BaseTerms )
    			.target(nDoc)
    			.predicate( new OSQLPredicate("$depth <= 1"))) {

        	ODocument doc = (ODocument) bTermDoc;

        	if ( doc.getClassName().equals(NdexClasses.BaseTerm) ) {
        		BaseTerm term = getBaseTerm(doc,network);
        		network.getBaseTerms().put(term.getId(), term);

        	}
        }

        
        for (OIdentifiable nodeDoc : new OTraverse()
        	.field("out_"+ NdexClasses.Network_E_Nodes )
        	.target(nDoc)
        	.predicate( new OSQLPredicate("$depth <= 1"))) {

        	ODocument doc = (ODocument) nodeDoc;

        	if ( doc.getClassName().equals(NdexClasses.Node) ) {
        		Node node = getNode(doc,network);
        		network.getNodes().put(node.getId(), node);
 
        	}
        }

        for (OIdentifiable nodeDoc : new OTraverse()
      	              	.field("out_"+ NdexClasses.Network_E_Edges )
      	              	.target(nDoc)
                      	.predicate( new OSQLPredicate("$depth <= 1"))) {

            ODocument doc = (ODocument) nodeDoc;
         
            if ( doc.getClassName().equals(NdexClasses.Edge) ) {
              	   Edge e = getEdgeFromDocument(doc,network);
              	   network.getEdges().put(e.getId(), e);
            	 
            }
        }
        
        return network;
	}
	
	
	// namespaceID < 0 means baseTerm has a local namespace
	public BaseTerm getBaseTerm(String baseterm, long namespaceID, String networkId) {
		List<ODocument> terms;
		
		if ( namespaceID >= 0 ) {
			//TODO: check out why this method throws exceptions from Orientdb.
		/*	OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>(baseTermQuery);
			terms = db.command(query).execute((Long)namespaceID, baseterm); */
			String query = "select from (traverse in_" + NdexClasses.BTerm_E_Namespace +
			  " from (select from " + NdexClasses.Namespace + " where " + 
			NdexClasses.Element_ID +"= "+ namespaceID+ ")) where @class='" +NdexClasses.BaseTerm + 
			     "' and "+NdexClasses.BTerm_P_name +  " ='" + baseterm +"'";
			terms = db.query(new OSQLSynchQuery<ODocument>(query));
			
		} else {
/*			OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>(baseTermQuery3);
			terms = db.command(query).execute( baseterm, networkId); */
			String query = "select from (traverse out_" + NdexClasses.Network_E_BaseTerms +
					" from (select from " + NdexClasses.Network + " where " +
			  		  NdexClasses.Network_P_UUID + "= '" + networkId + "')) where @class='"+  NdexClasses.BaseTerm + "' and " + 
				       NdexClasses.BTerm_P_name +  " ='" + baseterm + "'";
			terms = db.query(new OSQLSynchQuery<ODocument>(query));
		}
		     
		if (terms.isEmpty())
		    	 return null;
             return getBaseTerm(terms.get(0), null);
             
	}
	

	public Collection<BaseTerm> getBaseTerms(String networkUUID) {
		ArrayList<BaseTerm> baseTerms = new ArrayList<>();
		
		ODocument networkDoc = getNetworkDocByUUIDString(networkUUID);
		
		
        	for (OIdentifiable reifiedTRec : new OTraverse()
     			.field("in_"+ NdexClasses.Network_E_BaseTerms)
     			.target(networkDoc)
     			.predicate( new OSQLPredicate("$depth <= 1"))) {

        		ODocument doc = (ODocument) reifiedTRec;

        		if ( doc.getClassName().equals(NdexClasses.BaseTerm)) {
        			BaseTerm t = getBaseTerm(doc,null);
        			baseTerms.add(t);
        		}
        	}
    	  	
    	
    	return baseTerms;
    	
    }
	
	public Collection<Namespace> getNamespaces(String networkUUID) {
		ArrayList<Namespace> namespaces = new ArrayList<>();
		
		ODocument networkDoc = getNetworkDocByUUIDString(networkUUID);
		
		
        	for (OIdentifiable reifiedTRec : new OTraverse()
     			.field("out_"+ NdexClasses.Network_E_Namespace)
     			.target(networkDoc)
     			.predicate( new OSQLPredicate("$depth <= 1"))) {

        		ODocument doc = (ODocument) reifiedTRec;

        		if ( doc.getClassName().equals(NdexClasses.Namespace)) {
        			Namespace n = getNamespace(doc,null);
        			namespaces.add(n);
        		}
        	}
    	return namespaces;
	}

    public static NetworkSummary getNetworkSummary(ODocument doc) {
    	NetworkSummary networkSummary = new NetworkSummary();
    	setNetworkSummary(doc,networkSummary);
    	return networkSummary;
    }


	public NetworkSummary getNetworkSummaryById (String networkUUIDStr) {
		ODocument doc = getNetworkDocByUUIDString(networkUUIDStr);
		return getNetworkSummary(doc);
	}
 
	
	public boolean networkIsReadOnly(String networkUUIDStr) {
		ODocument doc = getNetworkDocByUUIDString(networkUUIDStr);
		Long commitId = doc.field(NdexClasses.Network_P_readOnlyCommitId );
		return commitId != null && commitId.longValue() >0 ;
	}
	
    public ODocument getNetworkDocByUUID(UUID id) {
    	return getNetworkDocByUUIDString(id.toString());
    }

    

    
	private static Citation getCitationFromDoc(ODocument doc, Network network) {
		Citation result = new Citation();
		result.setId((long)doc.field(NdexClasses.Element_ID));
		result.setTitle((String)doc.field(NdexClasses.Citation_P_title));
		result.setIdType((String)doc.field(NdexClasses.Citation_p_idType));
		result.setIdentifier((String)doc.field(NdexClasses.Citation_P_identifier));
		
		List<String> o = doc.field(NdexClasses.Citation_P_contributors);
		
		if ( o!=null && !o.isEmpty())
			result.setContributors(o);
		
		getPropertiesFromDocument(result,doc,network);

		return result;
	}


    private static Support getSupportFromDoc(ODocument doc, Network network) {
    	Support s = new Support();
    	s.setText((String)doc.field(NdexClasses.Support_P_text));
    	s.setId((long)doc.field(NdexClasses.Element_ID));
    	ODocument citationDoc = doc.field("out_" + NdexClasses.Support_E_citation);
    	if ( citationDoc != null) {
    		Long citationId = citationDoc.field(NdexClasses.Element_ID);
    		s.setCitationId(citationId);
    		if ( network != null && 
            		! network.getCitations().containsKey(citationId)) {
            	Citation citation = getCitationFromDoc(citationDoc, network);
            	network.getCitations().put(citationId, citation);
            }
    	}
        
		getPropertiesFromDocument(s,doc,network);

    	return s;
    	
    }

    
    //TODO: need to make sure the recursion doesn't form a loop.
    private FunctionTerm getFunctionTermfromDoc(ODocument doc,Network network) throws NdexException {
    	FunctionTerm term = new FunctionTerm();
    	
    	term.setId((long)doc.field(NdexClasses.Element_ID));

    	// get the functionTerm 
    	
    	ODocument baseTermDoc =doc.field("out_"+ NdexClasses.FunctionTerm_E_baseTerm);
    	BaseTerm functionNameTerm = getBaseTerm(baseTermDoc, network);
    	Long key = Long.valueOf(functionNameTerm.getId());
    	if ( network != null ) {
    		if ( !network.getBaseTerms().containsKey(key))
    			network.getBaseTerms().put(key, functionNameTerm);
    	}
    	
    	term.setFunctionTermId(functionNameTerm.getId());
    	// traverse for the argument
    	boolean isFirst= true; 
    	for (OIdentifiable parameterRec : new OTraverse()
 				.field("out_"+ NdexClasses.FunctionTerm_E_paramter )
 				.target(doc)
 				.predicate( new OSQLPredicate("$depth <= 1"))) {
    		ODocument parameterDoc = (ODocument) parameterRec;
    		if (isFirst ) {
    			isFirst = false;
    		} else {
    		   if ( network != null) { 
    		     if ( parameterDoc.getClassName().equals(NdexClasses.BaseTerm)) {
    			    BaseTerm t = getBaseTerm(parameterDoc, network);
    			    if ( !network.getBaseTerms().containsKey(t.getId()))
    			    	network.getBaseTerms().put(t.getId(), t);
    		     } else if(parameterDoc.getClassName().equals(NdexClasses.ReifiedEdgeTerm)) {
    		    	 ReifiedEdgeTerm t = 
    		    			 this.getReifiedEdgeTermFromDoc(parameterDoc, network);
    		    //	 if ( !network.getReifiedEdgeTerms().containsKey(t.getId())) {
    		    //		 network.getReifiedEdgeTerms().put(t.getId(), t);
    		    //	 }
    		     } else if ( parameterDoc.getClassName().equals(NdexClasses.FunctionTerm)) {
    		    	 FunctionTerm t = this.getFunctionTermfromDoc(parameterDoc, network);
    		    	 if ( !network.getFunctionTerms().containsKey(t.getId())) {
    		    		 network.getFunctionTerms().put(t.getId(), t);
    		    	 }
    		     }
    		   }
     		   Long argElementId = parameterDoc.field(NdexClasses.Element_ID);
     		   term.getParameterIds().add(argElementId);	
    		}
    	}
    	return term;
    }


    private ReifiedEdgeTerm getReifiedEdgeTermFromDoc(ODocument doc, Network network) throws NdexException {
    	ReifiedEdgeTerm term = new ReifiedEdgeTerm();
    	term.setId((long)doc.field(NdexClasses.Element_ID));
    	ODocument e = doc.field("out_" +NdexClasses.ReifiedEdge_E_edge );
    	term.setEdgeId((long)e.field(NdexClasses.Element_ID));
    	if ( network != null) {
    		if ( !network.getReifiedEdgeTerms().containsKey(term.getId()))
    			network.getReifiedEdgeTerms().put(term.getId(), term);
    		if ( !network.getEdges().containsKey(term.getEdgeId())) {
    			Edge edge = getEdgeFromDocument(e, network);
    			network.getEdges().put(edge.getId(), edge);
    		}
    	}
    		
    	return term;
    }
    
    private static NetworkSummary setNetworkSummary(ODocument doc, NetworkSummary nSummary) {
    	
		Helper.populateExternalObjectFromDoc (nSummary, doc);

    	nSummary.setName((String)doc.field(NdexClasses.Network_P_name));
    	nSummary.setDescription((String)doc.field(NdexClasses.Network_P_desc));
    	nSummary.setEdgeCount((int)doc.field(NdexClasses.Network_P_edgeCount));
    	nSummary.setNodeCount((int)doc.field(NdexClasses.Network_P_nodeCount));
    	nSummary.setVersion((String)doc.field(NdexClasses.Network_P_version));
        nSummary.setVisibility(VisibilityType.valueOf((String)doc.field(NdexClasses.Network_P_visibility)));
        Boolean isComplete = doc.field(NdexClasses.Network_P_isComplete);
        if ( isComplete != null)
        	nSummary.setIsComplete(isComplete.booleanValue());
        else 
        	nSummary.setIsComplete(false);
        
        nSummary.setEdgeCount((int)doc.field(NdexClasses.Network_P_edgeCount));

        Long ROcommitId = doc.field(NdexClasses.Network_P_readOnlyCommitId);
        if ( ROcommitId !=null)
        	nSummary.setReadOnlyCommitId(ROcommitId);
        
        Long ROCacheId = doc.field(NdexClasses.Network_P_cacheId);
        if ( ROCacheId !=null)
        	nSummary.setReadOnlyCacheId(ROCacheId);
        
/*        ODocument ud = doc.field("in_" + NdexClasses.E_admin);
        nSummary.setOwner((String)ud.field(NdexClasses.account_P_accountName));
*/        
        nSummary.setIsLocked((boolean)doc.field(NdexClasses.Network_P_isLocked));
        nSummary.setURI(NdexDatabase.getURIPrefix()+ "/network/" + nSummary.getExternalId().toString());

		NetworkSourceFormat fmt = Helper.getSourceFormatFromNetworkDoc(doc);
		if ( fmt !=null) {
			NdexPropertyValuePair p = new NdexPropertyValuePair(NdexClasses.Network_P_source_format,fmt.toString());
			nSummary.getProperties().add(p);
		}
        
		if ( nSummary instanceof Network ) {
			getPropertiesFromDocument(nSummary,doc, (Network)nSummary);
		} else 
			getPropertiesFromDocument(nSummary,doc, null);
		
		
        return nSummary;
    }


}
