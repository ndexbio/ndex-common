package org.ndexbio.common.access;

import org.ndexbio.common.helpers.Configuration;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;


public class NdexAOrientDBDAO {
	
    protected ODatabaseDocumentTx _ndexDatabase = null;
    
    public NdexAOrientDBDAO()
    {
        
    }
    
    protected void setup()
    {
    _ndexDatabase = ODatabaseDocumentPool.global().acquire(
            Configuration.getInstance().getProperty("OrientDB-URL"),
            Configuration.getInstance().getProperty("OrientDB-Username"),
            Configuration.getInstance().getProperty("OrientDB-Password"));
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
