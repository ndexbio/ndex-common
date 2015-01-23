package org.ndexbio.task.utility;

import org.ndexbio.common.access.NdexAOrientDBConnectionPool;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.models.dao.orientdb.UserDAO;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.NewUser;
import org.ndexbio.model.object.User;
import org.ndexbio.task.Configuration;

public class DatabaseInitializer {

	
	public static void main(String[] args) {
		
		try {

	    	// read configuration
	    	Configuration configuration = Configuration.getInstance();
	    	
	    	//and initialize the db connections
	    	NdexAOrientDBConnectionPool.createOrientDBConnectionPool(
	    			configuration.getDBURL(),
	    			configuration.getDBUser(),
	    			configuration.getDBPasswd(), 1);
	    	
	    	
			NdexDatabase db = new NdexDatabase(configuration.getHostURI());			
			System.out.println("Database initialized.");
            db.close();
            
		} catch (NdexException e) {
			System.err.println ("Error accurs when initializing Ndex database. " +  
					e.getMessage());
		} finally {
			NdexAOrientDBConnectionPool.close();
		}
	}

	public static void createUserIfnotExist(UserDAO dao, String accountName, String email, String password) throws NdexException {
		try {
			User u = dao.getUserByAccountName(accountName);
			if ( u!= null) return;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			throw new NdexException ("Failed to create new user after creating database. " + e.getMessage());
		} catch ( ObjectNotFoundException e2) {
			
		}
		
		NewUser newUser = new NewUser();
        newUser.setEmailAddress(email);
        newUser.setPassword(password);
        newUser.setAccountName(accountName);
        newUser.setFirstName("");
        newUser.setLastName("");
        dao.createNewUser(newUser);
        

	}
	
}
