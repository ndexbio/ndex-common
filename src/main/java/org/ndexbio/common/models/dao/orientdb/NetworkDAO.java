package org.ndexbio.common.models.dao.orientdb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexAOrientDBConnectionPool;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.model.object.NdexProperty;
import org.ndexbio.model.object.Permissions;
import org.ndexbio.model.object.PropertiedObject;
import org.ndexbio.model.object.ProvenanceEntity;
import org.ndexbio.model.object.User;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.Citation;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.FunctionTerm;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.Node;
import org.ndexbio.model.object.network.PropertyGraphEdge;
import org.ndexbio.model.object.network.PropertyGraphNetwork;
import org.ndexbio.model.object.network.PropertyGraphNode;
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
import com.orientechnologies.orient.core.db.record.ORecordOperation;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.filter.OSQLPredicate;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;

public class NetworkDAO extends OrientdbDAO {
	
	
	//flag to specify whether need to search in the current un-commited transaction. 
	//This is used to work around the problem that sql query doesn't search the current 
	// uncommited transaction in OriantDB.
	private boolean searchCurrentTx;
	
	private ObjectMapper mapper;
	private OrientGraph graph;
	
	static Logger logger = Logger.getLogger(NetworkDAO.class.getName());
	
	
	public NetworkDAO (ODatabaseDocumentTx db) {
	    super(db);
//		this.db = db;
		this.searchCurrentTx = false;
		mapper = new ObjectMapper();
		graph = new OrientGraph(this.db,false);
	}

	public NetworkDAO (ODatabaseDocumentTx db, boolean searchCurrentTransaction) {
	    super(db);
		this.searchCurrentTx = searchCurrentTransaction;
	}

/*	public NetworkDAO(NdexDatabase db, UUID networkID) {
		this.db = db;
		this.network = getNetworkById(networkID);
		
	} */
	
	
	/**
	 * Returns a subnetwork based on a block of edges selected from the network specified by networkUUID 
	 *     based on the specified blockSize and number of blocks to skip. It is intended to be used to 
	 *     incrementally "page" through a network by edges and forms the basis for operations like incremental copy. 
	 *     The returned network is fully poplulated and 'self-sufficient', including all nodes, terms, supports, 
	 *     citations, and namespaces referenced by the edges. 
	 *     The query selects a number of edges specified by the 'blockSize' parameter, 
	 *     starting at an offset specified by the 'skipBlocks' parameter. 
	 * @param networkID
	 * @param skipBlocks
	 * @param blockSize
	 * @return the subnetwork as a Network Object.   
	 * @throws NdexException 
	 */
	public Network getNetwork (UUID networkID, int skipBlocks, int blockSize) throws NdexException {
		ODocument nDoc = getNetworkDocByUUID(networkID);
		
	    if (nDoc == null) return null;

	    
	    int startPosition = skipBlocks * blockSize;
	    int counter = 0;
	    int endPosition = skipBlocks * blockSize + blockSize;

	    Network network = new Network(blockSize);  //result holder

        NetworkDAO.setNetworkSummary(nDoc, network);

        for (OIdentifiable nodeDoc : new OTraverse()
      	              	.field("out_"+ NdexClasses.Network_E_Edges )
      	              	.target(nDoc)
                      	.predicate( new OSQLPredicate("$depth <= 1"))) {
  

            ODocument doc = (ODocument) nodeDoc;
         
            if ( doc.getClassName().equals(NdexClasses.Edge) ) {

            	if ( counter >= endPosition) break;

                counter ++;
            	
            	if ( counter >= startPosition )  {
              	   Edge e = getEdgeFromDocument(doc,network);
              	   network.getEdges().put(e.getId(), e);
            	               
                }
            }
            
        }
        
        network.setEdgeCount(network.getEdges().size());
        network.setNodeCount(network.getNodes().size());
        
		 return network; 
	}
	
    /**
     * Check if an account has a certain privilege on a network.
     * @param accountName account name to be checked.
     * @param UUID  id of the network
     * @param permission  permission to be verified.
     * @return true if the account has that privilege.
     * @throws NdexException 
     * @throws ObjectNotFoundException 
     */
	
	public boolean checkPrivilege(String accountName, String UUIDStr, Permissions permission) throws ObjectNotFoundException, NdexException {
		
		ODocument d = this.getRecordById(UUID.fromString(UUIDStr), NdexClasses.Network);
		
		VisibilityType v = d.field(NdexClasses.Network_P_visibility);
		
		if ( v == VisibilityType.PUBLIC) return true;

		return Helper.checkPermissionOnNetworkByAccountName(db,UUIDStr, accountName, permission);
	}
	
	
	public int deleteNetwork (String UUID) {
		int counter = 0;
		
		String query = "traverse * from ( traverse out_networkNodes from (select from network where UUID='"
				+ UUID + "')) while @class <> 'network'";
        final List<ODocument> elements = db.query(new OSQLSynchQuery<ODocument>(query));
        
        for ( ODocument element : elements ) {
        	graph.removeVertex(graph.getVertex(element));
        	counter ++;
        }
		query = "select from network where UUID='"+ UUID + "'";
        final List<ODocument> networks = db.query(new OSQLSynchQuery<ODocument>(query));
        
        for ( ODocument n: networks ) {
        	graph.removeVertex(graph.getVertex(n));
        	counter ++;
        }
        
        db.commit();
		return counter;
	}
	
