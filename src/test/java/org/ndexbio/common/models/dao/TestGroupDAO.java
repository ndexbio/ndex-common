package org.ndexbio.common.models.dao;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;
import org.ndexbio.common.exceptions.DuplicateObjectException;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.models.dao.orientdb.UserDAO;
import org.ndexbio.common.models.dao.orientdb.GroupDAO;
import org.ndexbio.model.object.SimpleUserQuery;
import org.ndexbio.model.object.Group;
import org.ndexbio.model.object.Membership;
import org.ndexbio.model.object.MembershipType;
import org.ndexbio.model.object.Permissions;
import org.ndexbio.model.object.User;
import org.ndexbio.model.object.NewUser;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;


public class TestGroupDAO 
{
	private static UserDAO	userDAO;
	private static GroupDAO dao;
	private static NdexDatabase database;
	private static ODatabaseDocumentTx  localConnection;  //all DML will be in this connection, in one transaction.
	private static OrientGraphNoTx graph;
	
	private static User testUserGroupOwner;
	private static User testUser;
	private static Group testGroup;
	private static Group group;
	private static Group group2;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// For acquiring connections from the pool
		database = new NdexDatabase();
		localConnection = database.getAConnection();
		graph = new OrientGraphNoTx(localConnection);
		userDAO = new UserDAO(localConnection, graph);
		dao = new GroupDAO(localConnection, graph);
		
		//localConnection.begin();
		final NewUser newUser = new NewUser();
        newUser.setEmailAddress("testUserGroupOwner");
        newUser.setPassword("testUserGroupOwner");
        newUser.setAccountName("testUserGroupOwner");
        newUser.setFirstName("testUserGroupOwner");
        newUser.setLastName("testUserGroupOwner");
		
        testUserGroupOwner = userDAO.createNewUser(newUser);
        graph.commit();
        //localConnection.begin();
		
		Group newGroup = new Group();
        newGroup.setOrganizationName("group");
        newGroup.setAccountName("group");
        newGroup.setDescription("group");
        newGroup.setWebsite("group");
        group = dao.createNewGroup(newGroup, testUserGroupOwner.getExternalId());
        
        //localConnection.commit();
        
        newGroup = new Group();
        newGroup.setOrganizationName("group2");
        newGroup.setAccountName("group2");
        newGroup.setDescription("group2");
        newGroup.setWebsite("group2");
        group2 = dao.createNewGroup(newGroup, testUserGroupOwner.getExternalId());
        
