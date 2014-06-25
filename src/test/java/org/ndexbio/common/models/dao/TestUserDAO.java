package org.ndexbio.common.models.dao;

import java.util.Collection;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.ndexbio.common.exceptions.DuplicateObjectException;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.helpers.IdConverter;
import org.ndexbio.common.models.data.IUser;
import org.ndexbio.common.models.object.SearchParameters;
import org.ndexbio.common.models.object.privilege.NewUser;
import org.ndexbio.common.models.object.privilege.User;

import com.orientechnologies.orient.core.id.ORID;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestUserDAO extends TestDAO {
	
	
	private static final UserDAO dao = DAOFactorySupplier.INSTANCE.
			resolveDAOFactoryByType(CommonDAOValues.ORIENTDB_DAO_TYPE)
			.get().getUserDAO();
	
	 @Test
    public void addNetworkToWorkSurface()
    {
        Assert.assertTrue(putNetworkOnWorkSurface());
    }

    @Test(expected = IllegalArgumentException.class)
    public void addNetworkToWorkSurfaceInvalid() throws IllegalArgumentException, ObjectNotFoundException, NdexException
    {
        dao.addNetworkToWorkSurface("",this.testUserId);
    }

    @Test
    public void authenticateUser()
    {
        try
        {
            final User authenticatedUser = dao.authenticateUser(this.testUserName, this.testPassword);
            Assert.assertNotNull(authenticatedUser);
            Assert.assertEquals(authenticatedUser.getUsername(), this.testUserName);
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test(expected = SecurityException.class)
    public void authenticateUserInvalid() throws SecurityException, NdexException
    {
        dao.authenticateUser(this.testUserName, "xxxxx");
    }

    @Test(expected = SecurityException.class)
    public void authenticateUserInvalidUsername() throws SecurityException, NdexException
    {
        dao.authenticateUser("", "insecure");
    }

    @Test(expected = SecurityException.class)
    public void authenticateUserInvalidPassword() throws SecurityException, NdexException
    {
        dao.authenticateUser(this.testUserName, "");
    }

    //@Test
    public void changePassword()
    {
        try
        {
            dao.changePassword("not-secure", this.testUserId);
            
            User authenticatedUser = dao.authenticateUser(this.testUserName, "not-secure");
            Assert.assertNotNull(authenticatedUser);
            
            dao.changePassword(this.testPassword, this.testUserId);
            authenticatedUser = dao.authenticateUser(this.testUserName, this.testPassword);
            Assert.assertNotNull(authenticatedUser);
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void changePasswordInvalid() throws IllegalArgumentException, NdexException
    {
        dao.changePassword("",this.testUserId);
    }

    @Test
    public void createUser()
    {
        Assert.assertTrue(createNewUser());
    }

    @Test(expected = IllegalArgumentException.class)
    public void createUserInvalid() throws IllegalArgumentException, NdexException
    {
        dao.createUser(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createUserInvalidUsername() throws IllegalArgumentException, NdexException
    {
        final NewUser newUser = new NewUser();
        newUser.setEmailAddress("support@ndexbio.org");
        newUser.setPassword("probably-insecure");
        
        dao.createUser(newUser);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createUserInvalidPassword() throws IllegalArgumentException, NdexException
    {
        final NewUser newUser = new NewUser();
        newUser.setEmailAddress("support@ndexbio.org");
        newUser.setUsername("Support");
        
        dao.createUser(newUser);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createUserInvalidEmail() throws IllegalArgumentException, NdexException
    {
        final NewUser newUser = new NewUser();
        newUser.setPassword("probably-insecure");
        newUser.setUsername("Support");
        
        dao.createUser(newUser);
    }

    @Test
    public void deleteNetworkFromWorkSurface()
    {
        Assert.assertTrue(putNetworkOnWorkSurface());
        Assert.assertTrue(removeNetworkFromWorkSurface());
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteNetworkFromWorkSurfaceInvalid() throws IllegalArgumentException, NdexException
    {
        dao.deleteNetworkFromWorkSurface("", this.testUserId);
    }

    @Test
    public void deleteUser()
    {
        Assert.assertTrue(createNewUser());
        Assert.assertTrue(deleteTargetUser());
    }

    @Test
    public void emailNewPassword()
    {
        try
        {
            Assert.assertTrue(createNewUser());
            
            dao.emailNewPassword("Support");
            
            Assert.assertTrue(deleteTargetUser());
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void emailNewPasswordInvalid() throws IllegalArgumentException, NdexException
    {
        dao.emailNewPassword("");
    }

    @Test
    public void findUsers()
    {
        final SearchParameters searchParameters = new SearchParameters();
        searchParameters.setSearchString("biologist");
        searchParameters.setSkip(0);
        searchParameters.setTop(25);
        
        try
        {
            final Collection<User> usersFound = dao.findUsers(searchParameters, "contains");
            Assert.assertNotNull(usersFound);
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void findUsersInvalid() throws IllegalArgumentException, NdexException
    {
        dao.findUsers(null, null);
    }

    @Test
    public void getUserById()
    {
        try
        {
            final ORID testUserRid = getRid(this.testUserName);
            final User testUser = dao.getUser(IdConverter.toJid(testUserRid));
            Assert.assertNotNull(testUser);
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void getUserByUsername()
    {
        try
        {
            final User testUser = dao.getUser(this.testUserName);
            Assert.assertNotNull(testUser);
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void getUserInvalid() throws IllegalArgumentException, NdexException
    {
        dao.getUser("");
    }

    @Test
    public void updateUser()
    {
        try
        {
            Assert.assertTrue(createNewUser());
            
            
            User loggedInUser = getUser("Support");
            User user = dao.getUser(this.testUserId);

            user.setEmailAddress("updated-support@ndexbio.org");
            dao.updateUser(user, this.testUserId);
            Assert.assertEquals(dao.getUser(loggedInUser.getId()).getEmailAddress(), loggedInUser.getEmailAddress());

            Assert.assertTrue(deleteTargetUser());
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test(expected = SecurityException.class)
    public void updateUserInvalidUser() throws IllegalArgumentException, SecurityException, NdexException
    {
        Assert.assertTrue(createNewUser());
        
        dao.updateUser(dao.getUser("Support"),this.testUserId);
        
        Assert.assertTrue(deleteTargetUser());
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateUserInvalid() throws IllegalArgumentException, SecurityException, NdexException
    {
        dao.updateUser(null,this.testUserId);
    }
    
    
    
	
	 private boolean createNewUser()
	    {
	        try
	        {
	            final NewUser newUser = new NewUser();
	            newUser.setEmailAddress("support@ndexbio.org");
	            newUser.setPassword("probably-insecure");
	            newUser.setUsername("Support");
	            
	            final User createdUser = dao.createUser(newUser);
	            Assert.assertNotNull(createdUser);
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
	    
	    private boolean deleteTargetUser()
	    {
	        try
	        {	            
	            User user = getUser("Support");
	            dao.deleteUser(user.getId());
	            Assert.assertNull(dao.getUser(user.getId()));
	            return true;
	        }
	        catch (Exception e)
	        {
	            Assert.fail(e.getMessage());
	            e.printStackTrace();
	        }
	        
	        return false;
	    }
	    
	    private boolean putNetworkOnWorkSurface()
	    {
	        try
	        {
	            final ORID testNetworkRid = getRid(this.testNetworkName);
	            dao.addNetworkToWorkSurface(IdConverter.toJid(testNetworkRid),this.testUserId);
	            
	            final User testUser = dao.getUser(this.testUserName);
	            Assert.assertEquals(testUser.getWorkSurface().size(), 1);
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
	    
	    private boolean removeNetworkFromWorkSurface()
	    {
	        try
	        {
	            final ORID testNetworkRid = getRid(this.testNetworkName);
	            dao.deleteNetworkFromWorkSurface(IdConverter.toJid(testNetworkRid),this.testUserId);
	            
	            final User testUser = dao.getUser(this.testUserName);
	            Assert.assertEquals(testUser.getWorkSurface().size(), 0);
	            
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
