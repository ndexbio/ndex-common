package org.ndexbio.common.persistence.orientdb;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.model.object.NdexProperty;
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
	
	private NdexPersistenceService persistenceService;
	private ObjectMapper mapper;
	
	public PropertyGraphLoader (NdexDatabase db)  {
		this.persistenceService = new NdexPersistenceService(db);
		mapper = new ObjectMapper();
	}
	
	public NetworkSummary insertNetwork(PropertyGraphNetwork network, String accountName) throws Exception {
		UUID uuid = null;
		
		for ( NdexProperty p : network.getProperties()) {
			if ( p.getPredicateString().equals ( PropertyGraphNetwork.uuid) ) {
				uuid = UUID.fromString(p.getValue());
				break;
			}
		}
		
		if ( uuid == null) {
			uuid = NdexUUIDFactory.INSTANCE.getNDExUUID();
			insertNewNetwork(uuid, network, accountName);
		} else
			updateNetwork(uuid, network, accountName);

		persistenceService.persistNetwork();
		return persistenceService.getSummaryOfCurrentNetwork();
	}
	

	private void insertNewNetwork(UUID uuid, PropertyGraphNetwork network, String accountName) throws Exception {
        String title = null;
        String description = null;
        String version = null;
        List<NdexProperty> otherAttributes = new ArrayList<NdexProperty>();
        

        for ( NdexProperty p : network.getProperties()) {
			if ( p.getPredicateString().equals(PropertyGraphNetwork.name) ) {
				title = p.getValue();
			} else if ( p.getPredicateString().equals(PropertyGraphNetwork.version) ) {
				version = p.getValue();
			} else if ( p.getPredicateString().equals(PropertyGraphNetwork.description) ) {
				description = p.getValue();
			} else if (p.getPredicateString().equals(PropertyGraphNetwork.namspaces)) {
				Namespace[] namespaces = mapper.readValue(p.getValue(), Namespace[].class);
				if ( namespaces != null) {
					for ( Namespace ns : namespaces ) {
						persistenceService.createNamespace(ns.getPrefix(), ns.getUri());
					}
				}
			} else if (p.getPredicateString().equals(PropertyGraphNetwork.supports)) {
				Support[] supports = mapper.readValue(p.getValue(), Support[].class);
			} else if (p.getPredicateString().equals(PropertyGraphNetwork.citations)) {
				
			} else if ( !p.getPredicateString().equals(PropertyGraphNetwork.uuid) ) {
				otherAttributes.add(p);
			} 
		}
		
		persistenceService.createNewNetwork(accountName, title, version, uuid);
		persistenceService.setNetworkTitleAndDescription(title, description);
		
		persistenceService.setNetworkProperties(otherAttributes, network.getPresentationProperties());
		
		for ( PropertyGraphNode n : network.getNodes().values()) {
			
			String nodeName = null;
			String baseTerm = null;
			ArrayList<NdexProperty> otherProperties = new ArrayList<NdexProperty>(); 
			
			for ( NdexProperty p : n.getProperties()) {
			  if (p.getPredicateString().equals(PropertyGraphNode.represents)) {
				  baseTerm = p.getValue();  
			  } else if ( p.getPredicateString().equals(PropertyGraphNode.name)) {
				  nodeName = p.getValue();
			  } else
				  otherProperties.add(p);
			}
			
			Node node = persistenceService.findOrCreateNodeByExternalId(n.getId());;
			if (baseTerm != null) {
				BaseTerm term =  persistenceService.getBaseTerm(baseTerm);
				persistenceService.setNodeRepresentTerm(node.getId(), term.getId());
			}
			if ( nodeName != null ) 
				persistenceService.setNodeName(node.getId(), nodeName);

			persistenceService.setNodeProperties(node.getId(), otherProperties, n.getPresentationProperties());
		}
		
		// persist edges
		for ( PropertyGraphEdge e : network.getEdges()) {
			BaseTerm term = persistenceService.getBaseTerm(e.getPredicate());
			Node subjectNode = persistenceService.findOrCreateNodeByExternalId(e.getSubjectId());
			Node objectNode = persistenceService.findOrCreateNodeByExternalId(e.getObjectId());
			
			// process the citation , property list ...
			Support support = null;
			Citation citation = null;
			ArrayList<NdexProperty> otherProperties = new ArrayList<NdexProperty> (e.getProperties().size()); 
			for ( NdexProperty p : e.getProperties()) {
				if ( p.getPredicateString().equals(PropertyGraphEdge.supports)) {
					support = mapper.readValue(p.getValue(), Support.class);
				} else if ( p.getPredicateString().equals(PropertyGraphEdge.citations)) {
					citation = mapper.readValue(p.getValue(), Citation.class);
				} else  {
					otherProperties.add(p);
				}
			}
			
			persistenceService.createEdge(subjectNode, objectNode, term, 
					support, citation, otherProperties, e.getPresentationProperties());
			
		}
		
	}
	
	private void updateNetwork (UUID uuid, PropertyGraphNetwork network, String accountName) throws Exception {
		// remove the network from system first.
		
		insertNewNetwork(uuid,network, accountName);
		
	}
	
}
