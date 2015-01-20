package org.ndexbio.common.access;

import org.ndexbio.model.exceptions.NdexException;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

public class NdexAOrientDBDAO {
	
    protected ODatabaseDocumentTx _ndexDatabase = null;
    
    @Deprecated    
    protected void setup() throws NdexException
    {
       _ndexDatabase = NdexAOrientDBConnectionPool.getInstance().acquire();
    }
    
    protected void teardown()
    {
        
        if (_ndexDatabase != null)
        {
            _ndexDatabase.close();
            _ndexDatabase = null;
        }
        
    }

}
