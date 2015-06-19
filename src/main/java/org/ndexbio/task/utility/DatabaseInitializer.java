/**
 * Copyright (c) 2013, 2015, The Regents of the University of California, The Cytoscape Consortium
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
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
