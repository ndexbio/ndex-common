package org.ndexbio.common.models.dao;


import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;


import java.util.UUID;

import org.ndexbio.common.exceptions.DuplicateObjectException;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.models.dao.orientdb.UserDAO;
import org.ndexbio.model.object.SimpleUserQuery;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.model.object.User;
import org.ndexbio.model.object.NewUser;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

public class TestUserDAO extends TestDAO{

	private static UserDAO dao;
	private static NdexDatabase database;
	private static ODatabaseDocumentTx  localConnection;  //all DML will be in this connection, in one transaction.
	private static OrientGraph graph;
	private static User testUser;
	private static User user;
	private static User user2;
	private static User user3;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// For acquiring connections from the pool
		database = new NdexDatabase();
		
		// For use with the Orient Document API
		localConnection = database.getAConnection();
		
		// For use with the Orient Graph API. We do not need to issue a localConnection.begin()
		// to start a Transaction as new OrientGraph() does it already
		graph = new OrientGraph(localConnection);
		
		dao = new UserDAO(localConnection);
		
		// Create some users for testing. 
		NewUser newUser = new NewUser();
        newUser.setEmailAddress("support@ndexbio.org");
        newUser.setPassword("probably-insecure");
        newUser.setAccountName("Support");
        newUser.setFirstName("foo");
        newUser.setLastName("bar");
        user = dao.createNewUser(newUser);
        
        newUser = new NewUser();
        newUser.setEmailAddress("support2@ndexbio.org");
        newUser.setPassword("probably-insecure2");
        newUser.setAccountName("Support2");
        newUser.setFirstName("foo2");
        newUser.setLastName("bar2");
        user2 = dao.createNewUser(newUser);
        
        newUser = new NewUser();
        newUser.setEmailAddress("support3@ndexbio.org");
        newUser.setPassword("probably-insecure3");
        newUser.setAccountName("Support3");
        newUser.setFirstName("foo3");
        newUser.setLastName("bar3");
        user3 = dao.createNewUser(newUser);
        
