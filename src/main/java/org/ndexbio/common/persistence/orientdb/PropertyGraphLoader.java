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
package org.ndexbio.common.persistence.orientdb;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.models.dao.orientdb.Helper;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.common.models.object.network.RawNamespace;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.*;
import org.ndexbio.model.object.network.Citation;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.NetworkSourceFormat;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.PropertyGraphEdge;
import org.ndexbio.model.object.network.PropertyGraphNetwork;
import org.ndexbio.model.object.network.PropertyGraphNode;
import org.ndexbio.model.object.network.Support;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class PropertyGraphLoader {
	
	NdexDatabase db;
	private ObjectMapper mapper;
	
	public PropertyGraphLoader (NdexDatabase db)  {
		this.db = db;
		mapper = new ObjectMapper();
	}
	
	public NetworkSummary insertNetwork(PropertyGraphNetwork network, User loggedInUser) throws Exception {

		NdexPersistenceService persistenceService = null;
		try {
		
			persistenceService = new NdexPersistenceService(db);
			insertNewNetwork(network, persistenceService, loggedInUser );
			
			removeNetworkSourceFormat(network);
			
			persistenceService.setNetworkSourceFormat(NetworkSourceFormat.PROPERTYGRAPH);
			persistenceService.persistNetwork();
			NetworkSummary result = persistenceService.getCurrentNetwork();
			persistenceService = null;
			return result;
		} finally {
			if ( persistenceService !=null) persistenceService.close();
		}
	}
	

	public NetworkSummary updateNetwork(PropertyGraphNetwork network) throws Exception {
		UUID uuid = null;
		NdexPersistenceService persistenceService = null;
		try {
		
			for ( NdexPropertyValuePair p : network.getProperties()) {
				if ( p.getPredicateString().equals ( PropertyGraphNetwork.uuid) ) {
					uuid = UUID.fromString(p.getValue());
					break;
				}
			}
			
			if(uuid == null) 
				throw new NdexException("updateNetwork: UUID not found in PropertyGraph.");
			
			persistenceService = new NdexPersistenceService(db,uuid);
			updateNetwork(uuid, network, persistenceService );

			removeNetworkSourceFormat(network);
			
			persistenceService.setNetworkSourceFormat(NetworkSourceFormat.PROPERTYGRAPH);

			persistenceService.persistNetwork();
			NetworkSummary result = persistenceService.getCurrentNetwork();
			persistenceService = null;
			return result;
		} finally {
			if ( persistenceService !=null) persistenceService.close();
		}
	}
	
	
	private void insertNewNetwork(PropertyGraphNetwork network,
                                  NdexPersistenceService persistenceService, User loggedInUser) throws Exception {

		String title = null;
        String description = null;
        String version = null;
        List<NdexPropertyValuePair> otherAttributes = new ArrayList<>();
        
		//        Namespace[] namespaces = null;
        for ( NdexPropertyValuePair p : network.getProperties()) {
			if ( p.getPredicateString().equals(PropertyGraphNetwork.name) ) {
				title = p.getValue();
			} else if ( p.getPredicateString().equals(PropertyGraphNetwork.version) ) {
				version = p.getValue();
			} else if ( p.getPredicateString().equals(PropertyGraphNetwork.description) ) {
				description = p.getValue();
				
			} else if ( !p.getPredicateString().equals(PropertyGraphNetwork.uuid) &&
					    !p.getPredicateString().equals(NdexClasses.Network_P_source_format)) {
				otherAttributes.add(p);
			} 
		}
		
		persistenceService.createNewNetwork(loggedInUser.getAccountName(), title, version);
		persistenceService.setNetworkTitleAndDescription(title, description);

		persistenceService.setNetworkProperties(otherAttributes, null /*network.getPresentationProperties()*/);
		
		insertNetworkElements(network,persistenceService);

        //DW: Provenance
        NetworkSummary summary = persistenceService.getCurrentNetwork();

        ProvenanceEntity entity = new ProvenanceEntity();
        entity.setUri(summary.getURI());

        Helper.populateProvenanceEntity(entity, summary );

        ProvenanceEvent event = new ProvenanceEvent(NdexProvenanceEventType.PROGRAM_UPLOAD, summary.getModificationTime());

        List<SimplePropertyValuePair> eventProperties = new ArrayList<>();
        Helper.addUserInfoToProvenanceEventProperties( eventProperties, loggedInUser);
        event.setProperties(eventProperties);

        entity.setCreationEvent(event);

        persistenceService.setNetworkProvenance(entity);
	}
	
	
	private void insertNetworkElements(PropertyGraphNetwork network, NdexPersistenceService persistenceService) throws NdexException, 
		JsonParseException, JsonMappingException, IOException, ExecutionException {

		
        Namespace[] namespaces = null;
        for ( NdexPropertyValuePair p : network.getProperties()) {
			if (p.getPredicateString().equals(PropertyGraphNetwork.namspaces)) {
				namespaces = mapper.readValue(p.getValue(), Namespace[].class);
			} 
		/*	TODO: need to review if we have support and citation that only applied to network but not to nodes and edges.
			else if (p.getPredicateString().equals(PropertyGraphNetwork.supports)) {
				Support[] supports = mapper.readValue(p.getValue(), Support[].class);
			} else if (p.getPredicateString().equals(PropertyGraphNetwork.citations)) {
				
			}  */
		}
		
		if ( namespaces != null) {
			for ( Namespace ns : namespaces ) {
				persistenceService.getNamespace(new RawNamespace(ns.getPrefix(), ns.getUri()));
			}
		}

		
		for ( PropertyGraphNode n : network.getNodes().values()) {
			
			String nodeName = null;
			String baseTerm = null;
			ArrayList<NdexPropertyValuePair> otherProperties = new ArrayList<>(); 
			
			for ( NdexPropertyValuePair p : n.getProperties()) {
			  if (p.getPredicateString().equals(PropertyGraphNode.represents)) {
				  baseTerm = p.getValue();  
			  } else if ( p.getPredicateString().equals(PropertyGraphNode.name)) {
				  nodeName = p.getValue();
			  } else
				  otherProperties.add(p);
			}
			
			Long nodeId = persistenceService.findOrCreateNodeIdByExternalId(
					Long.toString(n.getId()),null);
			if (baseTerm != null) {
				Long termId =  persistenceService.getBaseTermId(baseTerm);
				persistenceService.setNodeRepresentBaseTerm(nodeId, termId);
			}
			if ( nodeName != null ) 
				persistenceService.setNodeName(nodeId, nodeName);

			persistenceService.setNodeProperties(nodeId, otherProperties, null /*n.getPresentationProperties() */);
		}
		
		// persist edges
		for ( PropertyGraphEdge e : network.getEdges().values()) {
			Long termId = persistenceService.getBaseTermId(e.getPredicate());
			Long subjectNodeId = persistenceService.findOrCreateNodeIdByExternalId(
					Long.toString(e.getSubjectId()), null);
			Long objectNodeId = persistenceService.findOrCreateNodeIdByExternalId(
					Long.toString(e.getObjectId()),null);
			
			//TODO: citations and supports are not populated.
			// process the citation , property list ...
			Support support = null;
			Citation citation = null;
			ArrayList<NdexPropertyValuePair> otherProperties = new ArrayList<> (e.getProperties().size()); 
			for ( NdexPropertyValuePair p : e.getProperties()) {
				if ( p.getPredicateString().equals(PropertyGraphEdge.supports)) {
					support = mapper.readValue(p.getValue(), Support.class);
				} else if ( p.getPredicateString().equals(PropertyGraphEdge.citations)) {
					citation = mapper.readValue(p.getValue(), Citation.class);
				} else  {
					otherProperties.add(p);
				}
			}
			
			persistenceService.createEdge(subjectNodeId, objectNodeId, termId, 
					null, null, otherProperties);
			
		}
		
	}
	
	private void updateNetwork (UUID uuid, PropertyGraphNetwork network,  NdexPersistenceService persistenceService) throws Exception {
		
			NetworkDAO dao = new NetworkDAO(persistenceService.getDbConnection());
			
			persistenceService.networkVertex.getRecord().field(NdexClasses.Network_P_isComplete, "false").save();
			persistenceService.commit();
			
			//TODO: remove the network from system first.
			dao.deleteNetworkElements(uuid.toString());
			dao.deleteNetworkProperties(persistenceService.networkVertex.getRecord());
			
			
			//save the network info
//	        List<NdexPropertyValuePair> otherAttributes = new ArrayList<>();
	        NetworkSummary currentNetwork = persistenceService.getCurrentNetwork();
	        currentNetwork.getProperties().clear();
	        currentNetwork.setName(null);
	        currentNetwork.setDescription(null);
	        currentNetwork.setVersion(null);
	        
	        for ( NdexPropertyValuePair p : network.getProperties()) {
				if ( p.getPredicateString().equals(PropertyGraphNetwork.name) ) {
					currentNetwork.setName(p.getValue());
				} else if ( p.getPredicateString().equals(PropertyGraphNetwork.version) ) {
					currentNetwork.setVersion(p.getValue());
				} else if ( p.getPredicateString().equals(PropertyGraphNetwork.description) ) {
					currentNetwork.setDescription(p.getValue());
				} else if ( !p.getPredicateString().equals(PropertyGraphNetwork.uuid) ) {
					currentNetwork.getProperties().add(p);
				} 
			}			
			
	        currentNetwork.setNodeCount(network.getNodes().size());
	        currentNetwork.setEdgeCount(network.getEdges().size());
	        persistenceService.networkVertex.getRecord().reload();
			ODocument networkDoc = persistenceService.networkVertex.getRecord();
	        networkDoc = networkDoc.fields(NdexClasses.ExternalObj_mTime, Calendar.getInstance().getTime(),
	        		NdexClasses.Network_P_name, currentNetwork.getName(),
	        		NdexClasses.Network_P_desc, currentNetwork.getDescription(),
	        		NdexClasses.Network_P_version, currentNetwork.getVersion(),
	        		NdexClasses.ExternalObj_mTime, Calendar.getInstance().getTime()).save();
	        
	        
			persistenceService.setNetworkProperties(currentNetwork.getProperties(), null /*network.getPresentationProperties()*/);

			persistenceService.networkVertex.getRecord().reload();
			// redo populate the elements
		    insertNetworkElements(network, persistenceService);
		
	}
	
	private static NetworkSourceFormat removeNetworkSourceFormat(PropertyGraphNetwork pg) {
		List<NdexPropertyValuePair> props = pg.getProperties(); 
		
		for ( int i = 0 ; i < props.size(); i++) {
			NdexPropertyValuePair p = props.get(i);
			if ( p.getPredicateString().equals(NdexClasses.Network_P_source_format)) {
				NetworkSourceFormat fmt = NetworkSourceFormat.valueOf(p.getValue());
				props.remove(i);
				return fmt;
			}
		}
		return null;
	}
	
	
}
