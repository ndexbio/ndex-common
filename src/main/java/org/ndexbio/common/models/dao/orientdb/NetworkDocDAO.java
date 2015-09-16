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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
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

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

public class NetworkDocDAO extends OrientdbDAO {

	private static Logger logger = Logger.getLogger(NetworkDocDAO.class.getName());
	
	
	public NetworkDocDAO (ODatabaseDocumentTx db) {
	    super(db);

	}

	public NetworkDocDAO () throws NdexException {
	    this(NdexDatabase.getInstance().getAConnection());
	}

	/**
	 * Set the islocked flag to true in the db.
	 * This is an atomic operation. Will commit the current transaction.
	 * @param networkID
	 */
	public void lockNetwork(String networkIDstr) {
		ODocument nDoc = getNetworkDocByUUIDString(networkIDstr);
		nDoc.field(NdexClasses.Network_P_isLocked,true);
		db.commit();
	}
	
	/**
	 * Set the islocked flag to false in the db.
	 * This is an atomic operation. Will commit the current transaction.
	 * @param networkID
	 */
	public void unlockNetwork (String networkIDstr) {
		ODocument nDoc = getNetworkDocByUUIDString(networkIDstr);
		nDoc.field(NdexClasses.Network_P_isLocked,false);
		db.commit();
	}
	
	public boolean networkIsLocked(String networkUUIDStr) {
		ODocument nDoc = getNetworkDocByUUIDString(networkUUIDStr);
		return nDoc.field(NdexClasses.Network_P_isLocked);
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
                +id+"' and (isDeleted = false)";
        final List<ODocument> networks = db.query(new OSQLSynchQuery<ODocument>(query));
 
        if (networks.isEmpty())
	        return null;
        
        return networks.get(0);
   }


