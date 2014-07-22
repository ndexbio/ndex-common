package org.ndexbio.common.access;

import org.ndexbio.common.exceptions.NdexException;
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
	
	private ODocument vdoc;
	
	public NdexDatabase() throws NdexException {
		pool = NdexAOrientDBConnectionPool.getInstance();
		ndexDatabase = pool.acquire();
//		ndexDatabase.begin();
		dictionary = ndexDatabase.getDictionary();
		NdexSchemaManager.INSTANCE.init(ndexDatabase);
		vdoc = (ODocument) dictionary.get(sequenceKey);
		if (vdoc == null ) {
			ndexDatabase.begin();
			vdoc = new ODocument(seqField, (long)1); // ids start with 1.
			vdoc.save();
			ndexDatabase.commit();	
			dictionary.put(sequenceKey, vdoc);
		}
		ndexDatabase.begin();

	}
	
    public synchronized long  getNextId() {
    	vdoc = (ODocument)dictionary.get(sequenceKey);
    	long nextval= vdoc.field(seqField);
    	dictionary.put(sequenceKey, vdoc.field(seqField,nextval+1));
    	vdoc.save();
    	return nextval;
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
    	ndexDatabase.begin();
    }
    
    public ODatabaseDocumentTx getAConnection() {
    	return pool.acquire();
    }
    
    public ODatabaseDocumentTx getTransactionConnection() {return ndexDatabase;}
}
