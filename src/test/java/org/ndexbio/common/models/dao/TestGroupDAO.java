package org.ndexbio.common.models.dao;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.ndexbio.common.exceptions.DuplicateObjectException;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.helpers.IdConverter;
import org.ndexbio.common.models.object.SearchParameters;
import org.ndexbio.common.models.object.privilege.Group;
import org.ndexbio.common.models.object.privilege.Membership;
import org.ndexbio.common.models.object.privilege.Permissions;

import com.orientechnologies.orient.core.id.ORID;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGroupDAO extends TestDAO
{
    private static final GroupDAO dao = DAOFactorySupplier.INSTANCE.resolveDAOFactoryByType(CommonDAOValues.ORIENTDB_DAO_TYPE)
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
}