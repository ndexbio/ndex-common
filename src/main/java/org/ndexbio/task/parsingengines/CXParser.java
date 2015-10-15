package org.ndexbio.task.parsingengines;

import java.io.FileInputStream;
import java.util.List;
import java.util.UUID;

import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.models.dao.orientdb.Helper;
import org.ndexbio.common.models.dao.orientdb.NetworkDocDAO;
import org.ndexbio.common.models.dao.orientdb.UserDocDAO;
import org.ndexbio.common.persistence.orientdb.CXNetworkLoader;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.NdexProvenanceEventType;
import org.ndexbio.model.object.ProvenanceEntity;
import org.ndexbio.model.object.SimplePropertyValuePair;
import org.ndexbio.model.object.User;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.tools.ProvenanceHelpers;

public class CXParser implements IParsingEngine {
	
	private String fileName;
	private String ownerAccountName;
	private UUID uuid;
	private String description;

	public CXParser(String fn, String ownerName, String description) {
		this.fileName = fn;
		this.ownerAccountName = ownerName;
		this.description = description;
	}

	@Override
	public void parseFile() throws NdexException  {

		
		try (CXNetworkLoader loader = new CXNetworkLoader(new FileInputStream(fileName), ownerAccountName)) {
			uuid = loader.persistCXNetwork();
			
			try (NetworkDocDAO dao = new NetworkDocDAO()) {
				NetworkSummary currentNetwork = dao.getNetworkSummaryById(uuid.toString());
				
				String uri = NdexDatabase.getURIPrefix();
		        @SuppressWarnings("resource")
				UserDocDAO userDocDAO = new UserDocDAO(dao.getDBConnection()) ;
		        User loggedInUser = userDocDAO.getUserByAccountName(ownerAccountName);
		        	
				ProvenanceEntity provEntity = ProvenanceHelpers.createProvenanceHistory(currentNetwork,
                    uri, NdexProvenanceEventType.FILE_UPLOAD, currentNetwork.getCreationTime(), 
                    (ProvenanceEntity)null);
				Helper.populateProvenanceEntity(provEntity, currentNetwork);
				provEntity.getCreationEvent().setEndedAtTime(currentNetwork.getModificationTime());

				List<SimplePropertyValuePair> l = provEntity.getCreationEvent().getProperties();
				Helper.addUserInfoToProvenanceEventProperties( l, loggedInUser);
				l.add(	new SimplePropertyValuePair ( "filename",description) );

				loader.setNetworkProvenance(provEntity);

			}
		} catch ( Exception e) {
			e.printStackTrace();
			throw new NdexException ("Failed to load CX file. " + e.getMessage());
		} 
		
	}

	@Override
	public UUID getUUIDOfUploadedNetwork() {
		return uuid;
	}

}