	public Network getNetworkById(UUID id) throws NdexException {
		ODocument nDoc = getNetworkDocByUUID(id);

        if (nDoc==null) return null;
   
        Network network = getNetwork(nDoc);
        
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
	
    
	public ODocument getNetworkDocByUUIDString(String id) {
	     String query = "select from " + NdexClasses.Network + " where UUID='"
                 +id+"'";
         final List<ODocument> networks = db.query(new OSQLSynchQuery<ODocument>(query));
  
         if (networks.isEmpty())
 	        return null;
         
    	return networks.get(0);
    }
 
	
    public ODocument getNetworkDocByUUID(UUID id) {
    	return getNetworkDocByUUIDString(id.toString());
    }

    
	public PropertyGraphNetwork getProperytGraphNetworkById (UUID networkID, int skipBlocks, int blockSize) throws JsonProcessingException, NdexException {
		ODocument nDoc = getNetworkDocByUUID(networkID);
		
	    if (nDoc == null) return null;
	    
	    PropertyGraphNetwork network = new PropertyGraphNetwork();
	    
		this.populatePropetyGraphNetworkFromDoc(network, nDoc);
	    
	    int startPosition = skipBlocks * blockSize;
	    int counter = 0;
	    int endPosition = skipBlocks * blockSize + blockSize;
	    
        
        TreeMap <ORID, String> termStringMap = new TreeMap<ORID,String> ();

        for (OIdentifiable nodeDoc : new OTraverse()
      	              	.field("out_"+ NdexClasses.Network_E_Edges )
      	              	.target(nDoc)
                      	.predicate( new OSQLPredicate("$depth <= 1"))) {
  

            ODocument doc = (ODocument) nodeDoc;
         
            if ( doc.getClassName().equals(NdexClasses.Edge) ) {

            	if ( counter >= endPosition) break;

                counter ++;
            	
            	if ( counter >= startPosition )  {
            	  fetchPropertyGraphEdgeToNetwork(doc,network, termStringMap);
                }
            } 	
        }
   	    return network; 
	}
    
    
    
	public PropertyGraphNetwork getProperytGraphNetworkById(UUID id) throws NdexException, JsonProcessingException {
		
		//TODO: populate citations and support
		
		ODocument networkDoc = getNetworkDocByUUID(id);
		
		if (networkDoc == null) return null;

		PropertyGraphNetwork network = new PropertyGraphNetwork();

		this.populatePropetyGraphNetworkFromDoc(network, networkDoc);
		
		TreeMap<ORID, String> termStringMap = new TreeMap<ORID, String>();
		
        Map<Long,PropertyGraphNode> nodeList = network.getNodes();
         for (OIdentifiable nodeDoc : new OTraverse()
       	    .field("out_"+ NdexClasses.Network_E_Nodes )
            .target(networkDoc)
            .predicate( new OSQLPredicate("$depth <= 1"))) {

              ODocument doc = (ODocument) nodeDoc;
          
              if ( doc.getClassName().equals(NdexClasses.Node)) {
          
                  PropertyGraphNode nd = getPropertyGraphNode(doc,network, termStringMap);
                  if ( nd != null)
                	  nodeList.put(nd.getId(),nd);
                  else
                	  throw new NdexException("Error occurred when getting node information from db "+ doc);
              }
         }
         
         for (OIdentifiable nodeDoc : new OTraverse()
       	    .field("out_"+ NdexClasses.Network_E_Edges )
            .target(networkDoc)
            .predicate( new OSQLPredicate("$depth <= 1"))) {

              ODocument doc = (ODocument) nodeDoc;
          
              if ( doc.getClassName().equals(NdexClasses.Edge)) {
          
                  this.fetchPropertyGraphEdgeToNetwork(doc, network, termStringMap);
              }
         }
         
		 return network; 
	}
	
	private void populatePropetyGraphNetworkFromDoc(PropertyGraphNetwork network, ODocument doc)
			throws JsonProcessingException {
        network.getProperties().add(new NdexProperty(
        		PropertyGraphNetwork.uuid, doc.field(NdexClasses.Network_P_UUID).toString()));
        
        network.getProperties().add(new NdexProperty(
        		PropertyGraphNetwork.name, (String)doc.field(NdexClasses.Network_P_name)));
        
        String desc = doc.field(NdexClasses.Network_P_desc);
        if ( desc != null) 
        	network.getProperties().add(new NdexProperty(PropertyGraphNetwork.description, desc));
        String version = doc.field(NdexClasses.Network_P_version);
        if ( version != null) 
        	network.getProperties().add(new NdexProperty(PropertyGraphNetwork.version, version));
        
        //namespace
        List<Namespace> nsList = new ArrayList<Namespace>();
        for (OIdentifiable nodeDoc : new OTraverse()
   	    	.field("out_"+ NdexClasses.Network_E_Namespace )
   	    	.target(doc)
   	    	.predicate( new OSQLPredicate("$depth <= 1"))) {

          ODocument nsDoc = (ODocument) nodeDoc;
      
          if ( nsDoc.getClassName().equals(NdexClasses.Namespace)) {
      
        	  nsList.add(getNamespace(nsDoc));
          }
          if ( ! nsList.isEmpty()) 
        	  network.getProperties().add(new NdexProperty(
            		PropertyGraphNetwork.namspaces, 
            		mapper.writeValueAsString(nsList)));
       }

       this.getPropertiesFromDocument(network,doc);
	}
	

	public void fetchPropertyGraphEdgeToNetwork(ODocument doc, PropertyGraphNetwork network,
					Map <ORID, String> termStringMap) throws NdexException {
		PropertyGraphEdge e = new PropertyGraphEdge();
		Long edgeId = doc.field(NdexClasses.Element_ID);
		e.setId(edgeId.longValue());

		ODocument predicateDoc = (ODocument)doc.field("out_"+NdexClasses.Edge_E_predicate);
		e.setPredicate(getBaseTermStringFromDoc(predicateDoc, termStringMap));

		// need to put the edge in the list first before recursion so that we don't run into dead loops.
		network.getEdges().put(edgeId, e);
		
		ODocument s =  doc.field("in_"+NdexClasses.Edge_E_subject);
		Long subjectId = s.field(NdexClasses.Element_ID);
		e.setSubjectId(subjectId );

		if ( network!=null && !network.getNodes().containsKey(subjectId)) {
			PropertyGraphNode n = getPropertyGraphNode(s,network, termStringMap);
			network.getNodes().put(n.getId(), n);
		}
		
		
		ODocument o   = doc.field("out_"+NdexClasses.Edge_E_object);
		Long objectId = o.field(NdexClasses.Element_ID);
		e.setObjectId(objectId);

		if ( network!=null && !network.getNodes().containsKey(objectId)) {
			PropertyGraphNode n = getPropertyGraphNode(o,network, termStringMap);
			network.getNodes().put(n.getId(), n);
		}
		
		getPropertiesFromDocument(e,doc);
	}

	private String getBaseTermStringFromDoc(ODocument doc, Map <ORID, String> termStringMap ) {
		String name = termStringMap.get(doc.getIdentity());
		if ( name != null) return name;
		
		name = doc.field(NdexClasses.BTerm_P_name);
		ODocument ns = doc.field(NdexClasses.BTerm_E_Namespace); 
		if (  ns != null ) {
			String prefix = ns.field(NdexClasses.ns_P_prefix);
			if ( prefix !=null)
				return prefix + ":" + name;
			return ns.field(NdexClasses.ns_P_uri) + name;
		}
		termStringMap.put(doc.getIdentity(), name);
		return name;
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

		getPropertiesFromDocument(e,doc);
		
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
					Citation citation = getCitationFromDoc(citationDoc);
					network.getCitations().put(citationId, citation);
				}
				e.getCitations().add(citationId);
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
				e.getSupports().add(supportId);
    		}
    	}

		return e;
	}
	
    private PropertyGraphNode getPropertyGraphNode(ODocument doc,PropertyGraphNetwork network,
    		Map <ORID, String> termStringMap ) throws NdexException {
    	PropertyGraphNode n = new PropertyGraphNode ();
        n.setId((long)doc.field(NdexClasses.Element_ID));    	
       
        //populate node name
        String name = doc.field(NdexClasses.Node_P_name);
        if ( name != null) {
        	n.getProperties().add(new NdexProperty(PropertyGraphNode.name, name));
        }
        
    	ODocument o = doc.field("out_" + NdexClasses.Node_E_represents);
    	if ( o != null) {
    		String repString = termStringMap.get(o.getIdentity());
    		if ( repString == null ) {
      		    String termType = o.getClassName();
    		
    			// populate objects in network
    			if ( termType.equals(NdexClasses.BaseTerm)) {
    				repString = this.getBaseTermStringFromDoc(o, termStringMap);
    			} else if (termType.equals(NdexClasses.ReifiedEdgeTerm)) {
    				repString = getReifiedTermStringFromDoc(o, network, termStringMap);
    			} else if (termType.equals(NdexClasses.FunctionTerm)) {
    				repString = getFunctionTermStringFromDoc(o,network,termStringMap);
    			} else 
    				throw new NdexException ("Unsupported term type '" + termType + 
    						"' found for term Id record:" + o.getIdentity().toString());
    		}
    		// original 
    		NdexProperty p = new NdexProperty( PropertyGraphNode.represents, repString);

    		n.getProperties().add(p);
    	}
		
    	//Populate properties
    	getPropertiesFromDocument(n, doc);
    	
		//TODO: populate citations etc.
   		
    	return n;
    }
    
    private String getReifiedTermStringFromDoc(ODocument doc, PropertyGraphNetwork network,
    		Map <ORID, String> termStringMap ) throws NdexException {
	
    	String name = termStringMap.get(doc.getIdentity());
		if ( name != null) return name;

        ODocument o = doc.field("out_"+NdexClasses.ReifedEdge_E_edge);
        Long edgeId = o.field(NdexClasses.Element_ID);
        
        name = PropertyGraphNetwork.reifiedEdgeTerm + "("+edgeId+")";
        termStringMap.put(doc.getIdentity(), name);
        
        if ( !network.getEdges().containsKey(edgeId)) {
        	this.fetchPropertyGraphEdgeToNetwork(o, network, termStringMap);
        }
        return name;
    }

    private String getFunctionTermStringFromDoc(ODocument doc, PropertyGraphNetwork network,
    		Map <ORID, String> termStringMap ) throws NdexException {
	
    	String name = termStringMap.get(doc.getIdentity());
		if ( name != null) return name;

		String baseTermStr = this.getBaseTermStringFromDoc(
				(ODocument)doc.field("out_"+NdexClasses.FunctionTerm_E_baseTerm), termStringMap);
		
		name = baseTermStr + "(";
		boolean isFirst = true;
		boolean isFirstArg = true;
    	for (OIdentifiable parameterRec : new OTraverse()
    		.field("out_"+ NdexClasses.FunctionTerm_E_paramter )
    		.target(doc)
    		.predicate( new OSQLPredicate("$depth <= 1"))) {

    		if ( isFirst) 
    			isFirst = false;
    		else {
    			ODocument parameterDoc = (ODocument) parameterRec;
    			String termType = parameterDoc.getClassName();
    			
    			String argStr;
    			if ( termType.equals(NdexClasses.BaseTerm)) {
    				argStr = this.getBaseTermStringFromDoc(parameterDoc, termStringMap);
    			} else if (termType.equals(NdexClasses.ReifiedEdgeTerm)) {
    				argStr = getReifiedTermStringFromDoc(parameterDoc, network, termStringMap);
    			} else if (termType.equals(NdexClasses.FunctionTerm)) {
    				argStr = getFunctionTermStringFromDoc(parameterDoc,network,termStringMap);
    			} else 
    				throw new NdexException ("Unsupported term type '" + termType + 
    						"' found for term Id record:" + parameterDoc.getIdentity().toString());

    			if ( isFirstArg) {
    				isFirstArg = false;
    			} else {
    				name += ",";
    			}
    			name += argStr;
    		}
    	}
		
		name += ")";
        return name;
    }
    
    
    // set properties in the passed in object by the information stored in a db document. 
    private void getPropertiesFromDocument(PropertiedObject obj, ODocument doc) {
    	for (OIdentifiable ndexPropertyDoc : new OTraverse()
    			.field("out_"+ NdexClasses.E_ndexProperties )
    			.target(doc)
    			.predicate( new OSQLPredicate("$depth <= 1"))) {

    		ODocument propDoc = (ODocument) ndexPropertyDoc;
  
    		if ( propDoc.getClassName().equals(NdexClasses.NdexProperty)) {
				
    			obj.getProperties().add( getNdexPropertyFromDoc(propDoc));
    		}
    	}

    	//Populate presentation properties
	
    	for (OIdentifiable ndexPropertyDoc : new OTraverse()
    			.field("out_"+ NdexClasses.E_ndexPresentationProps )
    			.target(doc)
    			.predicate( new OSQLPredicate("$depth <= 1"))) {

    		ODocument propDoc = (ODocument) ndexPropertyDoc;
  
    		if ( propDoc.getClassName().equals(NdexClasses.NdexProperty)) {
				
    			obj.getPresentationProperties().add( getNdexPropertyFromDoc(propDoc));
    		}
    	}
    	
    }
	
	private Network getNetwork(ODocument n) {
		
		Network result = new Network();
		
		setNetworkSummary(n, result);

		getPropertiesFromDocument(result,n);
		
		//TODO: populate all fields.
		
		return result;
	}
	
	// first parameter is namespaceID, second parameter is base term
	final static private String baseTermQuery = "select from (traverse in_" + NdexClasses.BTerm_E_Namespace +
			  " from (select from " + NdexClasses.Namespace + " where " + 
			NdexClasses.Element_ID +"= ?)) where @class='" +NdexClasses.BaseTerm + "' and "+NdexClasses.BTerm_P_name +
	    		 " =?"; 
	final static private String baseTermQuery2 = "select from " + NdexClasses.BaseTerm +
			  " where out_" + NdexClasses.BTerm_E_Namespace + " is null and "+NdexClasses.BTerm_P_name +
	    		 " =?"; 
	
	final static private String baseTermQuery3 = "select from (traverse out_" + NdexClasses.Network_E_BaseTerms +
			" from (select from " + NdexClasses.Network + " where " +
  		  NdexClasses.Network_P_UUID + "= ?)) where @class='"+  NdexClasses.BaseTerm + "' and " + 
	       NdexClasses.BTerm_P_name +  " =?";
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
             
       //TODO: need to check the current transaction.      
	}
	
	private BaseTerm getBaseTerm(ODocument o, Network network) {
		BaseTerm t = new BaseTerm();
		t.setId((long)o.field(NdexClasses.Element_ID));
		t.setName((String)o.field(NdexClasses.BTerm_P_name));
		
		ODocument nsDoc = o.field("out_"+NdexClasses.BTerm_E_Namespace);
		if ( nsDoc != null) {
			Long nsId = nsDoc.field(NdexClasses.Element_ID);
			t.setNamespace(nsId);
			
			if ( network != null &&
				 ! network.getNamespaces().containsKey(nsId)) {
				Namespace ns = getNamespace(nsDoc);
				network.getNamespaces().put(nsId, ns);
			}
		} else
			t.setNamespace(-1);
		
		return t;
	}
	
	public ODocument getDocumentByElementId(String NdexClassName, long elementID) {
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
             Namespace result = getNamespace(nss.get(0));
             return result;
	}
	
	static final private String citationQuery1 = 
			"select from " + NdexClasses.Citation + " where " +
		    		  NdexClasses.Citation_P_title + "= ? and "
		    		 + NdexClasses.Citation_p_idType + "=? and "
		    		 + NdexClasses.Citation_P_identifier + "= ?" ;
	
	public Citation getCitation(String title, String idType, String identifier, UUID networkID) {
/*		String query = "select from " + NdexClasses.Citation + " where " +
	    		  NdexClasses.Citation_P_title + "='" + title +"' " ;
	    final List<ODocument> citations = db.query(new OSQLSynchQuery<ODocument>(query)); */
		OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>(citationQuery1);
		final List<ODocument> citations = db.command(query)
				.execute(title,idType,identifier); 
	    
	    ODocument c = null;
        for (ODocument x : citations ) {
        	ODocument networkDoc = x.field("in_" + NdexClasses.Network_E_Citations);
        	String uuidStr = networkDoc.field(NdexClasses.Network_P_UUID);
        	if (networkID.toString().equals(uuidStr)) {
               	  c = x;
        		  break;
        	}
        }
        
        if (c == null) return null;
        
        // construct the object
        return getCitationFromDoc(c);
	}
	
	/**
	 *  This function returns the citations in this network.
	 * @param networkUUID
	 * @return
	 */
	public Collection<Citation> getNetworkCitations(String networkUUID) {
		ArrayList<Citation> citations = new ArrayList<Citation>();
		
		ODocument networkDoc = getNetworkDocByUUIDString(networkUUID);
		
    	for (OIdentifiable reifiedTRec : new OTraverse()
 			.field("out_"+ NdexClasses.Network_E_Citations )
 			.target(networkDoc)
 			.predicate( new OSQLPredicate("$depth <= 1"))) {

    		ODocument doc = (ODocument) reifiedTRec;

    		if ( doc.getClassName().equals(NdexClasses.Citation)) {
    			citations.add(this.getCitationFromDoc(doc));
    		}
    	}
    	return citations;
	}

	/**
	 * Get all citations (including citations relate to the network and citations that only 
	 * related to certain edges.) .  
	 * @param networkUUID
	 * @return
	 * @throws NdexException 
	 * @throws ObjectNotFoundException 
	 */