        //localConnection.commit();
        
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		
		//localConnection.begin();
		dao.deleteGroupById(group.getExternalId(), testUserGroupOwner.getExternalId());
		dao.deleteGroupById(group2.getExternalId(), testUserGroupOwner.getExternalId());
		userDAO.deleteUserById(testUserGroupOwner.getExternalId());
		//localConnection.commit();
		localConnection.close();
		database.close();
		
	}

	// initialize testGroup for test suite
	@Before
	public void setup() {
		createTestUser();
		createTestGroup();
	}
	//cleanup testGroup
	@After
	public void teardown() {
		deleteTestGroup();
	//	deleteTestUser();
	}
	
	
	@Test
	public void createGroup() {
		
		try {
			localConnection.begin();
			final Group newGroup = new Group();
            newGroup.setOrganizationName("create");
            newGroup.setAccountName("create");
            newGroup.setDescription("create");
            newGroup.setWebsite("create");
            
            assertNotNull(dao.createNewGroup(newGroup, testUser.getExternalId()));
            localConnection.rollback();
           
		} catch (Throwable e){
			
			fail(e.getMessage());
			
		}
		
	}
	
	@Test(expected = IllegalArgumentException.class)
    public void createGroupInvalid() throws IllegalArgumentException, NdexException {
		
        dao.createNewGroup(null, testUserGroupOwner.getExternalId());
        
    }
	
	@Test(expected = IllegalArgumentException.class)
	public void createGroupInvalidAccountName() throws NdexException, IllegalArgumentException {
		
			final Group newGroup = new Group();
            newGroup.setOrganizationName("test");
            newGroup.setAccountName("");
            newGroup.setDescription("test");
            newGroup.setWebsite("test");
            
            dao.createNewGroup(newGroup, testUserGroupOwner.getExternalId());
            
	}
	
	@Test(expected = DuplicateObjectException.class)
	public void createExistingGroup() throws IllegalArgumentException, NdexException, DuplicateObjectException {
		
				final Group newGroup = new Group();
	            newGroup.setOrganizationName("testGroup");
	            newGroup.setAccountName("testGroup");
	            newGroup.setDescription("testGroup");
	            newGroup.setWebsite("testGroup");
				
	            dao.createNewGroup(newGroup, testUserGroupOwner.getExternalId());
			
	}
	
	@Test(expected = NdexException.class)
	public void deleteGroupInvalidAdmin() throws ObjectNotFoundException, NdexException {
		
		//testUserGroupOwner does not admin testGroup
		
		dao.deleteGroupById(testGroup.getExternalId(), testUserGroupOwner.getExternalId());
		
	}
	
	@Test
    public void getGroupByUUID() {
    	
    	try {
    		
	        final Group retrievedGroup = dao.getGroupById(testGroup.getExternalId());
	        assertNotNull(retrievedGroup);
	        
    	} catch(Exception e) {
    		
    		fail(e.getMessage());
    		e.printStackTrace();
    		
    	} 
    	
    }
	
	@Test
    public void getGroupByAccountName() {
    	
    	try {
    		
	        final Group retrievedGroup = dao.getGroupByAccountName(testGroup.getAccountName());
	        assertNotNull(retrievedGroup);
	        
    	} catch(Exception e) {
    		
    		fail(e.getMessage());
    		e.printStackTrace();
    		
    	} 
    	
    }
	
	@Test
    public void updateGroup() {
        try {
        	
            //Group Group = dao.getGroupById(testGroup.getExternalId());
            final Group updated = new Group();
            updated.setDescription("changed");
            
            dao.updateGroup(updated, testGroup.getExternalId(), testUser.getExternalId());
            //localConnection.commit();
            
            assertEquals(updated.getDescription(), dao.getGroupById(testGroup.getExternalId()).getDescription());
            assertEquals(testGroup.getOrganizationName(), dao.getGroupById(testGroup.getExternalId()).getOrganizationName());
            
        } catch (Exception e) {
        	
            fail(e.getMessage());
            e.printStackTrace();
            
        } 
        
    }
	
	 @Test(expected = IllegalArgumentException.class)
	    public void updateGroupInvalid() throws IllegalArgumentException, SecurityException, NdexException {
	    	
	        dao.updateGroup(null, group.getExternalId(), testUserGroupOwner.getExternalId());
	        
	    }
	
	  @Test
	    public void findGroups() {
	    	
	    	try {
		    		
		    	final SimpleUserQuery simpleQuery = new SimpleUserQuery();
		    	simpleQuery.setSearchString("test");
		    	
		    	assertTrue(!dao.findGroups(simpleQuery, 0, 5).isEmpty());
	    	
			} catch (Exception e) {
				
				fail(e.getMessage());
				e.printStackTrace();
				
			} 
	    	
	    }
	  
	  @Test(expected = IllegalArgumentException.class)
	    public void findGroupsInvalid() throws IllegalArgumentException, NdexException {
	        dao.findGroups(null,0,0);
	    }

	 @Test
	  public void updateMembershipAddMember() throws ObjectNotFoundException, NdexException {
		 User member = null;
		 
		 try {
			 
			 //localConnection.begin();
			  final NewUser newUser = new NewUser();
	          newUser.setEmailAddress("member");
	          newUser.setPassword("member");
	          newUser.setAccountName("member");
	          newUser.setFirstName("member");
	          newUser.setLastName("member");
				
		      member = userDAO.createNewUser(newUser);
		      //localConnection.commit();
			  
			  Membership membership = new Membership();
			  membership.setMemberAccountName(member.getAccountName());
			  membership.setMemberUUID(member.getExternalId());
			  membership.setPermissions(Permissions.WRITE);
			  membership.setMembershipType(MembershipType.GROUP);
			  
			  dao.updateMember(membership, testGroup.getExternalId(), testUser.getExternalId());
			  
			  //localConnection.commit();
		  
		 } catch(Exception e) {
			 fail(e.getMessage());
		 } finally {
			  userDAO.deleteUserById(member.getExternalId());
			  //localConnection.commit();
		 }
		 
	  }
	 @Test
	  public void updateMembershipChangeAdmin() throws ObjectNotFoundException, NdexException {
		  
		 User member = null;
		 try {
			 //localConnection.begin();
			  final NewUser newUser = new NewUser();
	         newUser.setEmailAddress("member");
	         newUser.setPassword("member");
	         newUser.setAccountName("member");
	         newUser.setFirstName("member");
	         newUser.setLastName("member");
				
		      member = userDAO.createNewUser(newUser);
		      
		      //localConnection.commit();
		      //localConnection.begin();
		      
			  Membership membership = new Membership();
			  membership.setMemberAccountName(member.getAccountName());
			  membership.setMemberUUID(member.getExternalId());
			  membership.setPermissions(Permissions.ADMIN);
			  membership.setMembershipType(MembershipType.GROUP);
			  
			  dao.updateMember(membership, testGroup.getExternalId(), testUser.getExternalId());
			  
			 // localConnection.commit();
			  //localConnection.begin();
			  
			  membership = new Membership();
			  membership.setMemberAccountName(testUser.getAccountName());
			  membership.setMemberUUID(testUser.getExternalId());
			  membership.setPermissions(Permissions.READ);
			  membership.setMembershipType(MembershipType.GROUP);
			  
			  dao.updateMember(membership, testGroup.getExternalId(), testUser.getExternalId());
			  
			  //localConnection.commit();
			  
			  membership = new Membership();
			  membership.setMemberAccountName(testUser.getAccountName());
			  membership.setMemberUUID(testUser.getExternalId());
			  membership.setPermissions(Permissions.ADMIN);
			  membership.setMembershipType(MembershipType.GROUP);
			  
			  dao.updateMember(membership, testGroup.getExternalId(), member.getExternalId());
			  
			  //localConnection.commit();
			  
		 } catch (Exception e) {
			 fail(e.getMessage());
		 } finally {
			 userDAO.deleteUserById(member.getExternalId());
			 //localConnection.commit();
		 }
		  
	  }
	
	 @Test(expected = NdexException.class)
	  public void updateMembershipInvalidAdminChange() throws ObjectNotFoundException, NdexException {
		  
			  Membership membership = new Membership();
			  membership.setMemberAccountName(testUser.getAccountName());
			  membership.setMemberUUID(testUser.getExternalId());
			  membership.setPermissions(Permissions.READ);
			  membership.setMembershipType(MembershipType.GROUP);
			  
			  dao.updateMember(membership, testGroup.getExternalId(), testUser.getExternalId());
			  
	  }
	 
	private void createTestGroup() {
		
		try {
			
			final Group newGroup = new Group();
            newGroup.setOrganizationName("testGroup");
            newGroup.setAccountName("testGroup");
            newGroup.setDescription("testGroup");
            newGroup.setWebsite("testGroup");
			
	        testGroup = dao.createNewGroup(newGroup, testUser.getExternalId());
	        localConnection.commit();
        
        	//return true;
        	
		} catch (Exception e) {
			//System.out.println(e.getMessage());
			fail(e.getMessage());
			
		}
	}
	
	private void deleteTestGroup() {
		
		try {
			
			dao.deleteGroupById(testGroup.getExternalId(), testUser.getExternalId());
			//localConnection.commit();
			testGroup = null;
			
			//return true;
			
		} catch (Exception e) {
			
			fail(e.getMessage());
			
		}
		
	}
	
	private void createTestUser() {
		
		try {
			
			final NewUser newUser = new NewUser();
            newUser.setEmailAddress("testUser");
            newUser.setPassword("testUser");
            newUser.setAccountName("testUser");
            newUser.setFirstName("testUser");
            newUser.setLastName("testUser");
			
	        testUser = userDAO.createNewUser(newUser);
	        //localConnection.commit();
        
        	//return true;
        	
		} catch (Throwable e) {
			
			fail(e.getMessage());
			
		}
	}
	
