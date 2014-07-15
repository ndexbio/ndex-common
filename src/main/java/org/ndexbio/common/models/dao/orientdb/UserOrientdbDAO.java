/**
 * 
 */
package org.ndexbio.common.models.dao.orientdb;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.ws.rs.core.Response;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexAOrientDBConnectionPool;
import org.ndexbio.common.exceptions.DuplicateObjectException;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.helpers.Configuration;
import org.ndexbio.common.helpers.IdConverter;
import org.ndexbio.common.models.dao.CommonDAOValues;
import org.ndexbio.common.models.data.INetwork;
import org.ndexbio.common.models.data.IUser;
import org.ndexbio.common.models.object.UploadedFile;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.SearchParameters;
import org.ndexbio.model.object.User;
import org.ndexbio.common.util.Email;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.common.util.Security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.impls.orient.OrientElement;

import org.ndexbio.model.object.NewUser;

/**
 * @author fcriscuo
 *
 */
public class UserOrientdbDAO extends OrientdbDAO  {

	private static final Logger logger = LoggerFactory.getLogger(UserOrientdbDAO.class);
	private UserOrientdbDAO() { super(); }
	
	public static UserOrientdbDAO createInstance( ) { return new UserOrientdbDAO();}
	
	
	public Iterable<Network> addNetworkToWorkSurface(String networkId, String userId)
			throws IllegalArgumentException, ObjectNotFoundException,
			NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId),
				"A network id is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId),
				"A user id is required");

		networkId = networkId.replace("\"", "");
		
		//final ORID userRid = IdConverter.toRid(this.getLoggedInUser().getId());
		final ORID userRid = IdConverter.toRid(userId);
		final ORID networkRid = IdConverter.toRid(networkId);
		String username = "";
		try {
			setupDatabase();

			final IUser user = _orientDbGraph.getVertex(userRid, IUser.class);
			username = user.getUsername();
			final INetwork network = _orientDbGraph.getVertex(networkRid,
					INetwork.class);
			if (network == null)
				throw new ObjectNotFoundException("Network", networkId);

			final Iterable<INetwork> workSurface = user.getWorkSurface();
			if (workSurface != null) {
				for (INetwork checkNetwork : workSurface) {
					if (checkNetwork.asVertex().getId().equals(networkRid))
						throw new DuplicateObjectException("Network with RID: "
								+ networkRid
								+ " is already on the Work Surface.");
				}
			}

			user.addNetworkToWorkSurface(network);			

			final ArrayList<Network> updatedWorkSurface = Lists.newArrayList();
			final Iterable<INetwork> onWorkSurface = user.getWorkSurface();
			if (onWorkSurface != null) {
				for (INetwork workSurfaceNetwork : onWorkSurface)
					updatedWorkSurface.add(new Network());
			}

			return updatedWorkSurface;
		} catch (DuplicateObjectException | ObjectNotFoundException onfe) {
			throw onfe;
		} catch (Exception e) {
			logger.error(
					"Failed to add a network to "
							+ username
							+ "'s Work Surface.", e);
		
			throw new NdexException(
					"Failed to add the network to your Work Surface.");
		} finally {
			teardownDatabase();
		}
	}

	/* (non-Javadoc)
	 * @see org.ndexbio.common.models.dao.UserDAO#authenticateUser(java.lang.String, java.lang.String)
	 */
	
	public User authenticateUser(String username, String password)
			throws SecurityException, NdexException {
		if (Strings.isNullOrEmpty(username)|| Strings.isNullOrEmpty(password))
			throw new SecurityException("Invalid username or password.");

		try {
			final User authUser = Security.authenticateUser(username, password);
			if (authUser == null)
				throw new SecurityException("Invalid username or password.");

			return authUser;
		} catch (SecurityException se) {
			throw se;
		} catch (Exception e) {
			logger.error("Can't authenticate users.", e);
			throw new NdexException(
					"There's a problem with the authentication server. Please try again later.");
		}
	}

	/* (non-Javadoc)
	 * @see org.ndexbio.common.models.dao.UserDAO#changePassword(java.lang.String)
	 */
	
	public void changePassword(String password, String userId)
			throws IllegalArgumentException, NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId), 
				"A user id is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(password), 
				"A password is required");
		

		//final ORID userRid = IdConverter.toRid(this.getLoggedInUser().getId());
		final ORID userRid = IdConverter.toRid(userId);
		String username = "";
		try {
			// Remove quotes around the password
			if (password.startsWith("\""))
				password = password.substring(1);
			if (password.endsWith("\""))
				password = password.substring(0, password.length() - 1);

			setupDatabase();
			username = this.findIuserById(userId).getUsername();
			final IUser existingUser = _orientDbGraph.getVertex(userRid,
					IUser.class);
			
			existingUser.setPassword(Security.hashText(password.trim()));

			
		} catch (Exception e) {
			logger.error("Failed to change "
					+ username + "'s password.", e);
			
			throw new NdexException("Failed to change your password.");
		} finally {
			teardownDatabase();
		}

	}

	/* (non-Javadoc)
	 * @see org.ndexbio.common.models.dao.UserDAO#changeProfileImage(java.lang.String, org.ndexbio.common.models.object.UploadedFile)
	 */
	
	public void changeProfileImage(String imageType, UploadedFile uploadedImage, String userId)
			throws IllegalArgumentException, NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(imageType), "An image type is required");
		Preconditions.checkArgument(null != uploadedImage, "An uploaded image is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId), "A user id is required");
		
		

		//final ORID userId = IdConverter.toRid(this.getLoggedInUser().getId());
		final ORID userRId = IdConverter.toRid(userId);

		try {
			
			setupDatabase();
			final IUser user = this.findIuserById(userId);
			if (user == null)
				throw new ObjectNotFoundException("User", userRId);

			final BufferedImage newImage = ImageIO
					.read(new ByteArrayInputStream(uploadedImage.getFileData()));

			if (imageType.toLowerCase().equals("profile")) {
				final BufferedImage resizedImage = resizeImage(newImage,
						Integer.parseInt(Configuration.getInstance()
								.getProperty("Profile-Image-Width")),
						Integer.parseInt(Configuration.getInstance()
								.getProperty("Profile-Image-Height")));

				ImageIO.write(
						resizedImage,
						"jpg",
						new File(Configuration.getInstance().getProperty(
								"Profile-Image-Path")
								+ user.getUsername() + ".jpg"));
			} else {
				final BufferedImage resizedImage = resizeImage(newImage,
						Integer.parseInt(Configuration.getInstance()
								.getProperty("Profile-Background-Width")),
						Integer.parseInt(Configuration.getInstance()
								.getProperty("Profile-Background-Height")));

				ImageIO.write(resizedImage, "jpg", new File(Configuration
						.getInstance().getProperty("Profile-Background-Path")
						+ user.getUsername() + ".jpg"));
			}
		} catch (ObjectNotFoundException onfe) {
			throw onfe;
		} catch (Exception e) {
			logger.error("Failed to save a profile image.", e);
			throw new NdexException(e.getMessage());
		} finally {
			teardownDatabase();
		}

	}

	/**
	 * Create a user in the Given database using the information in newUser object.
	 * @param db
	 * @param newUser
	 * @return
	 * @throws NdexException 
	 */
	public boolean createUser(ODatabaseDocumentTx db, NewUser newUser) throws NdexException {

		Preconditions.checkArgument(null != newUser, 
				"A user object is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty( newUser.getUsername()),
				"A user name is required" );
		Preconditions.checkArgument(!Strings.isNullOrEmpty( newUser.getPassword()),
				"A user password is required" );
		Preconditions.checkArgument(!Strings.isNullOrEmpty( newUser.getEmailAddress()),
				"A user email address is required" );
		
		
		try {
			
			// confirm that the username and email address are unique
			// method throws an exception if either already exists
   		    this.checkForExistingUser(newUser);
   		    
   		    db.begin();
   		    ODocument account = new ODocument (NdexClasses.Account);
   		    account.field("accountName", newUser.getUsername());
   		    account.field("password", Security.hashText(newUser.getPassword()));
   		    
   		    ODocument user = new ODocument(NdexClasses.User);
   		    user.field("emailAddress", newUser.getEmailAddress());
   		    user.field("firstName", newUser.getFirstname());
   		    user.field("lastName", newUser.getLastname());
   		    user.field("hasAccount", account);
   		    
   		    account.save();
   		    user.save();
   		    
	/*		final IUser user = _orientDbGraph.addVertex("class:user",
					IUser.class);
			user.setUsername(newUser.getUsername());
			user.setPassword(Security.hashText(newUser.getPassword()));
			user.setEmailAddress(newUser.getEmailAddress());
   */
			
			db.commit();
			return true;
		} catch (Exception e) {
			logger.error(
					"Failed to create a new user: " + newUser.getUsername()
							+ ".", e);
			throw new NdexException(e.getMessage());
		} finally {
			teardownDatabase();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.ndexbio.common.models.dao.UserDAO#createUser(org.ndexbio.common.models.object.NewUser)
	 */
	
	public User createUser(NewUser newUser) throws IllegalArgumentException,
			DuplicateObjectException, NdexException {
		Preconditions.checkArgument(null != newUser, 
				"A user object is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty( newUser.getUsername()),
				"A user name is required" );
		Preconditions.checkArgument(!Strings.isNullOrEmpty( newUser.getPassword()),
				"A user password is required" );
		Preconditions.checkArgument(!Strings.isNullOrEmpty( newUser.getEmailAddress()),
				"A user email address is required" );
		
		
		try {

			setupDatabase();
			
   		    this.checkForExistingUser(newUser);
   		    
   		    this._ndexDatabase.begin();
   /*		    ODocument account = new ODocument (NdexClasses.Account);
   		    account.field("accountName", newUser.getUsername());
   		    account.field("password", Security.hashText(newUser.getPassword()));
   	*/	    
   		    User result = new User();
   		    
   		    result.setExternalId(NdexUUIDFactory.INSTANCE.getNDExUUID());
   		    result.setAccountName(newUser.getUsername());
   		    result.setEmailAddress(newUser.getEmailAddress());
   		    result.setFirstName(newUser.getFirstname());
   		    result.setLastName(newUser.getLastname());
   		    
   		    ODocument user = new ODocument(NdexClasses.User);
   		    user.field("emailAddress", newUser.getEmailAddress());
   		    user.field("firstName", newUser.getFirstname());
   		    user.field("lastName", newUser.getLastname());
   		    user.field("accountName", newUser.getUsername());
   		    user.field("password", Security.hashText(newUser.getPassword()));
   		    user.field("UUID", result.getExternalId());
   		    user.field("createdDate", result.getCreationDate());
   		    user.field("modificationDate", result.getModificationDate());
   		   // user.field("hasAccount", account);
   		    
   		  //  account.save();
   		    user.save();
   		    
			this._ndexDatabase.commit();
			return result;
		} catch (Exception e) {

			logger.error(
					"Failed to create a new user: " + newUser.getUsername()
							+ ".", e);
			
			throw new NdexException(e.getMessage());
		} finally {
			teardownDatabase();
		}
	}

	/* (non-Javadoc)
	 * @see org.ndexbio.common.models.dao.UserDAO#deleteNetworkFromWorkSurface(java.lang.String)
	 */
	
	public Iterable<Network> deleteNetworkFromWorkSurface(String networkId, String userId)
			throws IllegalArgumentException, ObjectNotFoundException,
			NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), 
				"A network id is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId), 
				"A user id is required");
	
		//final ORID userRid = IdConverter.toRid(this.getLoggedInUser().getId());
		final ORID userRid = IdConverter.toRid(userId);
		final ORID networkRid = IdConverter.toRid(networkId);
		String username = "";
		try {
			setupDatabase();
			username = this.findIuserById(userId).getUsername();
			final IUser user = _orientDbGraph.getVertex(userRid, IUser.class);

			final INetwork network = _orientDbGraph.getVertex(networkRid,
					INetwork.class);
			if (network == null)
				throw new ObjectNotFoundException("Network", networkId);

			user.removeNetworkFromWorkSurface(network);
			

			final ArrayList<Network> updatedWorkSurface = Lists.newArrayList();
			final Iterable<INetwork> onWorkSurface = user.getWorkSurface();
			if (onWorkSurface != null) {
				for (INetwork workSurfaceNetwork : onWorkSurface)
					updatedWorkSurface.add(new Network());
			}

			return updatedWorkSurface;
		} catch (ObjectNotFoundException onfe) {
			throw onfe;
		} catch (Exception e) {
			logger.error(
					"Failed to remove a network from "
							+ username
							+ "'s Work Surface.", e);
			
			throw new NdexException(
					"Failed to remove the network from your Work Surface.");
		} finally {
			teardownDatabase();
		}
	}

	/* (non-Javadoc)
	 * @see org.ndexbio.common.models.dao.UserDAO#deleteUser()
	 */
	public void deleteUser(String userId) throws NdexException {
		
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId), "A user id is required");
		
		//final ORID userRid = IdConverter.toRid(this.getLoggedInUser().getId());
		final ORID userRid = IdConverter.toRid(userId);
		String username = "";
		try {
			
			setupDatabase();
			username = this.findIuserById(userId).getUsername();

			final IUser userToDelete = _orientDbGraph.getVertex(userRid,
					IUser.class);

			final List<ODocument> adminGroups = _ndexDatabase
					.query(new OSQLSynchQuery<Integer>(
							"SELECT COUNT(@RID) FROM Membership WHERE in_groups = "
									+ userRid + " AND permissions = 'ADMIN'"));
			if (adminGroups == null || adminGroups.isEmpty())
				throw new NdexException(
						"Unable to query user/group membership.");
			else if ((long) adminGroups.get(0).field("COUNT") > 1)
				throw new NdexException(
						"Cannot delete a user that is an ADMIN member of any group.");

			final List<ODocument> adminNetworks = _ndexDatabase
					.query(new OSQLSynchQuery<Integer>(
							"SELECT COUNT(@RID) FROM Membership WHERE in_networks = "
									+ userRid + " AND permissions = 'ADMIN'"));
			if (adminNetworks == null || adminNetworks.isEmpty())
				throw new NdexException(
						"Unable to query user/network membership.");
			else if ((long) adminNetworks.get(0).field("COUNT") > 1)
				throw new NdexException(
						"Cannot delete a user that is an ADMIN member of any network.");

			final List<ODocument> userChildren = _ndexDatabase
					.query(new OSQLSynchQuery<Object>(
							"SELECT @RID FROM (TRAVERSE * FROM " + userRid
									+ " WHILE @class <> 'user')"));
			for (ODocument userChild : userChildren) {
				final ORID childId = userChild.field("rid", OType.LINK);

				final OrientElement element = _orientDbGraph.getBaseGraph()
						.getElement(childId);
				if (element != null)
					element.remove();
			}

			_orientDbGraph.removeVertex(userToDelete.asVertex());
			
		} catch (NdexException ne) {
			throw ne;
		} catch (Exception e) {
			logger.error("Failed to delete user: "
					+username + ".", e);
			
			throw new NdexException(e.getMessage());
		} finally {
			teardownDatabase();
		}

	}

	/* (non-Javadoc)
	 * @see org.ndexbio.common.models.dao.UserDAO#emailNewPassword(java.lang.String)
	 */
	
	public Response emailNewPassword(String username)
			throws IllegalArgumentException, NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(username), 
				"A username is required");
		
		try {
			setupDatabase();

			final Collection<ODocument> usersFound = _ndexDatabase.command(
					new OCommandSQL("select from User where username = ?"))
					.execute(username);

			if (usersFound.size() < 1)
				throw new ObjectNotFoundException("User", username);

			final IUser authUser = _orientDbGraph.getVertex(
					usersFound.toArray()[0], IUser.class);

			final String newPassword = Security.generatePassword();
			authUser.setPassword(Security.hashText(newPassword));

			final File forgotPasswordFile = new File(Configuration
					.getInstance().getProperty("Forgot-Password-File"));
			if (!forgotPasswordFile.exists())
				throw new java.io.FileNotFoundException(
						"File containing forgot password email content doesn't exist.");

			final BufferedReader fileReader = Files.newBufferedReader(
					forgotPasswordFile.toPath(), Charset.forName("US-ASCII"));
			final StringBuilder forgotPasswordText = new StringBuilder();

			String lineOfText = null;
			while ((lineOfText = fileReader.readLine()) != null)
				forgotPasswordText.append(lineOfText.replace("{password}",
						newPassword));

			Email.sendEmail(
					Configuration.getInstance().getProperty(
							"Forgot-Password-Email"),
					authUser.getEmailAddress(), "Password Recovery",
					forgotPasswordText.toString());

			
			return Response.ok().build();
		} catch (ObjectNotFoundException onfe) {
			throw onfe;
		} catch (Exception e) {
			logger.error("Failed to change " + username + "'s password.", e);
			
			throw new NdexException("Failed to recover your password.");
		} finally {
			teardownDatabase();
		}
	}

	/* (non-Javadoc)
	 * @see org.ndexbio.common.models.dao.UserDAO#findUsers(org.ndexbio.common.models.object.SearchParameters, java.lang.String)
	 */
	
	public List<User> findUsers(SearchParameters searchParameters,
			String searchOperator) throws IllegalArgumentException,
			NdexException {
		Preconditions.checkArgument(null != searchParameters, "Search parameters are required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(searchParameters.getSearchString()), 
				"A search string is required");
		
		searchParameters.setSearchString(searchParameters.getSearchString()
					.toLowerCase().trim());

		final List<User> foundUsers = new ArrayList<User>();
		String operator = searchOperator.toLowerCase();

		final int startIndex = searchParameters.getSkip()
				* searchParameters.getTop();

		String query = "";

		if (operator.equals("exact-match")) {

			query = "SELECT FROM User\n" + "WHERE username.toLowerCase() = '"
					+ searchParameters.getSearchString() + "'\n"
					+ "  OR lastName.toLowerCase() = '"
					+ searchParameters.getSearchString() + "'\n"
					+ "  OR firstName.toLowerCase() = '"
					+ searchParameters.getSearchString() + "'\n"
					+ "ORDER BY creation_date DESC\n" + "SKIP " + startIndex
					+ "\n" + "LIMIT " + searchParameters.getTop();
		} else if (operator.equals("starts-with")) {
			query = "SELECT FROM User\n"
					+ "WHERE username.toLowerCase() LIKE '"
					+ searchParameters.getSearchString() + "%'\n"
					+ "  OR lastName.toLowerCase() LIKE '"
					+ searchParameters.getSearchString() + "%'\n"
					+ "  OR firstName.toLowerCase() LIKE '"
					+ searchParameters.getSearchString() + "%'\n"
					+ "ORDER BY creation_date DESC\n" + "SKIP " + startIndex
					+ "\n" + "LIMIT " + searchParameters.getTop();
		} else if (operator.equals("contains")) {
			query = "SELECT FROM User\n"
					+ "WHERE username.toLowerCase() LIKE '%"
					+ searchParameters.getSearchString() + "%'\n"
					+ "  OR lastName.toLowerCase() LIKE '%"
					+ searchParameters.getSearchString() + "%'\n"
					+ "  OR firstName.toLowerCase() LIKE '%"
					+ searchParameters.getSearchString() + "%'\n"
					+ "ORDER BY creation_date DESC\n" + "SKIP " + startIndex
					+ "\n" + "LIMIT " + searchParameters.getTop();
		}
		try {
			setupDatabase();

			final List<ODocument> users = _ndexDatabase
					.query(new OSQLSynchQuery<ODocument>(query));
			for (final ODocument user : users)
				foundUsers.add(getUserFromDocument(user));

			return foundUsers;
		} catch (Exception e) {
			logger.error(
					"Failed to search for users: "
							+ searchParameters.getSearchString() + ".", e);
			
			throw new NdexException("Failed to search for users.");
		} finally {
			teardownDatabase();
		}
	}

	/* (non-Javadoc)
	 * @see org.ndexbio.common.models.dao.UserDAO#getUser(java.lang.String)
	 */
	
	@Deprecated
	public User getUser(String userId) throws IllegalArgumentException,
			NdexException {

		throw new NdexException("Unimplemented function UserOrientdbDAO.getUser(String)");
		/*		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId),
				"A user id is required");
		
		try {
			setupDatabase();

			final ORID userRid = IdConverter.toRid(userId);

			final IUser user = _orientDbGraph.getVertex(userRid, IUser.class);
			if (user != null)
				return new User(user, true);
		} catch (IllegalArgumentException ae) {
			// The user ID is actually a username
			final List<ODocument> matchingUsers = _ndexDatabase
					.query(new OSQLSynchQuery<Object>(
							"SELECT FROM User WHERE username = '" + userId
									+ "'"));
			if (!matchingUsers.isEmpty())
				return new User(_orientDbGraph.getVertex(matchingUsers.get(0),
						IUser.class), true);
		} catch (Exception e) {
			logger.error("Failed to get user: " + userId + ".", e);
			throw new NdexException("Failed to retrieve that user.");
		} finally {
			teardownDatabase();
		}

		return null; */
	}
	
	
	public User getUserByAccountName(String accountName) {
		try {
			setupDatabase();

			final List<ODocument> matchingUsers = _ndexDatabase
					.query(new OSQLSynchQuery<Object>(
							"SELECT FROM User WHERE username = '" + accountName
									+ "'"));
			if (!matchingUsers.isEmpty())
				return getUserFromDocument(matchingUsers.get(0));
		} catch (Exception e) {
			logger.error("User: " + accountName + " not found in NDEx.", e);
		} finally {
			teardownDatabase();
		}

		return null; 
		
	}
	

	/* (non-Javadoc)
	 * @see org.ndexbio.common.models.dao.UserDAO#updateUser(org.ndexbio.common.models.object.User)
	 */
	public void updateUser(User updatedUser, String userId) throws IllegalArgumentException,
			SecurityException, NdexException {
		throw new NdexException("Unimplemented function UserOrientdbDAO.updateUser(User, String)");
/*		Preconditions.checkArgument(null != updatedUser, 
				"Upadted user data are required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId), "A user id is required");
		
		if (!updatedUser.getId().equals(userId)){
			throw new SecurityException("You cannot update other users.");
		}

		
		//final ORID userRid = IdConverter.toRid(this.getLoggedInUser().getId());
		final ORID userRid = IdConverter.toRid(userId);
		String username = "";
		try {
			setupDatabase();
			username = this.findIuserById(userId).getUsername();
			final IUser userToUpdate = _orientDbGraph.getVertex(userRid,
					IUser.class);

			if (updatedUser.getBackgroundImage() != null
					&& !updatedUser.getBackgroundImage().equals(
							userToUpdate.getBackgroundImage()))
				userToUpdate.setBackgroundImage(updatedUser
						.getBackgroundImage());

			if (updatedUser.getDescription() != null
					&& !updatedUser.getDescription().equals(
							userToUpdate.getDescription()))
				userToUpdate.setDescription(updatedUser.getDescription());

			if (updatedUser.getEmailAddress() != null
					&& !updatedUser.getEmailAddress().equals(
							userToUpdate.getEmailAddress()))
				userToUpdate.setEmailAddress(updatedUser.getEmailAddress());

			if (updatedUser.getFirstName() != null
					&& !updatedUser.getFirstName().equals(
							userToUpdate.getFirstName()))
				userToUpdate.setFirstName(updatedUser.getFirstName());

			if (updatedUser.getForegroundImage() != null
					&& !updatedUser.getForegroundImage().equals(
							userToUpdate.getForegroundImage()))
				userToUpdate.setForegroundImage(updatedUser
						.getForegroundImage());

			if (updatedUser.getLastName() != null
					&& !updatedUser.getLastName().equals(
							userToUpdate.getLastName()))
				userToUpdate.setLastName(updatedUser.getLastName());

			if (updatedUser.getWebsite() != null
					&& !updatedUser.getWebsite().equals(
							userToUpdate.getWebsite()))
				userToUpdate.setWebsite(updatedUser.getWebsite());

			
		} catch (Exception e) {
			if (e.getMessage().indexOf("cluster: null") > -1)
				throw new ObjectNotFoundException("User", updatedUser.getId());

			logger.error("Failed to update user: "
					+username  + ".", e);
			
			throw new NdexException("Failed to update your profile.");
		} finally {
			teardownDatabase();
		}
*/
	}

	/**************************************************************************
	 * Resizes the source image to the specified dimensions.
	 * 
	 * @param sourceImage
	 *            The image to resize.
	 * @param width
	 *            The new image width.
	 * @param height
	 *            The new image height.
	 **************************************************************************/
	private BufferedImage resizeImage(final BufferedImage sourceImage,
			final int width, final int height) {
		final Image resizeImage = sourceImage.getScaledInstance(width, height,
				Image.SCALE_SMOOTH);

		final BufferedImage resizedImage = new BufferedImage(width, height,
				Image.SCALE_SMOOTH);
		resizedImage.getGraphics().drawImage(resizeImage, 0, 0, null);

		return resizedImage;
	}
	/*
	 * Both a User's username and emailAddress must be unique in the database.
	 * Throw a DuplicateObjectException if that is not the case
	 */
	private void checkForExistingUser(final NewUser newUser) 
			throws DuplicateObjectException {
		List<ODocument> existingUsers = _ndexDatabase
				.query(new OSQLSynchQuery<Object>(
						"SELECT @RID FROM " + NdexClasses.Account
						+ " WHERE accountName = '"
								+newUser.getUsername()
								+ "'"));
		if (!existingUsers.isEmpty())
			throw new DuplicateObjectException(
					CommonDAOValues.DUPLICATED_ACCOUNT_FLAG);

		existingUsers = _ndexDatabase
				.query(new OSQLSynchQuery<Object>(
						"SELECT @RID FROM " + NdexClasses.User
						+ " WHERE emailAddress = '"
								+newUser.getEmailAddress()
								+ "'"));
		if (!existingUsers.isEmpty())
			throw new DuplicateObjectException(
					CommonDAOValues.DUPLICATED_EMAIL_FLAG);

	}
	
	public static User getUserFromDocument(ODocument OUserDoc) {
    	User result = new User();
    	
    	result.setEmailAddress((String)OUserDoc.field("emailAddress"));
    	result.setFirstName((String)OUserDoc.field("firstName"));
    	result.setLastName((String)OUserDoc.field("lastName"));
    	String uuid = OUserDoc.field("UUID");
    	result.setExternalId(UUID.fromString(uuid));
    	result.setAccountName((String)OUserDoc.field("accountName"));
    	result.setCreationDate((Date)OUserDoc.field("createdDate"));
    	result.setModificationDate((Date)OUserDoc.field("modificationDate"));
        
    	return result;
	}
	

}