/*	
	public Collection<Citation> getNetworkCitationsAll(String networkUUID) throws ObjectNotFoundException, NdexException {
		Collection<Citation> citations = getNetworkCitations(networkUUID);
		
		TreeMap <OIdentifiable, Citation> citationMap = new TreeMap <OIdentifiable,Citation>();
		
		ODocument networkDoc = this.getRecordById(UUID.fromString(networkUUID), NdexClasses.Network);

    	for (OIdentifiable reifiedTRec : new OTraverse()
 			.fields("out_"+ NdexClasses.Network_E_Edges, "out_" + NdexClasses.Edge_E_citations )
 			.target( networkDoc)
 			.predicate( new OSQLPredicate("$depth <= 2"))) {

    		ODocument doc = (ODocument) reifiedTRec;

    		if ( doc.getClassName().equals(NdexClasses.Citation)) {
    			if ( !citationMap.containsKey(doc))
    				citationMap.put(doc , this.getCitationFromDoc(doc));
    		}
    	}
		
    	TreeSet<Citation> finalCitations = new TreeSet<Citation> ();
    	
    	finalCitations.addAll(citations);
    	
    	finalCitations.addAll(citationMap.values());
    	
		return finalCitations;
	}
*/	
	
	public Collection<Namespace> getNetworkNamespaces(String networkUUID) {
		ArrayList<Namespace> namespaces = new ArrayList<Namespace>();
		
		ODocument networkDoc = getNetworkDocByUUIDString(networkUUID);
		
    	for (OIdentifiable reifiedTRec : new OTraverse()
 			.field("out_"+ NdexClasses.Network_E_Namespace)
 			.target(networkDoc)
 			.predicate( new OSQLPredicate("$depth <= 1"))) {

    		ODocument doc = (ODocument) reifiedTRec;

    		if ( doc.getClassName().equals(NdexClasses.Namespace)) {
    			namespaces.add(getNamespace(doc));
    		}
    	}
    	return namespaces;
	}
	
	
	private Citation getCitationFromDoc(ODocument doc) {
		Citation result = new Citation();
		result.setId((long)doc.field(NdexClasses.Element_ID));
		result.setTitle((String)doc.field(NdexClasses.Citation_P_title));
		result.setIdType((String)doc.field(NdexClasses.Citation_p_idType));
		result.setIdentifier((String)doc.field(NdexClasses.Citation_P_identifier));
		
		List<String> o = doc.field(NdexClasses.Citation_P_contributors);
		
		if ( o!=null && !o.isEmpty())
			result.setContributors(o);
		
		getPropertiesFromDocument(result,doc);

		return result;
	}
	
	
	
	private static NdexProperty getNdexPropertyFromDoc(ODocument doc) {
		NdexProperty p = new NdexProperty();
		p.setPredicateString((String)doc.field(NdexClasses.ndexProp_P_predicateStr));
		p.setValue((String)doc.field(NdexClasses.ndexProp_P_value)) ;
    	p.setDataType((String)doc.field(NdexClasses.ndexProp_P_datatype));
		return p;

	}
	
