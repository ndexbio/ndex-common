package org.ndexbio.common.models.dao;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.dao.orientdb.OrientdbDAO;
import org.ndexbio.common.models.data.IUser;
import org.ndexbio.model.object.User;

import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

public abstract class TestDAO extends OrientdbDAO {
	 protected static final Properties _testProperties = new Properties();
	 protected String testUserId = "C31R0";
	 protected String testUserName = "biologist1";
	 protected String testPassword = "bio1";
	 
	 protected String searchGroupName = "calm1project";
	 protected String testNetworkName = "REACTOME:G1 Phase";
	 protected String testRquestMessage = "This is a test message";
	 protected String testGroupName = "Test Group";
	 
	  // @BeforeClass
    public static void initializeTests() throws Exception
    {
        final InputStream propertiesStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("ndex.properties");
        _testProperties.load(propertiesStream);

        
    }
    
   // @AfterClass
    public static void cleanUp()
    {
        
    }

    
    
    //@After
    public void resetDatabase()
    {
     this.teardownDatabase();   
    }

    //@Before
    public void setTestUser()
    {
        //User testUser = getUser("dbowner");
        //this.testUserId = testUser.getId();
    }
    
    
	
	  /**************************************************************************
	    * Gets the record ID of an object by its name from the database.
	    * 
	    * @param objectName
	    *            The name of the object.
	    * @return An ORID object containing the record ID.
	 * @throws NdexException 
	    **************************************************************************/
	    protected ORID getRid(String objectName) throws IllegalArgumentException, NdexException
	    {
	        objectName = objectName.replace("'", "\\'");
	        this.setupDatabase();
	        final List<ODocument> matchingUsers = _ndexDatabase.query(new OSQLSynchQuery<Object>("select from User where username = '" + objectName + "'"));
	        if (!matchingUsers.isEmpty())
	            return (ORID)_orientDbGraph.getVertex(matchingUsers.get(0)).getId();
	        
	        final List<ODocument> matchingGroups = _ndexDatabase.query(new OSQLSynchQuery<Object>("select from Group where name = '" + objectName + "'"));
	        if (!matchingGroups.isEmpty())
	            return (ORID)_orientDbGraph.getVertex(matchingGroups.get(0)).getId();

	        final List<ODocument> matchingNetworks = _ndexDatabase.query(new OSQLSynchQuery<Object>("select from Network where name = '" + objectName + "'"));
	        if (!matchingNetworks.isEmpty())
	            return (ORID)_orientDbGraph.getVertex(matchingNetworks.get(0)).getId();

	        final List<ODocument> matchingRequests = _ndexDatabase.query(new OSQLSynchQuery<Object>("select from Request where message = '" + objectName + "'"));
	        if (!matchingRequests.isEmpty())
	            return (ORID)_orientDbGraph.getVertex(matchingRequests.get(0)).getId();

	        final List<ODocument> matchingTasks = _ndexDatabase.query(new OSQLSynchQuery<Object>("select from Task where description = '" + objectName + "'"));
	        if (!matchingTasks.isEmpty())
	            return (ORID)_orientDbGraph.getVertex(matchingTasks.get(0)).getId();
	        
	        throw new IllegalArgumentException(objectName + " is not a user, group, network, request, or task.");
	    }

	    /**************************************************************************
	    * Queries the database for the user's ID by the username. Necessary to
	    * mock the logged in user.
	    * 
	    * @param username
	    *            The username.
	     * @throws NdexException 
	    **************************************************************************/
	    protected User getUser(final String username) throws NdexException
	    {
	    	this.setupDatabase();
	        final List<ODocument> matchingUsers = _ndexDatabase.query(new OSQLSynchQuery<Object>("select from User where username = '" + username + "'"));
	        
	        if (!matchingUsers.isEmpty())
	            return new User();//_orientDbGraph.getVertex(matchingUsers.get(0), IUser.class), true);
	        else
	            return null;
	    }
	    

}
