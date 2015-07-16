/**
 * Copyright (c) 2013, 2015, The Regents of the University of California, The Cytoscape Consortium
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package org.ndexbio.common.models.dao.orientdb;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.NetworkSourceFormat;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.model.exceptions.*;
import org.ndexbio.model.object.Membership;
import org.ndexbio.model.object.MembershipType;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.Permissions;
import org.ndexbio.model.object.PropertiedObject;
import org.ndexbio.model.object.SimplePropertyValuePair;
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
import org.ndexbio.model.object.network.VisibilityType;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.command.traverse.OTraverse;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.db.record.ridbag.ORidBag;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.filter.OSQLPredicate;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import com.orientechnologies.orient.core.id.ORID;

public class NetworkDAO extends NetworkDocDAO {
		
	private OrientGraph graph;	
	
	private static final int CLEANUP_BATCH_SIZE = 50000;
	
    private static final String[] networkElementType = {NdexClasses.Network_E_BaseTerms, NdexClasses.Network_E_Nodes, NdexClasses.Network_E_Citations,
    		NdexClasses.Network_E_Edges, NdexClasses.Network_E_FunctionTerms, NdexClasses.Network_E_Namespace,
    		NdexClasses.Network_E_ReifiedEdgeTerms, NdexClasses.Network_E_Supports,
    		NdexClasses.E_ndexPresentationProps, NdexClasses.E_ndexProperties
    		};
	
	static Logger logger = Logger.getLogger(NetworkDAO.class.getName());
	
	
	public NetworkDAO (ODatabaseDocumentTx db) {
	    super(db);
		graph = new OrientGraph(this.db,false);
		graph.setAutoScaleEdgeType(true);
		graph.setEdgeContainerEmbedded2TreeThreshold(40);
		graph.setUseLightweightEdges(true);

	}

	public NetworkDAO () throws NdexException {
	    this(NdexDatabase.getInstance().getAConnection());
	}

		
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
	

	
	public int deleteNetwork (String UUID) throws ObjectNotFoundException, NdexException {
		int counter = 0, cnt = 0;
		
		do {
			cnt = cleanupDeleteNetwork(UUID);
			if (cnt <0 ) 
				counter += -1*cnt;
			else 
				counter += cnt;
		} while ( cnt < 0 ); 
 		return counter;
	}
	
	/** 
	 * Delete up to CLEANUP_BATCH_SIZE vertices in a network. This function is for cleaning up a logically 
	 * deleted network in the database. 
	 * @param uuid
	 * @return Number of vertices being deleted. If the returned number is negative, it means the elements
	 * of the network are not completely deleted yet, and the number of vertices deleted are abs(returned number).
	 * @throws ObjectNotFoundException
	 * @throws NdexException
	 */
	public int cleanupDeleteNetwork(String uuid) throws ObjectNotFoundException, NdexException {
		ODocument networkDoc = getRecordByUUID(UUID.fromString(uuid), NdexClasses.Network);
		
		int count = cleanupNetworkElements(networkDoc);
		if ( count >= CLEANUP_BATCH_SIZE) {
			return (-1) * count;
		}
		
		// remove the network node.
		networkDoc.reload();
		
		for	(int retry = 0;	retry <	maxRetries;	++retry)	{
			try	{
				graph.removeVertex(graph.getVertex(networkDoc));
				break;
			} catch(ONeedRetryException	e)	{
				logger.warning("Retry: "+ e.getMessage());
				networkDoc.reload();
			}
		}
		
		return count++;
	}
	

	/**
	 * Delete up to CLEANUP_BATCH_SIZE vertices in a network. This function is for cleaning up a logically 
	 * deleted network in the database. 
	 * @param networkDoc
	 * @return the number of vertices being deleted. 
	 * @throws NdexException 
	 * @throws ObjectNotFoundException 
	 */
	private int cleanupNetworkElements(ODocument networkDoc) throws ObjectNotFoundException, NdexException {
        int counter = 0;

        
        for ( String fieldName : networkElementType) {
        	counter = cleanupElementsByEdge(networkDoc, fieldName, counter);
        	if ( counter >= CLEANUP_BATCH_SIZE) {
        		return counter;
        	}
        }
        
        return counter;
	}
	
	/**
	 * Cleanup up to CLEANUP_BATCH_SIZE vertices in the out going edge of fieldName. 
	 * @param doc The ODocument record to be clean up on.
	 * @param fieldName
	 * @param currentCounter
	 * @return the number of vertices being deleted. 
	 */
	private int cleanupElementsByEdge(ODocument doc, String fieldName, int currentCounter) {
		
		Object f = doc.field("out_"+fieldName);
		if ( f != null ) {
			if ( f instanceof ORidBag ) {
				ORidBag e = (ORidBag)f;
				int counter = currentCounter;
				for ( OIdentifiable rid : e) {
					counter = cleanupElement((ODocument)rid, counter);
					if ( counter >= CLEANUP_BATCH_SIZE) {
						return counter;
					}
				}
				return  counter;
			} 
			return cleanupElement((ODocument)f, currentCounter);
		}
		return currentCounter;
	}
	
	private int cleanupElement(ODocument doc, int currentCount) {
		int counter = currentCount;
		if (!doc.getClassName().equals(NdexClasses.NdexProperty) && 
				!doc.getClassName().equals(NdexClasses.SimpleProperty)) { // not NdexProperty or SimpleProperty Vertex
			counter = cleanupElementsByEdge(doc, NdexClasses.E_ndexProperties, counter);  // cleanup Properties
			counter = cleanupElementsByEdge(doc, NdexClasses.E_ndexPresentationProps, counter); 
		}
		doc.reload();

		for	(int retry = 0;	retry <	maxRetries;	++retry)	{
			try	{
				graph.removeVertex(graph.getVertex(doc));
				break;
			} catch(ONeedRetryException	e)	{
				logger.warning("Retry: "+ e.getMessage());
				doc.reload();
			}
		}
		counter ++;
		if ( counter % 2000 == 0 ) {
			graph.commit();
			if (counter % 10000 == 0 ) {
				logger.info("Deleted " + counter + " vertexes from network during cleanup.");
			}
		}
		return counter;
	}

	public int logicalDeleteNetwork (String uuid) throws ObjectNotFoundException, NdexException {
		ODocument networkDoc = getRecordByUUID(UUID.fromString(uuid), NdexClasses.Network);

		if ( networkDoc != null) {
		   networkDoc.fields(NdexClasses.ExternalObj_isDeleted,true,
				   NdexClasses.ExternalObj_mTime, new Date()).save();
		}
 		return 1;
	}
	
	
	public int deleteNetworkElements(String UUID) {
		int counter = 0;
		
		String query = "traverse * from ( traverse out_networkNodes,out_BaseTerms,out_networkNS from (select from network where UUID='"
				+ UUID + "')) while @class <> 'network'";
        final List<ODocument> elements = db.query(new OSQLSynchQuery<ODocument>(query));
        
        for ( ODocument element : elements ) {
        	element.reload();
        	graph.removeVertex(graph.getVertex(element));
        	counter ++;
        	if ( counter % 1500 == 0 ) {
        		graph.commit();
        		if (counter % 6000 == 0 ) {
        			logger.info("Deleted " + counter + " vertexes from network during cleanup." + UUID);
        		}
        	}

        }
        return counter;
	}
	
	/** 
	 * delete all ndex and presentation properties from a network record.
	 * Properities on network elements won't be deleted.
	 */
	public void deleteNetworkProperties(ODocument networkDoc) {

        for (OIdentifiable propertyDoc : new OTraverse()
    	.field("out_"+ NdexClasses.E_ndexProperties )
    	.target(networkDoc)
    	.predicate( new OSQLPredicate("$depth <= 1"))) {

        	ODocument doc = (ODocument) propertyDoc;

        	if ( doc.getClassName().equals(NdexClasses.NdexProperty) ) {
        		graph.removeVertex(graph.getVertex(doc));
        	}
        }

        for (OIdentifiable propertyDoc : new OTraverse()
    	.field("out_"+ NdexClasses.E_ndexPresentationProps )
    	.target(networkDoc)
    	.predicate( new OSQLPredicate("$depth <= 1"))) {

        	ODocument doc = (ODocument) propertyDoc;

        	if ( doc.getClassName().equals(NdexClasses.SimpleProperty) ) {
        		graph.removeVertex(graph.getVertex(doc));
        	}
        }
	}
	
	
	public PropertyGraphNetwork getProperytGraphNetworkById (UUID networkID, int skipBlocks, int blockSize) throws NdexException {
		ODocument nDoc = getNetworkDocByUUID(networkID);
		
	    if (nDoc == null) return null;
	    
	    PropertyGraphNetwork network = new PropertyGraphNetwork();
	    
		populatePropetyGraphNetworkFromDoc(network, nDoc);
	    
	    int startPosition = skipBlocks * blockSize;
	    int counter = 0;
	    int endPosition = skipBlocks * blockSize + blockSize;
	    
        
        TreeMap <ORID, String> termStringMap = new TreeMap<> ();

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

    
    
	public PropertyGraphNetwork getProperytGraphNetworkById(UUID id) throws NdexException {
		
		ODocument networkDoc = getNetworkDocByUUID(id);
		
		if (networkDoc == null) return null;

		PropertyGraphNetwork network = new PropertyGraphNetwork();

		NetworkDAO.populatePropetyGraphNetworkFromDoc(network, networkDoc);
		
		TreeMap<ORID, String> termStringMap = new TreeMap<>();
		
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
	
	private static void populatePropetyGraphNetworkFromDoc(PropertyGraphNetwork network, ODocument doc) {
        network.getProperties().add(new NdexPropertyValuePair(
        		PropertyGraphNetwork.uuid, doc.field(NdexClasses.Network_P_UUID).toString()));
        
        network.getProperties().add(new NdexPropertyValuePair(
        		PropertyGraphNetwork.name, (String)doc.field(NdexClasses.Network_P_name)));
        
        String desc = doc.field(NdexClasses.Network_P_desc);
        if ( desc != null && !desc.equals("")) 
        	network.getProperties().add(new NdexPropertyValuePair(PropertyGraphNetwork.description, desc));
        String version = doc.field(NdexClasses.Network_P_version);
        if ( version != null) 
        	network.getProperties().add(new NdexPropertyValuePair(PropertyGraphNetwork.version, version));
        
        NetworkSourceFormat fmt = Helper.getSourceFormatFromNetworkDoc(doc);
        if ( fmt != null)
            network.getProperties().add(new NdexPropertyValuePair(
            		NdexClasses.Network_P_source_format, fmt.toString()));
        	

        getPropertiesFromDocumentForPropertyGraph(network,doc);
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

		if ( !network.getNodes().containsKey(subjectId)) {
			PropertyGraphNode n = getPropertyGraphNode(s,network, termStringMap);
			network.getNodes().put(n.getId(), n);
		}
		
		
		ODocument o   = doc.field("out_"+NdexClasses.Edge_E_object);
		Long objectId = o.field(NdexClasses.Element_ID);
		e.setObjectId(objectId);

		if ( !network.getNodes().containsKey(objectId)) {
			PropertyGraphNode n = getPropertyGraphNode(o,network, termStringMap);
			network.getNodes().put(n.getId(), n);
		}
		
		getPropertiesFromDocumentForPropertyGraph(e,doc);
	}

	private static String getBaseTermStringFromDoc(ODocument doc, Map <ORID, String> termStringMap ) {
		String name = termStringMap.get(doc.getIdentity());
		if ( name != null) return name;
		
		name = doc.field(NdexClasses.BTerm_P_name);
		ODocument ns = doc.field("out_"+NdexClasses.BTerm_E_Namespace); 
		if (  ns != null ) {
			String prefix = ns.field(NdexClasses.ns_P_prefix);
			if ( prefix !=null)
				return prefix + ":" + name;
			return ns.field(NdexClasses.ns_P_uri) + name;
		}
		termStringMap.put(doc.getIdentity(), name);
		return name;
	}
	
	
    private PropertyGraphNode getPropertyGraphNode(ODocument doc,PropertyGraphNetwork network,
    		Map <ORID, String> termStringMap ) throws NdexException {
    	PropertyGraphNode n = new PropertyGraphNode ();
        n.setId((long)doc.field(NdexClasses.Element_ID));    	
       
        //populate node name
        String name = doc.field(NdexClasses.Node_P_name);
        if ( name != null) {
        	n.getProperties().add(new NdexPropertyValuePair(PropertyGraphNode.name, name));
        }
        
    	ODocument o = doc.field("out_" + NdexClasses.Node_E_represents);
    	if ( o != null) {
    		String repString = termStringMap.get(o.getIdentity());
    		if ( repString == null ) {
      		    String termType = o.getClassName();
    		
    			// populate objects in network
    			if ( termType.equals(NdexClasses.BaseTerm)) {
    				repString = getBaseTermStringFromDoc(o, termStringMap);
    			} else if (termType.equals(NdexClasses.ReifiedEdgeTerm)) {
    				repString = getReifiedTermStringFromDoc(o, network, termStringMap);
    			} else if (termType.equals(NdexClasses.FunctionTerm)) {
    				repString = getFunctionTermStringFromDoc(o,network,termStringMap);
    			} else 
    				throw new NdexException ("Unsupported term type '" + termType + 
    						"' found for term Id record:" + o.getIdentity().toString());
    		}
    		// original 
    		NdexPropertyValuePair p = new NdexPropertyValuePair( PropertyGraphNode.represents, repString);

    		n.getProperties().add(p);
    	}
		
    	//Populate properties
    	getPropertiesFromDocumentForPropertyGraph(n, doc);
    	
   		
    	return n;
    }
    
    private String getReifiedTermStringFromDoc(ODocument doc, PropertyGraphNetwork network,
    		Map <ORID, String> termStringMap ) throws NdexException {
	
    	String name = termStringMap.get(doc.getIdentity());
		if ( name != null) return name;

        ODocument o = doc.field("out_"+NdexClasses.ReifiedEdge_E_edge);
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

		String baseTermStr = NetworkDAO.getBaseTermStringFromDoc(
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
    				argStr = NetworkDAO.getBaseTermStringFromDoc(parameterDoc, termStringMap);
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
    public static void getPropertiesFromDocumentForPropertyGraph(PropertiedObject obj, ODocument doc) {
    	for (OIdentifiable ndexPropertyDoc : new OTraverse()
    			.field("out_"+ NdexClasses.E_ndexProperties )
    			.target(doc)
    			.predicate( new OSQLPredicate("$depth <= 1"))) {

    		ODocument propDoc = (ODocument) ndexPropertyDoc;
  
    		if ( propDoc.getClassName().equals(NdexClasses.NdexProperty)) {
    			NdexPropertyValuePair p = Helper.getNdexPropertyFromDoc(propDoc);
    			String predicate= p.getPredicateString();
    			if ( obj instanceof Network) {
    				if ( ! ((predicate.equals(PropertyGraphNetwork.uuid) ||
    						predicate.equals(PropertyGraphNetwork.name) || 
    						predicate.equals(PropertyGraphNetwork.description) ||
    						predicate.equals(PropertyGraphNetwork.version)) 
    					&& containsProperty(obj.getProperties(), predicate)) )
       					  obj.getProperties().add( Helper.getNdexPropertyFromDoc(propDoc));	
    			} else if ( obj instanceof Node) {
    				if ( ! ((predicate.equals(PropertyGraphNode.name) ||
    						predicate.equals(PropertyGraphNode.represents) || 
    						predicate.equals(PropertyGraphNode.aliases) ||
    						predicate.equals(PropertyGraphNode.relatedTerms)) 
    					&& containsProperty(obj.getProperties(), predicate)) )
       					  obj.getProperties().add( Helper.getNdexPropertyFromDoc(propDoc));	
    			} else 
					obj.getProperties().add( Helper.getNdexPropertyFromDoc(propDoc));	
    			
    		}
    	}

    	//Populate presentation properties
	/*
    	for (OIdentifiable ndexPropertyDoc : new OTraverse()
    			.field("out_"+ NdexClasses.E_ndexPresentationProps )
    			.target(doc)
    			.predicate( new OSQLPredicate("$depth <= 1"))) {

    		ODocument propDoc = (ODocument) ndexPropertyDoc;
  
    		if ( propDoc.getClassName().equals(NdexClasses.SimpleProperty)) {
				
    			obj.getPresentationProperties().add( Helper.getSimplePropertyFromDoc(propDoc));
    		}
    	}  */
    }
    
    private static boolean containsProperty(Collection<NdexPropertyValuePair> properties, String predicate) {
    	for ( NdexPropertyValuePair p : properties) {
    		if ( p.getPredicateString().equals(predicate))
    			return true;
    	}
    	return false;
    }
    
	
	public Collection<Namespace> getNetworkNamespaces(String networkUUID) {
		ODocument networkDoc = getNetworkDocByUUIDString(networkUUID);
		return getNamespacesFromNetworkDoc(networkDoc, null);
	}
	
		
    private static final String nodeQuery = "select from (traverse in_" + 
         NdexClasses.Node_E_represents + " from (select from "+ NdexClasses.BaseTerm + " where " +
         NdexClasses.Element_ID + " = ?)) where @class='"+ NdexClasses.Node +"'";
    
    public Node findNodeByBaseTermId(long baseTermID) throws NdexException {
		OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>(nodeQuery);
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
   		OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>(functionTermNodeQuery);
   		List<ODocument> nodes = db.command(query).execute( functionTermID);
       	
   		if (nodes.isEmpty())
   			return null;
   		
       	return getNode(nodes.get(0),null);
    }
    
    private static final String reifiedEdgeTermNodeQuery = 
    		"select from (traverse in_" + NdexClasses.Node_E_represents + 
    		" from (select from "+ NdexClasses.ReifiedEdgeTerm + " where " +
            NdexClasses.Element_ID + " = ?)) where @class='"+ NdexClasses.Node +"'";

    public Node findNodeByReifiedEdgeTermId (long reifiedEdgeTermId) throws NdexException {
   		OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>(reifiedEdgeTermNodeQuery);
   		List<ODocument> nodes = db.command(query).execute( reifiedEdgeTermId);
       	
   		if (nodes.isEmpty())
   			return null;
   		
       	return getNode(nodes.get(0),null);
    }
    
    private static final String refiedEdgeTermQuery = 
    		"select from (traverse in_" + NdexClasses.ReifiedEdge_E_edge + 
    		" from (select from "+ NdexClasses.Edge + " where " +
            NdexClasses.Element_ID + " = ?)) where @class='"+ 
    		NdexClasses.ReifiedEdgeTerm +"'";
       
    public ReifiedEdgeTerm findReifiedEdgeTermByEdgeId(long edgeId) {
   		OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>(refiedEdgeTermQuery);
   		List<ODocument> nodes = this.db.command(query).execute( edgeId);
       	
   		if (!nodes.isEmpty()) {
   			ReifiedEdgeTerm t = new ReifiedEdgeTerm();
   			t.setId((long)nodes.get(0).field(NdexClasses.Element_ID));
   			t.setEdgeId(edgeId);
   			return t;
   		}

       	return null;
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
    
    
    private static final String functionTermQuery = "select from (traverse in_" + 
            NdexClasses.FunctionTerm_E_baseTerm + " from (select from "+ NdexClasses.BaseTerm + " where " +
            NdexClasses.Element_ID + " = ?)) where @class='"+ NdexClasses.FunctionTerm +"'";

    // input parameter is a "rawFunctionTerm", which as elementid = -1;
    // This function will find the correspondent FunctionTerm from db.
    public FunctionTerm getFunctionTerm(FunctionTerm func) {
   		OSQLSynchQuery<ODocument> query = 
   				new OSQLSynchQuery<>(functionTermQuery);
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
            	 if ( doc.field(NdexClasses.Element_ID).equals(func.getParameterIds().get(counter)) ) {
            		 counter ++;
            	 } else 
            		 break;
	         
             }
   			}
   			if ( counter == func.getParameterIds().size()) {
   				FunctionTerm result = new FunctionTerm();
   				result.setId((long)n.field(NdexClasses.Element_ID));
   				result.setFunctionTermId(func.getFunctionTermId());
   				for (Long pid : func.getParameterIds()) 
   				  result.getParameterIds().add(pid);
   				
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
	    
    	List<Node> results = new ArrayList<>();
	    if ( !nodes.isEmpty()) {
	    	for ( ODocument doc : nodes) {
	    		results.add(getNode(doc,null));
	    	}
	    	return results;
	    }
	    
	    //TODO: check the current Transaction.
	    return results;
    }
    

    
	/**************************************************************************
	    * getNetworkUserMemberships
	    *
	    * @param networkId
	    *            UUID for network
	    * @param permission
	    * 			Type of memberships to retrieve, ADMIN, WRITE, or READ
	    * @param skipBlocks
	    * 			amount of blocks to skip
	    * @param blockSize
	    * 			The size of blocks to be skipped and retrieved
	    * @throws NdexException
	    *            Invalid parameters or an error occurred while accessing the database
	    * @throws ObjectNotFoundException
	    * 			Invalid groupId
	    **************************************************************************/
	
	public List<Membership> getNetworkUserMemberships(UUID networkId, Permissions permission, int skipBlocks, int blockSize) 
			throws ObjectNotFoundException, NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId.toString()),
		
				"A network UUID is required");
		if ( permission !=null )
			Preconditions.checkArgument( 
				(permission.equals( Permissions.ADMIN) )
				|| (permission.equals( Permissions.WRITE ))
				|| (permission.equals( Permissions.READ )),
				"Valid permission required");
		
		ODocument network = this.getRecordByUUID(networkId, NdexClasses.Network);
		
		final int startIndex = skipBlocks
				* blockSize;
		
			List<Membership> memberships = new ArrayList<>();
			
			String networkRID = network.getIdentity().toString();
			
			String traverseCondition = null;
			
			if ( permission != null) 
				traverseCondition = NdexClasses.Network +".in_"+ permission.name().toString();
			else 
				traverseCondition = "in_" + Permissions.ADMIN + ",in_" + Permissions.READ + ",in_" + Permissions.WRITE;   
			
			OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>(
		  			"SELECT " + NdexClasses.account_P_accountName + "," +
		  					NdexClasses.ExternalObj_ID + ", $path" +
		  					
			        " FROM"
		  			+ " (TRAVERSE "+ traverseCondition.toLowerCase() +" FROM"
		  				+ " " + networkRID
		  				+ "  WHILE $depth <=1)"
		  			+ " WHERE @class = '" + NdexClasses.User + "'"
		  			+ " OR @class='" + NdexClasses.Group + "'"
		 			+ " ORDER BY " + NdexClasses.ExternalObj_cTime + " DESC " + " SKIP " + startIndex
		 			+ " LIMIT " + blockSize);
			
			List<ODocument> records = this.db.command(query).execute(); 
			for(ODocument member: records) {
				
				Membership membership = new Membership();
				membership.setMembershipType( MembershipType.NETWORK );
				membership.setMemberAccountName( (String) member.field(NdexClasses.account_P_accountName) ); 
				membership.setMemberUUID( UUID.fromString( (String) member.field(NdexClasses.ExternalObj_ID) ) );
				membership.setPermissions( Helper.getNetworkPermissionFromInPath ((String)member.field("$path") ));
				membership.setResourceName( (String) network.field("name") );
				membership.setResourceUUID( networkId );
				
				memberships.add(membership);
			}
			
			logger.info("Successfuly retrieved network-user memberships");
			return memberships;
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
        ODocument accountdoc = this.getRecordByUUID(UUID.fromString(accountUUID), null);
        OrientVertex networkV = graph.getVertex(networkdoc);
        OrientVertex accountV = graph.getVertex(accountdoc);
        
        for ( com.tinkerpop.blueprints.Edge e : accountV.getEdges(networkV, Direction.OUT)) { 
        //NdexClasses.E_admin, NdexClasses.account_E_canEdit,NdexClasses.account_E_canRead)) {
          	graph.removeEdge(e);
        }

        networkdoc.reload();
        accountdoc.reload();
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
        ODocument accountdoc = this.getRecordByUUID(UUID.fromString(accountUUID), null);
        OrientVertex networkV = graph.getVertex(networkdoc);
        OrientVertex accountV = graph.getVertex(accountdoc);
        
        for ( com.tinkerpop.blueprints.Edge e : accountV.getEdges(networkV, Direction.OUT)) { 
        		                   //NdexClasses.E_admin, NdexClasses.account_E_canEdit,NdexClasses.account_E_canRead)) {
          	graph.removeEdge(e);
          	break;
        }

    	return 1;
    }

	
	public void rollback() {
		graph.rollback();		
	}

	@Override
	public void commit() {
		graph.commit();
		
	}
	
	@Override
	public void close() {
		graph.shutdown();
	}
    
	
	public void updateNetworkProfile(UUID networkId, NetworkSummary newSummary) {
		ODocument doc = this.getNetworkDocByUUID(networkId);
		
		Helper.updateNetworkProfile(doc, newSummary);
	}
	
	/**
	 * This function sets network properties using the given property list. All Existing properties
	 * of the network will be deleted. 
	 * @param networkId
	 * @param properties
	 * @return
	 * @throws ObjectNotFoundException
	 * @throws NdexException
	 */
	public int setNetworkProperties (UUID networkId, Collection<NdexPropertyValuePair> properties
			 ) throws ObjectNotFoundException, NdexException {

		
		ODocument rec = this.getRecordByUUID(networkId, null);
		OrientVertex networkV = graph.getVertex(rec);
		String traverseField = "out_" + NdexClasses.E_ndexProperties; 
		
    	for (OIdentifiable ndexPropertyDoc : new OTraverse()
			.field(traverseField)
			.target(rec)
			.predicate( new OSQLPredicate("$depth <= 1"))) {

    		ODocument propDoc = (ODocument) ndexPropertyDoc;

    		if ( propDoc.getClassName().equals(NdexClasses.NdexProperty)) {
    			OrientVertex v = graph.getVertex(propDoc);
    			v.remove();
    		}
    	}

    	networkV.getRecord().reload();
		int counter = 0 ;
		for (NdexPropertyValuePair e : properties) {
			if ( !e.getPredicateString().equals(NdexClasses.Network_P_source_format)) {
				ODocument pDoc = createNdexPropertyDoc(e);
				OrientVertex pV = graph.getVertex(pDoc);
				networkV.addEdge(NdexClasses.E_ndexProperties, pV);
				counter ++;
			}
		}


        rec.field(NdexClasses.ExternalObj_mTime, Calendar.getInstance().getTime()).save();

		return counter;
	}

	/**
	 * Set network presentation properties using the given list. All existing presentation properties of this 
	 * network will be removed.
	 * @param networkId
	 * @param properties
	 * @return
	 * @throws ObjectNotFoundException
	 * @throws NdexException
	 */
	public int setNetworkPresentationProperties (UUID networkId, 
				Collection<SimplePropertyValuePair> properties
			 ) throws ObjectNotFoundException, NdexException {

		
		ODocument rec = this.getRecordByUUID(networkId, null);
		OrientVertex networkV = graph.getVertex(rec);
		String traverseField = "out_" + NdexClasses.E_ndexPresentationProps; 
		
		for (OIdentifiable ndexPropertyDoc : new OTraverse()
			.field(traverseField)
			.target(rec)
			.predicate( new OSQLPredicate("$depth <= 1"))) {

			ODocument propDoc = (ODocument) ndexPropertyDoc;

			if ( propDoc.getClassName().equals(NdexClasses.SimpleProperty)) {
				OrientVertex v = graph.getVertex(propDoc);
				v.remove();
   			}
		}

		int counter = 0 ;
		for (SimplePropertyValuePair e : properties) {
			ODocument pDoc = Helper.createSimplePropertyDoc(e);
			OrientVertex pV = graph.getVertex(pDoc);
            networkV.addEdge(NdexClasses.E_ndexPresentationProps, pV);
      		counter ++;
		}

        rec.field(NdexClasses.ExternalObj_mTime, Calendar.getInstance().getTime());

		return counter;
	}

	//TODO: need to modify this to create baseterm and namespace first.
	private static ODocument createNdexPropertyDoc( //OrientBaseGraph graph,
			NdexPropertyValuePair property) {
		
	//	ODatabaseDocumentTx db = graph.getRawGraph();
		ODocument pDoc = new ODocument(NdexClasses.NdexProperty)
		.fields(NdexClasses.ndexProp_P_predicateStr,property.getPredicateString(),
				NdexClasses.ndexProp_P_value, property.getValue(),
				NdexClasses.ndexProp_P_datatype, property.getDataType());
/*		if ( property.getPredicateId() >0) 
			pDoc = pDoc.field(NdexClasses.ndexProp_P_predicateId, property.getPredicateId());
		if (property.getValueId() >0)
			pDoc = pDoc.field(NdexClasses.ndexProp_P_valueId, property.getValueId()); */
		return  pDoc.save();
	}

	/**
	 * Get all the node and edges that has neither citations nor supports as a subnetwork. This is a 
	 * utitlity function for xbel export.
	 * @param networkUUID
	 * @return
	 * @throws NdexException
	 */
    public Network getOrphanStatementsSubnetwork(String networkUUID) throws NdexException {
    	
    	ODocument networkDoc = getRecordByUUID(UUID.fromString(networkUUID), NdexClasses.Network);
    	
    	Network result = new Network();
    	
    	// get all edges that have neither citations nor supports.
    	for (OIdentifiable edgeRec : new OTraverse()
 			.field("out_"+ NdexClasses.Network_E_Edges)
 			.target(networkDoc)
 			.predicate( new OSQLPredicate("$depth <= 1"))) {

    		ODocument edgeDoc = (ODocument) edgeRec;

    		if ( edgeDoc.getClassName().equals(NdexClasses.Edge)) {
    			if ( edgeDoc.field("out_"+NdexClasses.Edge_E_citations) == null && 
    					edgeDoc.field("out_"+NdexClasses.Edge_E_supports) == null) {
    				Edge e = getEdgeFromDocument(edgeDoc, result);
    				result.getEdges().put(e.getId(), e);
    			}
    		}
    	}
    	
    	// get orphan nodes that has neither citations nor supports
    	for (OIdentifiable nodeRec : new OTraverse()
 			.field("out_"+ NdexClasses.Network_E_Nodes)
 			.target(networkDoc)
 			.predicate( new OSQLPredicate("$depth <= 1"))) {

    		ODocument nodeDoc = (ODocument) nodeRec;

    		if ( nodeDoc.getClassName().equals(NdexClasses.Node)){
                Long nodeId = nodeDoc.field(NdexClasses.Element_ID);
                if ( nodeDoc.field("out_" + NdexClasses.Edge_E_subject) == null &&
                	 nodeDoc.field("in_" + NdexClasses.Edge_E_object) == null && 
                	 !result.getNodes().containsKey(nodeId)) {
                    if ( nodeDoc.field("out_"+ NdexClasses.Node_E_citations) == null && 
        					nodeDoc.field("out_"+NdexClasses.Node_E_supports) == null) {
            			Node n = this.getNode(nodeDoc,result);
            		    result.getNodes().put(n.getId(), n);	
                    }
                }
    		}
    	}
    	
    	result.setEdgeCount(result.getEdges().size());
    	result.setNodeCount(result.getNodes().size());
    	
    	return result;
    }


}