/*	//This function is no longer needed.
	public ODocument getNamespaceDocByEId (long elementID) {
		String query = "select from " + NdexClasses.Namespace + " where " + 
	        NdexClasses.Element_ID + "=" +elementID;
        final List<ODocument> nss = db.query(new OSQLSynchQuery<ODocument>(query));
  
        if (!nss.isEmpty())
 	       return nss.get(0);
        
        List<ORecordOperation> txOperations = db.getTransaction().getRecordEntriesByClass(NdexClasses.Namespace);
        for (ORecordOperation op : txOperations) {
        	long id = ((ODocument) op.getRecord()).field(NdexClasses.Element_ID);
        if (id == elementID)
           return (ODocument) op.getRecord();
        }
        return null;
	}
*/	
    private Namespace getNamespace(ODocument ns) {
       Namespace rns = new Namespace();
       rns.setId((long)ns.field("id"));
       rns.setPrefix((String)ns.field(NdexClasses.ns_P_prefix));
       rns.setUri((String)ns.field(NdexClasses.ns_P_uri));
       
       getPropertiesFromDocument(rns, ns);
       return rns;
    } 
    
    private static final String nodeQuery = "select from (traverse in_" + 
         NdexClasses.Node_E_represents + " from (select from "+ NdexClasses.BaseTerm + " where " +
         NdexClasses.Element_ID + " = ?)) where @class='"+ NdexClasses.Node +"'";
    
    public Node findNodeByBaseTermId(long baseTermID) throws NdexException {
		OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>(nodeQuery);
		List<ODocument> nodes = db.command(query).execute( baseTermID);
    	
		if (nodes.isEmpty())
			return null;
		
    	return getNode(nodes.get(0), null);
    }

    private static final String functionTermNodeQuery = 
    		"select from (traverse in_" + NdexClasses.Node_E_represents + 
    		" from (select from "+ NdexClasses.FunctionTerm + " where " +
            NdexClasses.Element_ID + " = ?)) where @class='"+ NdexClasses.Node +"'";
       
    public Node findNodeByFunctionTermId(long functionTermID) throws NdexException {
   		OSQLSynchQuery<ODocument> query = 
   				new OSQLSynchQuery<ODocument>(functionTermNodeQuery);
   		List<ODocument> nodes = db.command(query).execute( functionTermID);
       	
   		if (nodes.isEmpty())
   			return null;
   		
       	return getNode(nodes.get(0),null);
    }
    
    private static final String reifedEdgeTermNodeQuery = 
    		"select from (traverse in_" + NdexClasses.Node_E_represents + 
    		" from (select from "+ NdexClasses.ReifiedEdgeTerm + " where " +
            NdexClasses.Element_ID + " = ?)) where @class='"+ NdexClasses.Node +"'";

    public Node findNodeByReifiedEdgeTermId (long reifiedEdgeTermId) throws NdexException {
   		OSQLSynchQuery<ODocument> query = 
   				new OSQLSynchQuery<ODocument>(reifedEdgeTermNodeQuery);
   		List<ODocument> nodes = db.command(query).execute( reifiedEdgeTermId);
       	
   		if (nodes.isEmpty())
   			return null;
   		
       	return getNode(nodes.get(0),null);
    }
    
    private static final String refiedEdgeTermQuery = 
    		"select from (traverse in_" + NdexClasses.ReifedEdge_E_edge + 
    		" from (select from "+ NdexClasses.Edge + " where " +
            NdexClasses.Element_ID + " = ?)) where @class='"+ 
    		NdexClasses.ReifiedEdgeTerm +"'";
       
    public ReifiedEdgeTerm findReifiedEdgeTermByEdgeId(long edgeId) {
   		OSQLSynchQuery<ODocument> query = 
   				new OSQLSynchQuery<ODocument>(refiedEdgeTermQuery);
   		List<ODocument> nodes = this.db.command(query).execute( edgeId);
       	
   		if (!nodes.isEmpty()) {
   			ReifiedEdgeTerm t = new ReifiedEdgeTerm();
   			t.setId((long)nodes.get(0).field(NdexClasses.Element_ID));
   			t.setEdgeId(edgeId);
   			return t;
   		}

   		if ( this.searchCurrentTx ) {
	         List<ORecordOperation> txOperations = db.getTransaction().getRecordEntriesByClass(NdexClasses.Edge);
	         for (ORecordOperation op : txOperations) {
	         	long id = ((ODocument) op.getRecord()).field(NdexClasses.Element_ID);
	            if (id == edgeId) {
	            	for (OIdentifiable reifiedTRec : new OTraverse()
      	       	    			.field("in_"+ NdexClasses.ReifedEdge_E_edge )
      	       	    			.target((ODocument) op.getRecord())
      	       	    			.predicate( new OSQLPredicate("$depth <= 1"))) {

	       				ODocument doc = (ODocument) reifiedTRec;
       	          
	       				if ( doc.getClassName().equals(NdexClasses.ReifiedEdgeTerm)) {
	       		   				ReifiedEdgeTerm t = new ReifiedEdgeTerm();
	       		   				t.setId((long)doc.field(NdexClasses.Element_ID));
	       		   				t.setEdgeId(edgeId);
	       						return t;
	       					
	       				}
	       			}
	            }
	         }
   		}
       	return null;
    }
    /*
    private ReifiedEdgeTerm getReifiedTermFromDocument(ODocument doc) {
    	ReifiedEdgeTerm rt = new ReifiedEdgeTerm();
    	rt.setEdgeId(termEdge);
    } */
    
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
     	getPropertiesFromDocument(n, nodeDoc);

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
    					ReifiedEdgeTerm reTerm = getReifiedEdgeTermFromDoc(o);
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
 				.field("out_"+ NdexClasses.Node_E_ciations )
 				.target(nodeDoc)
 				.predicate( new OSQLPredicate("$depth <= 1"))) {
    		ODocument doc = (ODocument) citationRec;
    		if ( doc.getClassName().equals(NdexClasses.Citation)) {
        		Long citationId = doc.field(NdexClasses.Element_ID);
				if ( network != null &&
						!network.getCitations().containsKey(citationId)) {
					Citation citation = getCitationFromDoc(doc);
					network.getCitations().put(citationId, citation);
				}
				n.getCitations().add(citationId);
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
				n.getSupports().add(supportId);
    		}
    	}

    	return n;
    }
    
    //TODO: need to make sure the recursion doesn't form a loop.
    private FunctionTerm getFunctionTermfromDoc(ODocument doc,Network network) {
    	FunctionTerm term = new FunctionTerm();
    	
    	term.setId((long)doc.field(NdexClasses.Element_ID));

    	// get the functionTerm 
    	
    	ODocument baseTermDoc =doc.field("out_"+ NdexClasses.FunctionTerm_E_baseTerm);
    	BaseTerm functionNameTerm = this.getBaseTerm(baseTermDoc, network);
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
    		    	 ReifiedEdgeTerm t = this.getReifiedEdgeTermFromDoc(parameterDoc);
    		    	 if ( !network.getReifiedEdgeTerms().containsKey(t.getId())) {
    		    		 network.getReifiedEdgeTerms().put(t.getId(), t);
    		    	 }
    		     } else if ( parameterDoc.getClassName().equals(NdexClasses.FunctionTerm)) {
    		    	 FunctionTerm t = this.getFunctionTermfromDoc(parameterDoc, network);
    		    	 if ( !network.getFunctionTerms().containsKey(t.getId())) {
    		    		 network.getFunctionTerms().put(t.getId(), t);
    		    	 }
    		     }
    		   }
     		   Long argElementId = parameterDoc.field(NdexClasses.Element_ID);
     		   term.getParameters().add(argElementId);	
    		}
    	}
    	return term;
    }
 
    private ReifiedEdgeTerm getReifiedEdgeTermFromDoc(ODocument doc) {
    	ReifiedEdgeTerm term = new ReifiedEdgeTerm();
    	term.setId((long)doc.field(NdexClasses.Element_ID));
    	ODocument e = doc.field("out_" +NdexClasses.ReifedEdge_E_edge );
    	term.setEdgeId((long)e.field(NdexClasses.Element_ID));
    	return term;
    }
    
    private static NetworkSummary setNetworkSummary(ODocument doc, NetworkSummary nSummary) {
    	nSummary.setCreationDate((Date)doc.field(NdexClasses.ExternalObj_cDate));
    	nSummary.setExternalId(UUID.fromString((String)doc.field(NdexClasses.Network_P_UUID)));
    	nSummary.setName((String)doc.field(NdexClasses.Network_P_name));
    	nSummary.setDescription((String)doc.field(NdexClasses.Network_P_desc));
    	
    	nSummary.setModificationDate((Date)doc.field(NdexClasses.ExternalObj_mDate));
    	nSummary.setEdgeCount((int)doc.field(NdexClasses.Network_P_edgeCount));
    	nSummary.setNodeCount((int)doc.field(NdexClasses.Network_P_nodeCount));
    	nSummary.setVersion((String)doc.field(NdexClasses.Network_P_version));
        nSummary.setVisibility(VisibilityType.valueOf((String)doc.field(NdexClasses.Network_P_visibility)));

        return nSummary;
    }
    
    public static NetworkSummary getNetworkSummary(ODocument doc) {
    	NetworkSummary networkSummary = new NetworkSummary();
    	return setNetworkSummary(doc,networkSummary);
    }
    
    public Citation getCitationById(long elementId) {
    	ODocument doc = getDocumentByElementId(NdexClasses.Citation,elementId);
    	if ( doc == null) return null;
    	return getCitationFromDoc(doc);
    	
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
        
    	if ( this.searchCurrentTx) {
	         List<ORecordOperation> txOperations = db.getTransaction().getRecordEntriesByClass(NdexClasses.Citation);
	         for (ORecordOperation op : txOperations) {
	         	long id = ((ODocument) op.getRecord()).field(NdexClasses.Element_ID);
	            if (id == citationId) {
	            	for (OIdentifiable supportRec : new OTraverse()
       	       	    			.field("in_"+ NdexClasses.Support_E_citation )
       	       	    			.target((ODocument) op.getRecord())
       	       	    			.predicate( new OSQLPredicate("$depth <= 1"))) {

	       				ODocument doc = (ODocument) supportRec;
        	          
	       				if ( doc.getClassName().equals(NdexClasses.Support)) {
	       					if ( doc.field(NdexClasses.Support_P_text).equals(text) ) {
	       						return getSupportFromDoc(doc,null);
	       					}
	       				}
	       			}
	        	    
	            }
	         }
    	}
    	return null;
    }
    
    private Support getSupportFromDoc(ODocument doc, Network network) {
    	Support s = new Support();
    	s.setText((String)doc.field(NdexClasses.Support_P_text));
    	s.setId((long)doc.field(NdexClasses.Element_ID));
    	ODocument citationDoc = doc.field("out_" + NdexClasses.Support_E_citation);
    	Long citationId = citationDoc.field(NdexClasses.Element_ID);
    	s.setCitation(citationId);
        if ( network != null && 
        		! network.getCitations().containsKey(citationId)) {
        	Citation citation = getCitationFromDoc(citationDoc);
        	network.getCitations().put(citationId, citation);
        }
    	return s;
    	
    }

    
    private static final String functionTermQuery = "select from (traverse in_" + 
            NdexClasses.FunctionTerm_E_baseTerm + " from (select from "+ NdexClasses.BaseTerm + " where " +
            NdexClasses.Element_ID + " = ?)) where @class='"+ NdexClasses.FunctionTerm +"'";

    // input parameter is a "rawFunctionTerm", which as elementid = -1;
    // This function will find the correspondent FunctionTerm from db.
    public FunctionTerm getFunctionTerm(FunctionTerm func) {
   		OSQLSynchQuery<ODocument> query = 
   				new OSQLSynchQuery<ODocument>(functionTermQuery);
   		List<ODocument> nodes = db.command(query).execute( func.getFunctionTermId());
       	
   		if (nodes.isEmpty())
   			return null;
   	
   		// check the parameters.
   		for ( ODocument n : nodes ) {
   			int counter = 0;
   			for (OIdentifiable parameterRec : new OTraverse()
	       	    .field("out_"+ NdexClasses.FunctionTerm_E_paramter )
	            .target(n)
	            .predicate( new OSQLPredicate("$depth <= 1"))) {

             ODocument doc = (ODocument) parameterRec;
	         
             String clsName = doc.getClassName();
             if ( clsName.equals(NdexClasses.BaseTerm) ||
            	  clsName.equals(NdexClasses.ReifiedEdgeTerm) || 
            	  clsName.equals(NdexClasses.FunctionTerm)) {
            	 if ( doc.field(NdexClasses.Element_ID).equals(func.getParameters().get(counter)) ) {
            		 counter ++;
            	 } else 
            		 break;
	         
             }
   			}
   			if ( counter == func.getParameters().size()) {
   				FunctionTerm result = new FunctionTerm();
   				result.setId((long)n.field(NdexClasses.Element_ID));
   				result.setFunctionTermId(func.getFunctionTermId());
   				for (Long pid : func.getParameters()) 
   				  result.getParameters().add(pid);
   				
   				return result;
   				
   			}
   		}
   		
   		return null;
    }
    