/*	private void deleteTestUser() {
		
		try {
			
			userDAO.deleteUserById(testUser.getExternalId());
			//localConnection.commit();
			testUser = null;
			
			//return true;
			
		} catch (Throwable e) {
			
			fail(e.getMessage());
			
		}
		
	} */
	
  /*  private static final GroupDAO dao = DAOFactorySupplier.INSTANCE.resolveDAOFactoryByType(CommonDAOValues.ORIENTDB_DAO_TYPE)
			.get().getGroupDAO();
    
    
   @Test
    public void createGroup()
    {
        Assert.assertTrue(createNewGroup());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void createGroupInvalid() throws IllegalArgumentException, NdexException
    {
       dao.createGroup(null, testUserId);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void createGroupInvalidName() throws IllegalArgumentException, NdexException
    {
        final Group newGroup = new Group();
        newGroup.setDescription("This is a test group.");
        newGroup.setOrganizationName("Unit Tested Group");
        newGroup.setWebsite("http://www.ndexbio.org");
        
        this.dao.createGroup(null,testUserId);
    }
    
   @Test
    public void deleteGroup() throws IllegalArgumentException, NdexException
    {
    
        final ORID testGroupRid = getRid(this.testGroupName);
        Assert.assertTrue(deleteTargetGroup(IdConverter.toJid(testGroupRid)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteGroupInvalid() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        dao.deleteGroup("","");
    }

    @Test(expected = ObjectNotFoundException.class)
    public void deleteGroupNonexistant() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        dao.deleteGroup("C999R999",testUserId);
    }

    @Test
    public void findGroups()
    {
        final SearchParameters searchParameters = new SearchParameters();
        searchParameters.setSearchString(this.searchGroupName);
        searchParameters.setSkip(0);
        searchParameters.setTop(25);
        
        try
        {
            dao.findGroups(searchParameters, "contains");
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void findGroupsInvalid() throws IllegalArgumentException, NdexException
    {
        final SearchParameters searchParameters = new SearchParameters();
        searchParameters.setSearchString("");
        searchParameters.setSkip(0);
        searchParameters.setTop(25);
        
       dao.findGroups(null, null);
    }

    @Test
    public void getGroupById()
    {
        try
        {
            final ORID groupRid = getRid(this.searchGroupName);
            final Group testGroup = dao.getGroup(IdConverter.toJid(groupRid));
            Assert.assertNotNull(testGroup);
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void getGroupByName()
    {
        try
        {
            final Group testGroup = dao.getGroup(this.searchGroupName);
            Assert.assertNotNull(testGroup);
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void getGroupInvalid() throws IllegalArgumentException, NdexException
    {
        dao.getGroup("");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void removeMemberInvalidGroup() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        final ORID userId = getRid(testUserName);

        dao.removeMember("", IdConverter.toJid(userId),testUserId);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void removeMemberInvalidUserId() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        final ORID testGroupRid = getRid(this.searchGroupName);

        dao.removeMember(IdConverter.toJid(testGroupRid), "","");
    }
    
    @Test(expected = ObjectNotFoundException.class)
    public void removeMemberNonexistantGroup() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        final ORID userId = getRid(testUserName);

        dao.removeMember("C999R999", IdConverter.toJid(userId),testUserId);
    }
    
    @Test(expected = Exception.class)
    public void removeMemberNonexistantUser() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        final ORID testGroupRid = getRid(this.searchGroupName);

        dao.removeMember(IdConverter.toJid(testGroupRid), "C999R999",testUserId);
    }
    
    @Test(expected = Exception.class)
    public void removeMemberOnlyAdminMember() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        final ORID testGroupRid = getRid("triptychjs");
        final ORID userId = getRid(testUserName);

        dao.removeMember(IdConverter.toJid(testGroupRid), IdConverter.toJid(userId),testUserId);
    }

    @Test
    public void updateGroup()
    {
        try
        {
            Assert.assertTrue(createNewGroup());
            
            final ORID testGroupRid = getRid(testGroupName);
            final Group testGroup = dao.getGroup(IdConverter.toJid(testGroupRid));

            testGroup.setName("Updated Test Group");
            dao.updateGroup(testGroup, testUserId);
            Assert.assertEquals(dao.getGroup(testGroup.getId()).getName(), testGroup.getName());

            Assert.assertTrue(deleteTargetGroup(testGroup.getId()));
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    //@Test(expected = IllegalArgumentException.class)
    public void updateGroupInvalid() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        dao.updateGroup(null,testUserId);
    }

    //@Test(expected = ObjectNotFoundException.class)
    public void updateGroupNonexistant() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        final Group updatedGroup = new Group();
        updatedGroup.setId("C999R999");
        updatedGroup.setDescription("This is a test group.");
        updatedGroup.setName(testGroupName);
        updatedGroup.setOrganizationName("Unit Tested Group");
        updatedGroup.setWebsite("http://www.ndexbio.org");

        dao.updateGroup(updatedGroup,testUserId);
    }
    
    //@Test(expected = IllegalArgumentException.class)
    public void updateMemberInvalidGroup() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        final ORID testUserId = getRid(testUserName);
        
        final Membership testMembership = new Membership();
        testMembership.setPermissions(Permissions.READ);
        testMembership.setResourceId(IdConverter.toJid(testUserId));
        testMembership.setResourceName(testUserName);

        dao.updateMember("", testMembership,"");
    }
    
    //@Test(expected = IllegalArgumentException.class)
    public void updateMemberInvalidMembership() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        final ORID testGroupRid = getRid(this.searchGroupName);
        
        dao.updateMember(IdConverter.toJid(testGroupRid), null,"");
    }
    
    //@Test(expected = Exception.class)
    public void updateMemberInvalidUserId() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        final ORID testGroupRid = getRid(this.searchGroupName);
        
        final Membership testMembership = new Membership();
        testMembership.setPermissions(Permissions.READ);
        testMembership.setResourceId("C999R999");
        testMembership.setResourceName(testUserName);

        dao.updateMember(IdConverter.toJid(testGroupRid), testMembership, testUserId);
    }
    
    //@Test(expected = ObjectNotFoundException.class)
    public void updateMemberNonexistantGroup() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        final ORID orid = getRid(testUserName);
        
        final Membership testMembership = new Membership();
        testMembership.setPermissions(Permissions.READ);
        testMembership.setResourceId(IdConverter.toJid(orid));
        testMembership.setResourceName(testUserName);

        dao.updateMember("C999R999", testMembership,testUserId);
    }
    
    //@Test(expected = Exception.class)
    public void updateMemberNonexistantUser() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        final ORID testGroupRid = getRid(this.searchGroupName);
        
        final Membership testMembership = new Membership();
        testMembership.setPermissions(Permissions.READ);
        testMembership.setResourceId("C999R999");
        testMembership.setResourceName(testUserName);

        dao.updateMember(IdConverter.toJid(testGroupRid), testMembership, testUserId);
    }
    
    //@Test(expected = Exception.class)
    public void updateMemberOnlyAdminMember() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException
    {
        final ORID testGroupRid = getRid(this.searchGroupName);
        final ORID orid = getRid(testUserName);
        
        final Membership testMembership = new Membership();
        testMembership.setPermissions(Permissions.READ);
        testMembership.setResourceId(IdConverter.toJid(orid));
        testMembership.setResourceName(testUserName);

        dao.updateMember(IdConverter.toJid(testGroupRid), testMembership, testUserId);
    }


    
    
    private boolean createNewGroup()
    {
        final Group newGroup = new Group();
        newGroup.setDescription("This is a test group.");
        newGroup.setName(testGroupName);
        newGroup.setOrganizationName("Unit Tested Group");
        newGroup.setWebsite("http://www.ndexbio.org");

        try
        {
            final Group createdGroup = dao.createGroup(newGroup, testUserId);
            Assert.assertNotNull(createdGroup);
            
            return true;
        }
        catch (DuplicateObjectException doe)
        {
            return true;
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    private boolean deleteTargetGroup(String groupId)
    {
        try
        {
            dao.deleteGroup(groupId,testUserId);
            Assert.assertNull(dao.getGroup(groupId));
            
            return true;
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    */
}