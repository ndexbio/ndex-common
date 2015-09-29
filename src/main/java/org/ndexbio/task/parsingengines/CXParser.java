package org.ndexbio.task.parsingengines;

import java.io.FileInputStream;
import java.util.UUID;


import org.ndexbio.common.persistence.orientdb.CXNetworkLoader;
import org.ndexbio.model.exceptions.NdexException;

public class CXParser implements IParsingEngine {
	
	private String fileName;
	private String ownerAccountName;
	private UUID uuid;

	public CXParser(String fn, String ownerName) {
		this.fileName = fn;
		this.ownerAccountName = ownerName;
	}

	@Override
	public void parseFile() throws NdexException  {

		
		try (CXNetworkLoader loader = new CXNetworkLoader(new FileInputStream(fileName), ownerAccountName)) {
			uuid = loader.persistCXNetwork();
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
