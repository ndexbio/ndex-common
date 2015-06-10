/**
 *   Copyright (c) 2013, 2015
 *  	The Regents of the University of California
 *  	The Cytoscape Consortium
 *
 *   Permission to use, copy, modify, and distribute this software for any
 *   purpose with or without fee is hereby granted, provided that the above
 *   copyright notice and this permission notice appear in all copies.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *   WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *   MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *   ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *   WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *   ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *   OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package org.ndexbio.task.utility;

import org.ndexbio.common.models.dao.orientdb.UserDocDAO;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.exceptions.ObjectNotFoundException;
import org.ndexbio.model.object.NewUser;
import org.ndexbio.model.object.User;

public class DatabaseInitializer {

/*	
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
*/
	public static void createUserIfnotExist(UserDocDAO dao, String accountName, String email, String password) throws NdexException {
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
