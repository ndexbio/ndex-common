package org.ndexbio.common.persistence.orientdb;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.model.object.NdexProperty;
import org.ndexbio.model.object.network.PropertyGraphNetwork;

public class PropertyGraphLoader {
	
	private NdexPersistenceService persistenceService;
	
	
	public PropertyGraphLoader (NdexDatabase db)  {
		this.persistenceService = new NdexPersistenceService(db);
	}
	
	public UUID insertNetwork(PropertyGraphNetwork network, String accountName) throws Exception {
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
		
		return uuid;
	}
	

	private void insertNewNetwork(UUID uuid, PropertyGraphNetwork network, String accountName) throws Exception {
        String name = null;
        String description = null;
        String version = null;
        List<NdexProperty> otherAttributes = new ArrayList<NdexProperty>();
        
		for ( NdexProperty p : network.getProperties()) {
			if ( p.getPredicateString().equals(PropertyGraphNetwork.name) ) {
				name = p.getValue();
			} else if ( p.getPredicateString().equals(PropertyGraphNetwork.version) ) {
				version = p.getValue();
			} else if ( p.getPredicateString().equals(PropertyGraphNetwork.description) ) {
				description = p.getValue();
			} if ( !p.getPredicateString().equals(PropertyGraphNetwork.uuid) ) {
				otherAttributes.add(p);
			}
		}
		
		persistenceService.createNewNetwork(accountName, name, version);
		persistenceService.setNetworkTitleAndDescription(name, description);
		
		
	}
	
	private void updateNetwork (UUID uuid, PropertyGraphNetwork network, String accountName) {
		
		
	}
	
}
