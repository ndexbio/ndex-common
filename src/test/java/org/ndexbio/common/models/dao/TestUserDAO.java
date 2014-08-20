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
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

public class TestUserDAO extends TestDAO{

	private static UserDAO dao;
	private static NdexDatabase database;
	private static ODatabaseDocumentTx  localConnection;  //all DML will be in this connection, in one transaction.
	private static OrientGraphNoTx graph;
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
		
		graph = new OrientGraphNoTx(localConnection);
		
		dao = new UserDAO(localConnection, graph);
		
		// Create some users for testing. 
		NewUser newUser = new NewUser();
        newUser.setEmailAddress("test@test.org");
        newUser.setPassword("test");
        newUser.setAccountName("test");
        newUser.setFirstName("test");
        newUser.setLastName("test");
        user = dao.createNewUser(newUser);
        
        newUser = new NewUser();
        newUser.setEmailAddress("test2@test.org");
        newUser.setPassword("test2");
        newUser.setAccountName("test2");
        newUser.setFirstName("test2");
        newUser.setLastName("test2");
        user2 = dao.createNewUser(newUser);
        
        newUser = new NewUser();
        newUser.setEmailAddress("test3@test.org");
        newUser.setPassword("test3");
        newUser.setAccountName("test3");
        newUser.setFirstName("test3");
        newUser.setLastName("test3");
        user3 = dao.createNewUser(newUser);
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		
//		dao.deleteUserById(user.getExternalId());
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

            final User authenticatedUser = dao.authenticateUser(testUser.getAccountName(), "testUser");
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
	
	/*@Test
	public void createUser() {
		
		try {
			
			final NewUser newUser = new NewUser();
            newUser.setEmailAddress("create");
            newUser.setPassword("create");
            newUser.setAccountName("create");
            newUser.setFirstName("create");
            newUser.setLastName("create");
            
            assertNotNull(dao.createNewUser(newUser));
            
            localConnection.rollback();
           
		} catch (Throwable e){
			
			fail(e.getMessage());
			
		}
		
	}*/
	
    @Test
    public void getUserByUUID() {
    	
    	try {

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

	    	final SimpleUserQuery simpleQuery = new SimpleUserQuery();
	    	simpleQuery.setSearchString("test");
	    	
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
    
	@Test(expected = ObjectNotFoundException.class)
	public void deleteUserByUUID() throws ObjectNotFoundException {

		final UUID id = testUser.getExternalId();

		try {
			dao.deleteUserById(id);
			testUser = null;
			
			dao.getUserById(id);
		} catch(ObjectNotFoundException e) {
			throw e;
		} catch (Exception e) {
			fail(e.getMessage());
		} finally {
			assertTrue(createTestUser());
		}
		
	}
	
	@Test(expected = IllegalArgumentException.class)
    public void createUserInvalid() throws IllegalArgumentException, NdexException {

        dao.createNewUser(null);
        
    }
	
	@Test(expected = IllegalArgumentException.class)
	public void createUserInvalidAccountName() throws NdexException, IllegalArgumentException {

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
	*/
	/*
	@Test(expected = IllegalArgumentException.class)
    public void emailNewPasswordInvalid() throws IllegalArgumentException, NdexException {
        dao.emailNewPassword("");
    }*/
	
	@Test
    public void changePassword() {
		
        try {
        	

            dao.changePassword("not-secure", testUser.getExternalId());
            //localConnection.commit();
            
            User authenticatedUser = dao.authenticateUser(testUser.getAccountName(), "not-secure");
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
			
		} catch (Exception e){
			
			throw e;
			
		}
    }
	
	@Test
    public void updateUser() {
        try {
        	
            //User user = dao.getUserById(testUser.getExternalId());
            final User updated = new User();
            updated.setDescription("changed");

            dao.updateUser(updated, testUser.getExternalId());
            //localConnection.commit();
            
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
			
			//localConnection.begin();
			final NewUser newUser = new NewUser();
            newUser.setEmailAddress("testUser");
            newUser.setPassword("testUser");
            newUser.setAccountName("testUser");
            newUser.setFirstName("testUser");
            newUser.setLastName("testUser");
			
	        testUser = dao.createNewUser(newUser);
	        //localConnection.commit();
        
        	return true;
        	
		} catch (Throwable e) {
			
			return false;
			
		}
	}
	
	private boolean deleteTestUser() {
		
		try {
			//localConnection.begin();
			dao.deleteUserById(testUser.getExternalId());
			//localConnection.commit();
			testUser = null;
			
			return true;
			
		} catch (Throwable e) {
			
			return false;
			
		}
		
	}
	
}
