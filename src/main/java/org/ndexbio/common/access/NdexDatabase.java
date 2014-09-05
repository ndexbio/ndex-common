package org.ndexbio.common.access;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.helpers.Configuration;
import org.ndexbio.orientdb.NdexSchemaManager;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.dictionary.ODictionary;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class NdexDatabase {
	
	private NdexAOrientDBConnectionPool pool;
	
	private ODatabaseDocumentTx ndexDatabase;  // this connection is used for transactions in this database object.
	
	private ODictionary dictionary;
	
	private static final String sequenceKey="NdexSeq";

	private static final String seqField= "f1";
	
	private int batchCounter;
	
	private long internalCounterBase;

	private static final int blockSize = 50;  
	
	private ODocument vdoc;
	
	static private String URIPrefix = null;
	
	
	public NdexDatabase() throws NdexException {
		pool = NdexAOrientDBConnectionPool.getInstance();
		ndexDatabase = pool.acquire();
		dictionary = ndexDatabase.getDictionary();
		NdexSchemaManager.INSTANCE.init(ndexDatabase);
		vdoc = (ODocument) dictionary.get(sequenceKey);
		if (vdoc == null ) {
			ndexDatabase.commit();
			internalCounterBase = 1;
			vdoc = new ODocument(seqField, internalCounterBase);  // + blockSize); // ids start with 1.
			vdoc = vdoc.save();
			dictionary.put(sequenceKey, vdoc);
			ndexDatabase.commit();	
		} 
		batchCounter=blockSize;
		
	}
	
	static public String getURIPrefix () throws NdexException {
		if ( URIPrefix == null) {
			URIPrefix = Configuration.getInstance().getProperty("HostURI");
			if ( URIPrefix == null) {
				try {
					URIPrefix = "http://" + InetAddress.getLocalHost().getHostName() ;
				} catch (UnknownHostException e) {
					throw new NdexException ("Failed to construct base URI for Ndex server because of UnknownHostException: " 
							+ e.getMessage());
				}
			}
		}
		return URIPrefix;

	}
	
    public synchronized long  getNextId() {
    	
    	if ( batchCounter == blockSize) {
    		vdoc.reload();
        	internalCounterBase = vdoc.field(seqField);
    	    batchCounter = 0 ;
            vdoc = vdoc.field(seqField, internalCounterBase + blockSize).save();
            dictionary.put(sequenceKey, vdoc);
        	commit();
       // 	System.out.println("New batch in id sequence:" + internalCounterBase );
    	}
    	long rvalue = internalCounterBase + batchCounter;
    	batchCounter++;
        
    	return rvalue;
    }
    
    public synchronized void resetIdCounter() {
    	vdoc.field(seqField,0);
    }
    
    public void close () {
    //	vdoc.save();
    	ndexDatabase.commit();
    	ndexDatabase.close();
    }
    
    public void commit() {
    	ndexDatabase.commit();
//    	ndexDatabase.begin();
    }
    
    public ODatabaseDocumentTx getAConnection() {
    	return pool.acquire();
    }
    
 //   public ODatabaseDocumentTx getTransactionConnection() {return ndexDatabase;}
}