	public  Edge getEdgeFromDocument(ODocument doc, Network network) throws NdexException {
		Edge e = new Edge();
		e.setId((long)doc.field(NdexClasses.Element_ID));
		SingleNetworkDAO.getPropertiesFromDoc(doc, e);
		
		ODocument s =  doc.field("in_"+NdexClasses.Edge_E_subject);
		Long subjectId = s.field(NdexClasses.Element_ID);
		e.setSubjectId( subjectId );
		
		if ( network !=null && 
				!network.getNodes().containsKey(subjectId)) {
			Node node = getNode (s,network);
			network.getNodes().put(subjectId, node);
		}
		
		//ODocument predicateDoc = (ODocument)doc.field("out_"+NdexClasses.Edge_E_predicate);
		Long predicateId = doc.field(NdexClasses.Edge_P_predicateId);
		e.setPredicateId(predicateId);
		
		if ( network != null && !network.getBaseTerms().containsKey(predicateId)) {
    		   BaseTerm t = getBaseTerm(getDocumentByElementId(NdexClasses.BaseTerm, predicateId),network);
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

		//populate citations
		Set<Long> citationIds = doc.field(NdexClasses.Citation);
		if ( citationIds !=null && citationIds.size()>0) {
			e.setCitationIds(citationIds);

			if ( network != null) {
				for ( Long citationId : citationIds) {
					if (! network.getCitations().containsKey(citationId)) {
						ODocument citationDoc = this.getDocumentByElementId(NdexClasses.Citation,citationId);
						Citation t = getCitationFromDoc(citationDoc);
						network.getCitations().put(citationId, t);
					}
				}
			}

		} 
		
		//populate support
		Set<Long> supportIds = doc.field(NdexClasses.Support);
		if ( supportIds !=null && supportIds.size()>0) {
			e.setSupportIds(supportIds);

			if ( network != null) {
				for ( Long supportId : supportIds) {
					if (! network.getSupports().containsKey(supportId)) {
						ODocument supportDoc = this.getDocumentByElementId(NdexClasses.Support,supportId);
						Support t = getSupportFromDoc(supportDoc,network);
						network.getSupports().put(supportId, t);
					}
				}
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
    	SingleNetworkDAO.getPropertiesFromDoc(nodeDoc, n);

     	// populate baseterm
    	Long representsId = nodeDoc.field(NdexClasses.Node_P_represents);
    	
    	if( representsId !=null) {
    		n.setRepresents(representsId);
    		String termType = nodeDoc.field(NdexClasses.Node_P_representTermType);
    		n.setRepresentsTermType(termType);
    		if (network !=null) {
    			// populate objects in network
    			if ( termType.equals(NdexClasses.BaseTerm)) {
    				if ( !network.getBaseTerms().containsKey(representsId) ) {
    	    			ODocument o = this.getDocumentByElementId(NdexClasses.BaseTerm,representsId);
    					BaseTerm bTerm = getBaseTerm(o, network);
    					network.getBaseTerms().put(representsId, bTerm);
    				}
    			} else if (termType.equals(NdexClasses.ReifiedEdgeTerm)) {
    				if ( !network.getReifiedEdgeTerms().containsKey(representsId)) {
    	    			ODocument o = this.getDocumentByElementId(NdexClasses.ReifiedEdgeTerm,representsId);
    					ReifiedEdgeTerm reTerm = getReifiedEdgeTermFromDoc(o,network);
    					network.getReifiedEdgeTerms().put(representsId, reTerm);
    				}
    			} else if (termType.equals(NdexClasses.FunctionTerm)) {
    				if ( !network.getFunctionTerms().containsKey(representsId)) {
    	    			ODocument o = this.getDocumentByElementId(NdexClasses.FunctionTerm,representsId);
    					FunctionTerm funcTerm = getFunctionTermfromDoc(o, network);
    					network.getFunctionTerms().put(representsId, funcTerm);
    				}
    			} else 
    				throw new NdexException ("Unsupported term type '" + termType + 
    						"' found for term Id:" + representsId);
    		}
    	}
		
    	//populate aliases
    	Set<Long> aliases = nodeDoc.field(NdexClasses.Node_P_alias);
    	if ( aliases !=null && aliases.size() > 0 ) {
    		n.setAliases(aliases);
    	
    		if ( network != null) {
    			for ( Long alias : aliases) {
    				if (! network.getBaseTerms().containsKey(alias)) {
    					ODocument doc = this.getDocumentByElementId(NdexClasses.BaseTerm,alias);
    					BaseTerm t = getBaseTerm(doc,network);
    					network.getBaseTerms().put(alias, t);
    				}
    			}
    		}
    	}
    	
    	//populate related terms
		Set<Long> relateTos = nodeDoc.field(NdexClasses.Node_P_relateTo);
		if ( relateTos !=null && relateTos.size()> 0 ) {
			n.setRelatedTerms(relateTos);
		
			if ( network != null) {
				for ( Long relatedTermId : relateTos) {
					if (! network.getBaseTerms().containsKey(relatedTermId)) {
						ODocument doc = this.getDocumentByElementId(NdexClasses.BaseTerm,relatedTermId);
						BaseTerm t = getBaseTerm(doc,network);
						network.getBaseTerms().put(relatedTermId, t);
					}
				}
			}
		}
    	
		//populate citations
		Set<Long> citations = nodeDoc.field(NdexClasses.Citation);
		if ( citations != null && citations.size() >0 ) { 
			n.setCitationIds(citations);
		
			if ( network != null) {
				for ( Long citationId : citations) {
					if (! network.getCitations().containsKey(citationId)) {
						ODocument doc = this.getDocumentByElementId(NdexClasses.Citation, citationId);
						Citation t = getCitationFromDoc(doc);
						network.getCitations().put(citationId, t);
					}
				}
			}
		}
			
		//populate support
		Set<Long> supports = nodeDoc.field(NdexClasses.Support);
		if ( supports !=null && supports.size() > 0 ) { 
			n.setSupportIds(supports);
		
			if ( network != null) {
				for ( Long supportId : supports) {
					if (! network.getSupports().containsKey(supportId)) {
						ODocument doc = this.getDocumentByElementId(NdexClasses.Support,supportId);
						Support t = getSupportFromDoc(doc,network);
						network.getSupports().put(supportId, t);
					}
				}
			}
		}
		
    	return n;
    }

    
	/**
	 *  This function returns the citations in this network.
	 * @param networkUUID
	 * @return
	 * @throws NdexException 
	 */
	public Collection<Citation> getNetworkCitations(String networkUUID) throws NdexException {
		ArrayList<Citation> citations = new ArrayList<>();
		
		ODocument networkDoc = getNetworkDocByUUIDString(networkUUID);
		
		for ( ODocument doc : Helper.getNetworkElements(networkDoc, NdexClasses.Network_E_Citations)) {
    			citations.add(getCitationFromDoc(doc));
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


	public  Collection<Namespace> getNamespacesFromNetworkDoc(ODocument networkDoc,Network network)  {
		ArrayList<Namespace> namespaces = new ArrayList<>();
		
		for ( ODocument doc : Helper.getNetworkElements(networkDoc, NdexClasses.Network_E_Namespace)) {
    			namespaces.add(getNamespace(doc,network));
    	}
    	return namespaces;
	}
	
    /**
     * This funciton return a self-contained sub network from a given citation. It is mainly for the XBel exporter.
     * No networkSummary values are populated from the db in the result.
     * @param networkUUID
     * @param citationId
     * @return
     * @throws NdexException
     */

	private BaseTerm getBaseTerm(ODocument o, Network network) throws NdexException {
		BaseTerm t = new BaseTerm();
		t.setId((long)o.field(NdexClasses.Element_ID));
		t.setName((String)o.field(NdexClasses.BTerm_P_name));
		
		Long nsId = o.field(NdexClasses.BTerm_NS_ID);
		if ( nsId !=null) {
		   t.setNamespaceId(nsId);

		   if ( nsId >0) {
			   if ( network != null &&
					 ! network.getNamespaces().containsKey(nsId)) {
					Namespace ns = getNamespace(getDocumentByElementId(NdexClasses.Namespace, nsId),network);
					network.getNamespaces().put(nsId, ns);
				}
		   }
		}
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
	
	
	public ODocument getDocumentByElementId(String NdexClassName, long elementID) {
		return Helper.getDocumentByElementId(db, elementID, NdexClassName);
	}

/*	private static String getBaseTermStrForBaseTerm(BaseTerm bterm, Network n) {
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
*/
    private  Namespace getNamespace(ODocument ns, Network network)  {
        Namespace rns = new Namespace();
        rns.setId((long)ns.field("id"));
        rns.setPrefix((String)ns.field(NdexClasses.ns_P_prefix));
        rns.setUri((String)ns.field(NdexClasses.ns_P_uri));
        
        SingleNetworkDAO.getPropertiesFromDoc(ns, rns);
        
        return rns;
     } 
     

    
    
	public Network getNetworkById(UUID id) throws NdexException {
		ODocument nDoc = getNetworkDocByUUIDString(id.toString());

        if (nDoc==null) return null;
   
        Network network = new Network(); 

        setNetworkSummary(nDoc, network);
        
        for ( Namespace ns : getNamespacesFromNetworkDoc(nDoc, network)) {
        	network.getNamespaces().put(ns.getId(),ns);
        }

        // get all baseTerms
        for ( ODocument doc : Helper.getNetworkElements(nDoc, NdexClasses.Network_E_BaseTerms) ) {

        		BaseTerm term = getBaseTerm(doc,network);
        		network.getBaseTerms().put(term.getId(), term);
        }

        for ( ODocument doc : Helper.getNetworkElements(nDoc,NdexClasses.Network_E_Nodes)) {
        		Node node = getNode(doc,network);
        		network.getNodes().put(node.getId(), node);
        }

        for ( ODocument doc: Helper.getNetworkElements(nDoc, NdexClasses.Network_E_Edges)) {
              	   Edge e = getEdgeFromDocument(doc,network);
              	   network.getEdges().put(e.getId(), e);
        }
        
        return network;
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

        setNetworkSummary(nDoc, network);

        
        for ( ODocument doc : Helper.getNetworkElements(nDoc, NdexClasses.Network_E_Edges) ){ 
            	if ( counter >= endPosition) break;

                counter ++;
            	
            	if ( counter >= startPosition )  {
              	   Edge e = getEdgeFromDocument(doc,network);
              	   network.getEdges().put(e.getId(), e);
            	               
                }
        }
        
        network.setEdgeCount(network.getEdges().size());
        network.setNodeCount(network.getNodes().size());
        
		 return network; 
	}
	

	public Collection<BaseTerm> getBaseTerms(String networkUUID) throws NdexException {
		ArrayList<BaseTerm> baseTerms = new ArrayList<>();
		
		ODocument networkDoc = getNetworkDocByUUIDString(networkUUID);
		
		
		for ( ODocument doc : Helper.getNetworkElements(networkDoc, NdexClasses.Network_E_BaseTerms)) {
        
        			BaseTerm t = getBaseTerm(doc,null);
        			baseTerms.add(t);
        }
    	  	
    	return baseTerms;
    	
    }
	
	public Collection<Namespace> getNamespaces(String networkUUID)  {
		ArrayList<Namespace> namespaces = new ArrayList<>();
		
		ODocument networkDoc = getNetworkDocByUUIDString(networkUUID);
		
		for ( ODocument doc : Helper.getNetworkElements(networkDoc, NdexClasses.Network_E_Namespace)) {
 
        			Namespace n = getNamespace(doc,null);
        			namespaces.add(n);
        	}
    	return namespaces;
	}

    public static NetworkSummary getNetworkSummary(ODocument doc)  {
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

    

    
	private static Citation getCitationFromDoc(ODocument doc) {
		Citation result = new Citation();
		result.setId((long)doc.field(NdexClasses.Element_ID));
		result.setTitle((String)doc.field(NdexClasses.Citation_P_title));
		result.setIdType((String)doc.field(NdexClasses.Citation_p_idType));
		result.setIdentifier((String)doc.field(NdexClasses.Citation_P_identifier));
		
		List<String> o = doc.field(NdexClasses.Citation_P_contributors);
		
		if ( o!=null && !o.isEmpty())
			result.setContributors(o);
		
    	List<NdexPropertyValuePair> props = doc.field(NdexClasses.ndexProperties);
    	if ( props !=null && props.size() > 0 )
    		result.setProperties(props);

    	return result;
	}


    private Support getSupportFromDoc(ODocument doc, Network network) throws NdexException {
    	Support s = new Support();
    	s.setText((String)doc.field(NdexClasses.Support_P_text));
    	s.setId((long)doc.field(NdexClasses.Element_ID));
    	Long citationId = doc.field(NdexClasses.Citation);
    	if ( citationId !=null) {
    		s.setCitationId(citationId);
    	
    		if ( network !=null && 
            		! network.getCitations().containsKey(citationId)) {
    			ODocument citationDoc = this.getDocumentByElementId(NdexClasses.Citation, citationId);
            	Citation citation = getCitationFromDoc(citationDoc);
            	network.getCitations().put(citationId, citation);
            }
    	}
        
    	SingleNetworkDAO.getPropertiesFromDoc(doc, s);

    	return s;
    	
    }

    
    //TODO: need to make sure the recursion doesn't form a loop.
    private FunctionTerm getFunctionTermfromDoc(ODocument doc,Network network) throws NdexException {
    	FunctionTerm term = new FunctionTerm();
    	
    	term.setId((long)doc.field(NdexClasses.Element_ID));

    	// get the functionTerm 
    	
    	Long baseTermId =doc.field(NdexClasses.BaseTerm);
    	
    	if ( network !=null && !network.getBaseTerms().containsKey(baseTermId)) {
    		ODocument baseTermDoc = getDocumentByElementId(NdexClasses.BaseTerm, baseTermId);
    		BaseTerm bt = getBaseTerm(baseTermDoc, network);
   			network.getBaseTerms().put(baseTermId, bt);
    	}
    	
    	term.setFunctionTermId(baseTermId);
    	// traverse for the argument
    	boolean isFirst= true; 
    	
    	for ( ODocument parameterDoc : Helper.getDocumentLinks(doc, "out_", NdexClasses.FunctionTerm_E_paramter))  {

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
    	//	}
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
    
    protected static  NetworkSummary setNetworkSummary(ODocument doc, NetworkSummary nSummary)  {
    	
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
        
        nSummary.setIsLocked((boolean)doc.field(NdexClasses.Network_P_isLocked));
        nSummary.setURI(NdexDatabase.getURIPrefix()+ "/network/" + nSummary.getExternalId().toString());

        SingleNetworkDAO.getPropertiesFromDoc(doc, nSummary);
        
		NetworkSourceFormat fmt = Helper.getSourceFormatFromNetworkDoc(doc);
		if ( fmt !=null) {
			NdexPropertyValuePair p = new NdexPropertyValuePair(NdexClasses.Network_P_source_format,fmt.toString());
			nSummary.getProperties().add(p);
		}
        
        return nSummary;
    }


}