/*    public FunctionTerm getFunctionTermFromDoc(ODocument doc) {
    	FunctionTerm result = new FunctionTerm();
    	result.setId((long)doc.field(NdexClasses.Element_ID));
    	
    	
    	return result;
    } */
 
    /**
     * 
     * @param name
     * @param networkId
     * @return
     * @throws NdexException 
     */
    public List<Node> findNodesByName(String name, String networkId) throws NdexException {
    	String query = "select from (traverse out_" + NdexClasses.Network_E_Nodes +
				" from (select from " + NdexClasses.Network + " where " +
		  		  NdexClasses.Network_P_UUID + "= '" + networkId + "')) where @class='"+  NdexClasses.Node + "' and " + 
			       NdexClasses.Node_P_name +  " ='" + name + "'";	
    	
	    final List<ODocument> nodes = db.query(new OSQLSynchQuery<ODocument>(query));
	    
    	List<Node> results = new ArrayList<Node>();
	    if ( !nodes.isEmpty()) {
	    	for ( ODocument doc : nodes) {
	    		results.add(getNode(doc,null));
	    	}
	    	return results;
	    }
	    
	    //TODO: check the current Transaction.
	    return results;
    }
    
	public Collection<BaseTerm> getBaseTerms(String networkUUID) {
		ArrayList<BaseTerm> baseTerms = new ArrayList<BaseTerm>();
		
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
    
    public Collection<BaseTerm> getBaseTermsByPrefix(String networkUUID, String nsPrefix) {
		ArrayList<BaseTerm> baseTerms = new ArrayList<BaseTerm>();
		
		ODocument networkDoc = getNetworkDocByUUIDString(networkUUID);
		
		ODocument nsdoc = null;
    	for (OIdentifiable reifiedTRec : new OTraverse()
 			.field("out_"+ NdexClasses.Network_E_Namespace)
 			.target(networkDoc)
 			.predicate( new OSQLPredicate("$depth <= 1"))) {

    		ODocument doc = (ODocument) reifiedTRec;

    		if ( doc.getClassName().equals(NdexClasses.Namespace)) {
    			Namespace ns1 = getNamespace(doc);
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
    	Citation c = this.getCitationFromDoc(citationDoc);
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
    	result.setEdgeCount(result.getEdges().size());
    	result.setNodeCount(result.getNodes().size());
    	
    	return result;
    }
    
    public int grantPrivilege(String networkUUID, String accountUUID, Permissions permission) throws NdexException {
    	// check if the edge already exists?

    	Permissions p = Helper.getNetworkPermissionByAccout(db,networkUUID, accountUUID);

        if ( p!=null && p == permission) {
        	logger.info("Permission " + permission + " already exists between account " + accountUUID + 
        			 " and network " + networkUUID + ". Igore grant request."); 
        	return 0;
        }
        
        //check if this network has other admins
        if ( permission != Permissions.ADMIN && !Helper.canRemoveAdmin(db, networkUUID, accountUUID)) {
        	
        	throw new NdexException ("Privilege change failed. Network " + networkUUID +" will not have an administrator if permission " +
        	    permission + " are granted to account " + accountUUID);
        }
        
        ODocument networkdoc = this.getNetworkDocByUUID(UUID.fromString(networkUUID));
        ODocument accountdoc = this.getRecordById(UUID.fromString(accountUUID), NdexClasses.Account);
        OrientVertex networkV = graph.getVertex(networkdoc);
        OrientVertex accountV = graph.getVertex(accountdoc);
        
        for ( com.tinkerpop.blueprints.Edge e : accountV.getEdges(networkV, Direction.OUT)) { 
        		                   //NdexClasses.E_admin, NdexClasses.account_E_canEdit,NdexClasses.account_E_canRead)) {
          	graph.removeEdge(e);
        }

        accountV.addEdge(permission.toString().toLowerCase(), networkV);
    	return 1;
    }

    public int revokePrivilege(String networkUUID, String accountUUID) throws NdexException {
    	// check if the edge exists?

    	Permissions p = Helper.getNetworkPermissionByAccout(this.db,networkUUID, accountUUID);

        if ( p ==null ) {
        	logger.info("Permission doesn't exists between account " + accountUUID + 
        			 " and network " + networkUUID + ". Igore revoke request."); 
        	return 0;
        }
        
        //check if this network has other admins
        if ( p == Permissions.ADMIN && !Helper.canRemoveAdmin(this.db, networkUUID, accountUUID)) {
        	
        	throw new NdexException ("Privilege revoke failed. Network " + networkUUID +" only has account " + accountUUID
        			+ " as the administrator.");
        }
        
        ODocument networkdoc = this.getNetworkDocByUUID(UUID.fromString(networkUUID));
        ODocument accountdoc = this.getRecordById(UUID.fromString(accountUUID), NdexClasses.Account);
        OrientVertex networkV = graph.getVertex(networkdoc);
        OrientVertex accountV = graph.getVertex(accountdoc);
        
        for ( com.tinkerpop.blueprints.Edge e : accountV.getEdges(networkV, Direction.OUT)) { 
        		                   //NdexClasses.E_admin, NdexClasses.account_E_canEdit,NdexClasses.account_E_canRead)) {
          	graph.removeEdge(e);
          	break;
        }

    	return 1;
    }

	public ProvenanceEntity getProvenance(UUID networkId) throws JsonParseException, JsonMappingException, IOException {
		// get the network document
		ODocument nDoc = getNetworkDocByUUID(networkId);
		// get the provenance string
		String provenanceString = nDoc.field("provenance");
		// deserialize it to create a ProvenanceEntity object
		return mapper.readValue(provenanceString, ProvenanceEntity.class);
	}
    
	public int setProvenance(UUID networkId, ProvenanceEntity provenance) throws JsonProcessingException {
		// get the network document
		ODocument nDoc = getNetworkDocByUUID(networkId);	
		// serialize the ProvenanceEntity
		String provenanceString = mapper.writeValueAsString(provenance);
		// store provenance string
		nDoc.field("provenance", provenanceString);
				
		return 1;
	}

	public void rollback() {
		graph.rollback();		
	}

	public void commit() {
		graph.commit();
		
	}
    
}



