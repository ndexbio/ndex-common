package org.ndexbio.common.persistence.orientdb;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.Citation;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.Node;
import org.ndexbio.model.object.network.PropertyGraphEdge;
import org.ndexbio.model.object.network.PropertyGraphNetwork;
import org.ndexbio.model.object.network.PropertyGraphNode;
import org.ndexbio.model.object.network.Support;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PropertyGraphLoader {
	
//	private NdexPersistenceService persistenceService;
	NdexDatabase db;
	private ObjectMapper mapper;
	
	public PropertyGraphLoader (NdexDatabase db)  {
		this.db = db;
//		this.persistenceService = new NdexPersistenceService(db);
		mapper = new ObjectMapper();
	}
	
	public NetworkSummary insertNetwork(PropertyGraphNetwork network, String accountName) throws Exception {
		UUID uuid = null;
		NdexPersistenceService persistenceService = null;
		try {
			persistenceService = new NdexPersistenceService(db);;
		
			for ( NdexPropertyValuePair p : network.getProperties()) {
				if ( p.getPredicateString().equals ( PropertyGraphNetwork.uuid) ) {
					uuid = UUID.fromString(p.getValue());
					break;
				}
			}
		
			if ( uuid == null) {
				uuid = NdexUUIDFactory.INSTANCE.getNDExUUID();
				insertNewNetwork(uuid, network, accountName,persistenceService );
			} else
				updateNetwork(uuid, network, accountName,persistenceService );

			persistenceService.persistNetwork();
			NetworkSummary result = persistenceService.getCurrentNetwork();
			persistenceService = null;
			return result;
		} finally {
			if ( persistenceService !=null) persistenceService.close();
		}
	}
	

	private void insertNewNetwork(UUID uuid, PropertyGraphNetwork network, String accountName,
			NdexPersistenceService persistenceService) throws Exception {
        String title = null;
        String description = null;
        String version = null;
        List<NdexPropertyValuePair> otherAttributes = new ArrayList<NdexPropertyValuePair>();
        
        Namespace[] namespaces = null;
        for ( NdexPropertyValuePair p : network.getProperties()) {
			if ( p.getPredicateString().equals(PropertyGraphNetwork.name) ) {
				title = p.getValue();
			} else if ( p.getPredicateString().equals(PropertyGraphNetwork.version) ) {
				version = p.getValue();
			} else if ( p.getPredicateString().equals(PropertyGraphNetwork.description) ) {
				description = p.getValue();
			} else if (p.getPredicateString().equals(PropertyGraphNetwork.namspaces)) {
				namespaces = mapper.readValue(p.getValue(), Namespace[].class);
			} else if (p.getPredicateString().equals(PropertyGraphNetwork.supports)) {
				Support[] supports = mapper.readValue(p.getValue(), Support[].class);
			} else if (p.getPredicateString().equals(PropertyGraphNetwork.citations)) {
				
			} else if ( !p.getPredicateString().equals(PropertyGraphNetwork.uuid) ) {
				otherAttributes.add(p);
			} 
		}
		
		persistenceService.createNewNetwork(accountName, title, version, uuid);
		persistenceService.setNetworkTitleAndDescription(title, description);

		if ( namespaces != null) {
			for ( Namespace ns : namespaces ) {
				persistenceService.createNamespace(ns.getPrefix(), ns.getUri());
			}
		}

		persistenceService.setNetworkProperties(otherAttributes, network.getPresentationProperties());
		
		for ( PropertyGraphNode n : network.getNodes().values()) {
			
			String nodeName = null;
			String baseTerm = null;
			ArrayList<NdexPropertyValuePair> otherProperties = new ArrayList<NdexPropertyValuePair>(); 
			
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
				persistenceService.setNodeRepresentTerm(nodeId, termId);
			}
			if ( nodeName != null ) 
				persistenceService.setNodeName(nodeId, nodeName);

			persistenceService.setNodeProperties(nodeId, otherProperties, n.getPresentationProperties());
		}
		
		// persist edges
		for ( PropertyGraphEdge e : network.getEdges().values()) {
			Long termId = persistenceService.getBaseTermId(e.getPredicate());
			Long subjectNodeId = persistenceService.findOrCreateNodeIdByExternalId(
					Long.toString(e.getSubjectId()), null);
			Long objectNodeId = persistenceService.findOrCreateNodeIdByExternalId(
					Long.toString(e.getObjectId()),null);
			
			// process the citation , property list ...
			Support support = null;
			Citation citation = null;
			ArrayList<NdexPropertyValuePair> otherProperties = new ArrayList<NdexPropertyValuePair> (e.getProperties().size()); 
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
					support, citation, otherProperties, e.getPresentationProperties());
			
		}
		
	}
	
	private void updateNetwork (UUID uuid, PropertyGraphNetwork network, String accountName,
			NdexPersistenceService persistenceService) throws Exception {
		//TODO: remove the network from system first.
		
		insertNewNetwork(uuid,network, accountName, persistenceService);
		
	}
	
	
}