        localConnection.commit();
        
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		
		localConnection.begin();	
		dao.deleteUserById(user.getExternalId());
		dao.deleteUserById(user2.getExternalId());
		dao.deleteUserById(user3.getExternalId());
		localConnection.commit();
		localConnection.close();
		database.close();
		
	}
	

	// initialize testUser for test suite
	@Before
	public void setup() {
		assertTrue(createTestUser());
	}
	//cleanup testUser
	@After
	public void teardown() {
		assertTrue(deleteTestUser());
	}

	
	@Test
    public void authenticateUser() {
		
        try {
        	
            final User authenticatedUser = dao.authenticateUser(testUser.getAccountName(), "test");
            assertNotNull(authenticatedUser);
            assertEquals(authenticatedUser.getAccountName(), testUser.getAccountName());
            
        } catch (Exception e) {
        	
            fail(e.getMessage());
            e.printStackTrace();
            
        }
        
    }
	
	@Test(expected = SecurityException.class)
    public void authenticateUserInvalid() throws SecurityException, NdexException  {
		// using user constructed in before class
        dao.authenticateUser(user.getAccountName(), "xxxxx");
        
    }

    @Test(expected = SecurityException.class)
    public void authenticateUserInvalidUsername() throws SecurityException, NdexException {
    	// using password for user constructed in before class
        dao.authenticateUser("", "probably-insecure");
        
    }

    @Test(expected = SecurityException.class)
    public void authenticateUserInvalidPassword() throws SecurityException, NdexException {
    	// using user constructed in before class
        dao.authenticateUser(user.getAccountName(), "");
        
    }
	
	@Test
	public void createUser() {
		
		try {
			
			localConnection.begin(); // also aborts any uncommitted transactions.
			
			final NewUser newUser = new NewUser();
            newUser.setEmailAddress("test2");
            newUser.setPassword("test2");
            newUser.setAccountName("test2");
            newUser.setFirstName("test2");
            newUser.setLastName("test2");
            
            assertNotNull(dao.createNewUser(newUser));
           
		} catch (Throwable e){
			
			fail(e.getMessage());
			
		}
		
	}
	
    @Test
    public void getUserByUUID() {
    	
    	try {
    		
    		localConnection.begin(); // also aborts any uncommitted transactions.
	        final User retrievedUser = dao.getUserById(testUser.getExternalId());
	        assertNotNull(retrievedUser);
	        
    	} catch(Throwable e) {
    		
    		fail(e.getMessage());
    		e.printStackTrace();
    		
    	} 
    	
    }
    
    @Test
    public void getUserByAccountName() {
    	
    	try {
    		
    		localConnection.begin(); // also aborts any uncommitted transactions.
	        final User retrievedUser = dao.getUserByAccountName(testUser.getAccountName());
	        assertEquals(retrievedUser.getAccountName(), testUser.getAccountName());
	        assertNotNull(retrievedUser);

    	} catch(Throwable e) {
    		
    		fail(e.getMessage());
    		e.printStackTrace();
    		
    	} 
    	
    }
    
    @Test
    public void findUsers() {
    	
    	try {
	    		
    		localConnection.begin();
	    	final SimpleUserQuery simpleQuery = new SimpleUserQuery();
	    	simpleQuery.setSearchString("support");
	    	
	    	assertTrue(!dao.findUsers(simpleQuery, 0, 5).isEmpty());
    	
		} catch (Exception e) {
			
			fail(e.getMessage());
			e.printStackTrace();
			
		} 
    	
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void findUsersInvalid() throws IllegalArgumentException, NdexException {
        dao.findUsers(null,0,0);
    }
    
	@Test(expected = NdexException.class)
	public void deleteUserByUUID() throws NdexException {

		final UUID id = testUser.getExternalId();
		localConnection.begin();
		dao.deleteUserById(id);
		testUser = null;
		localConnection.commit();
		localConnection.begin();
		try { 
		dao.getUserById(id);
		} catch(NdexException e) {
			throw e;
		} finally {
			assertTrue(createTestUser());
		}
		
	}
	
	@Test(expected = IllegalArgumentException.class)
    public void createUserInvalid() throws IllegalArgumentException, NdexException {
		
		localConnection.begin();
        dao.createNewUser(null);
        
    }
	
	@Test(expected = IllegalArgumentException.class)
	public void createUserInvalidAccountName() throws NdexException, IllegalArgumentException {
		
			localConnection.begin(); // also aborts any uncommitted transactions.
			
			final NewUser newUser = new NewUser();
            newUser.setEmailAddress("test");
            newUser.setPassword("test");
            newUser.setAccountName("");
            newUser.setFirstName("test");
            newUser.setLastName("test");
            
            dao.createNewUser(newUser);
            
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void createUserInvalidPassword() throws NdexException, IllegalArgumentException {
		
			localConnection.begin(); // also aborts any uncommitted transactions.
			
			final NewUser newUser = new NewUser();
            newUser.setEmailAddress("test");
            newUser.setPassword("");
            newUser.setAccountName("test");
            newUser.setFirstName("test");
            newUser.setLastName("test");
            
            dao.createNewUser(newUser);
            
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void createUserInvalidEmail() throws NdexException, IllegalArgumentException {
		
			localConnection.begin(); // also aborts any uncommitted transactions.
			
			final NewUser newUser = new NewUser();
            newUser.setEmailAddress("");
            newUser.setPassword("test");
            newUser.setAccountName("test");
            newUser.setFirstName("test");
            newUser.setLastName("test");
            
            dao.createNewUser(newUser);
            
	}
	
	@Test(expected = DuplicateObjectException.class)
	public void createUserExistingUser() throws IllegalArgumentException, NdexException, DuplicateObjectException {
		
				localConnection.begin(); // also aborts any uncommitted transactions.	
	  
				final NewUser newUser = new NewUser();
	            newUser.setEmailAddress("test");
	            newUser.setPassword("test");
	            newUser.setAccountName("test");
	            newUser.setFirstName("test");
	            newUser.setLastName("test");
				
	            dao.createNewUser(newUser);
			
	}
	
	@Test(expected = ObjectNotFoundException.class)
	public void deleteUser_nonExistant() throws NdexException, ObjectNotFoundException {
	
			localConnection.begin(); // also aborts any uncommitted transactions.
            dao.deleteUserById(NdexUUIDFactory.INSTANCE.getNDExUUID());
            
	} 
	
	/*@Test
	public void emailNewPassword() {
		 //fail("Does this verify complete funcitonality?");
	    try {
		   
           assertEquals(dao.emailNewPassword("Support").getStatus(), 200);
		   
	    } catch (Exception e)  {
		   
           fail(e.getMessage());
           e.printStackTrace();
           
	    } 
	        
	}
	/*
	@Test(expected = IllegalArgumentException.class)
    public void emailNewPasswordInvalid() throws IllegalArgumentException, NdexException {
        dao.emailNewPassword("");
    }*/
	
	@Test
    public void changePassword() {
		
        try {
        	
            dao.changePassword("not-secure", testUser.getExternalId());
            
            User authenticatedUser = dao.authenticateUser(testUser.getAccountName(), "not-secure");
            assertNotNull(authenticatedUser);
            
            dao.changePassword("test", testUser.getExternalId());
            authenticatedUser = dao.authenticateUser(testUser.getAccountName(), "test");
            
            assertNotNull(authenticatedUser);
            
        } catch (Exception e) {
        	
            fail(e.getMessage());
            e.printStackTrace();
            
        } 
    }
	
	@Test(expected = IllegalArgumentException.class)
    public void changePasswordInvalid() throws IllegalArgumentException, NdexException {
		
		try {
			
			dao.changePassword("", testUser.getExternalId());
			
		} catch (Throwable e){
			
			throw e;
			
		}
    }
	
	@Test
    public void updateUser() {
        try {
        	
            //User user = dao.getUserById(testUser.getExternalId());
            final User updated = new User();
            updated.setDescription("changed");
            
            localConnection.begin();
            dao.updateUser(updated, testUser.getExternalId());
            localConnection.commit();
            
            assertEquals(updated.getDescription(), dao.getUserById(testUser.getExternalId()).getDescription());
            assertEquals(testUser.getEmailAddress(), dao.getUserById(testUser.getExternalId()).getEmailAddress());
            
        } catch (Exception e) {
        	
            fail(e.getMessage());
            e.printStackTrace();
            
        } 
        
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateUserInvalid() throws IllegalArgumentException, SecurityException, NdexException {
    	
        dao.updateUser(null, user.getExternalId());
        
    }

	private boolean createTestUser() {
		
		try {
			
			localConnection.begin();
			final NewUser newUser = new NewUser();
            newUser.setEmailAddress("test");
            newUser.setPassword("test");
            newUser.setAccountName("test");
            newUser.setFirstName("test");
            newUser.setLastName("test");
			
	        testUser = dao.createNewUser(newUser);
	        localConnection.commit();
        
        	return true;
        	
		} catch (Throwable e) {
			
			return false;
			
		}
	}
	
	private boolean deleteTestUser() {
		
		try {
			
			localConnection.begin();
			dao.deleteUserById(testUser.getExternalId());
			localConnection.commit();
			testUser = null;
			
			return true;
			
		} catch (Throwable e) {
			
			return false;
			
		}
		
	}
	
}
