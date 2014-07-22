package org.ndexbio.common.models.dao.orientdb;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.exceptions.DuplicateObjectException;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.helpers.Configuration;
import org.ndexbio.common.models.dao.CommonDAOValues;
import org.ndexbio.common.persistence.orientdb.NdexPersistenceService;
import org.ndexbio.model.object.SimpleUserQuery;
import org.ndexbio.common.util.Email;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.common.util.Security;
import org.ndexbio.model.object.User;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

public class UserDAO {

	private ODatabaseDocumentTx db;
	private static final Logger logger = Logger.getLogger(UserDAO.class.getName());
	
	/*
	 * User operations can be achieved with Orient Document API methods.
	 * The constructor will need to accept a OrientGraph object if we wish
	 * to use the Graph API.
	 */
	/**************************************************************************
	    * UserDAO
	    * 
	    * @param db
	    *            Database instance from the Connection pool, should be opened
	    **************************************************************************/
	public UserDAO (ODatabaseDocumentTx db) {
		this.db = db;
	}
	
	/**************************************************************************
	 * Authenticates a user trying to login.
	 * 
	 * @param username
	 *            The username.	
	 * @param password
	 *            The password.
	 * @throws SecurityException
	 *             Invalid username or password.
	 * @throws NdexException
	 *             Can't authenticate users against the database.
	 * @return The user, from NDEx Object Model.
	 **************************************************************************/
	public User authenticateUser(String accountName, String password) 
		throws SecurityException, NdexException {
		
			if (Strings.isNullOrEmpty(accountName) || Strings.isNullOrEmpty(password))
				throw new SecurityException("No accountName or password entered.");

			try {
				final ODocument OAuthUser = _getUserByAccountName(accountName);
				if(!Security.authenticateUser(password, OAuthUser)) {
					throw new SecurityException("Invalid accountName or password.");
				}
				return _getUserFromDocument(OAuthUser);
			} catch (SecurityException se) {
				logger.info("Authentication failed: " + se.getMessage());
				throw se;
			} catch (ObjectNotFoundException e) {
				throw new SecurityException(e.getMessage());
			} catch (Exception e) {
				throw new NdexException("There's a problem with the authentication server. Please try again later.");
			}
	}
	
	/**************************************************************************
	    * Create a new user
	    * 
	    * @param newUser
	    *            A NewUser object, from the NDEx Object Model
	    * @throws NdexException
	    *            Attempting to save an ODocument to the database
	    * @throws IllegalArgumentException
	    * 			 The newUser does not contain proper fields
	    * @throws DuplicateObjectException
	    * 			 The account name and/or email already exist
	    * @returns User object, from the NDEx Object Model
	    **************************************************************************/
	public User createNewUser(User newUser)
		throws NdexException, IllegalArgumentException, DuplicateObjectException {

		Preconditions.checkArgument(null != newUser, 
				"A user object is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty( newUser.getAccountName()),
				"A accountName is required" );
		Preconditions.checkArgument(!Strings.isNullOrEmpty( newUser.getPassword()),
				"A user password is required" );
		Preconditions.checkArgument(!Strings.isNullOrEmpty( newUser.getEmailAddress()),
				"A user email address is required" );
		
		_checkForExistingUser(newUser);
			
		try {
			User result = new User();
			    
			result.setExternalId(NdexUUIDFactory.INSTANCE.getNDExUUID());
			result.setAccountName(newUser.getAccountName());
			result.setEmailAddress(newUser.getEmailAddress());
		    result.setFirstName(newUser.getFirstName());
			result.setLastName(newUser.getLastName());
			result.setWebsite(newUser.getWebsite());
			result.setDescription(newUser.getDescription());
			result.setImage(newUser.getImage());
			
			ODocument user = new ODocument(NdexClasses.User);
			user.field("description", newUser.getDescription());
			user.field("websiteURL", newUser.getWebsite());
			user.field("imageURL", newUser.getImage());
			user.field("emailAddress", newUser.getEmailAddress());
			user.field("firstName", newUser.getFirstName());
		    user.field("lastName", newUser.getLastName());
		    user.field("accountName", newUser.getAccountName());
		    user.field("password", Security.hashText(newUser.getPassword()));
		    user.field("UUID", result.getExternalId());
		    user.field("creationDate", result.getCreationDate());
		    user.field("modificationDate", result.getModificationDate());
		   
			user.save();
			
			logger.info("A new user with accountName " + newUser.getAccountName() + " has been created");
			
			return result;
		} 
		catch(Exception e) {
			logger.severe("Could not save new user to the database");
			throw new NdexException(e.getMessage());
		}
	}
	
	/**************************************************************************
	    * Delete a user
	    * 
	    * @param id
	    *            UUID for User
	    * @throws NdexException
	    *            Attempting to access and delete an ODocument from the database
	    **************************************************************************/
	public void deleteUserById(UUID id) 
		throws NdexException, ObjectNotFoundException{
		
			ODocument user = _getUserById(id);
			try {
				user.delete();
			}
			catch (Exception e) {
				logger.severe("Could not delete user from the database");
				throw new NdexException(e.getMessage());
			}
		
	}
	
	/**************************************************************************
	    * Get a user
	    * 
	    * @param id
	    *            UUID for User
	    * @throws NdexException
	    *            Attempting to query the database
	    * @returns User object, from the NDEx Object Model
	    **************************************************************************/
	public User getUserById(UUID id) 
		throws NdexException, ObjectNotFoundException {
		
		final ODocument user = _getUserById(id);
	    return _getUserFromDocument(user);
	     
	}
	
	/**************************************************************************
	    * Get a user
	    * 
	    * @param accountName
	    *            accountName for User
	    * @throws NdexException
	    *            Attempting to query the database
	    * @returns User object, from the NDEx Object Model
	    **************************************************************************/
	public User getUserByAccountName(String accountName) 
		throws NdexException, ObjectNotFoundException {

		final ODocument user = _getUserByAccountName(accountName);
	    return _getUserFromDocument(user);

	}
	
	/**************************************************************************
	    * Find users
	    * 
	    * @param id
	    *            UUID for User
	    * @throws NdexException
	    *            Attempting to query the database
	    * @returns User object, from the NDEx Object Model
	    **************************************************************************/
	public List<User> findUsers(SimpleUserQuery simpleQuery, int skip, int top) 
			throws IllegalArgumentException,
			NdexException {
		Preconditions.checkArgument(null != simpleQuery, "Search parameters are required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(simpleQuery.getSearchString()), 
				"A search string is required");
		
		simpleQuery.setSearchString(simpleQuery.getSearchString()
					.toLowerCase().trim());

		final List<User> foundUsers = new ArrayList<User>();

		final int startIndex = skip
				* top;

		String query = "SELECT FROM " + NdexClasses.User + " "
					+ "WHERE accountName.toLowerCase() LIKE '%"
					+ simpleQuery.getSearchString() + "%'"
					+ "  OR lastName.toLowerCase() LIKE '%"
					+ simpleQuery.getSearchString() + "%'"
					+ "  OR firstName.toLowerCase() LIKE '%"
					+ simpleQuery.getSearchString() + "%'"
					+ "  ORDER BY creation_date DESC " + " SKIP " + startIndex
					+ " LIMIT " + top;
		
		try {
			
			final List<ODocument> users = this.db.query(new OSQLSynchQuery<ODocument>(query));
			for (final ODocument user : users) {
				foundUsers.add(_getUserFromDocument(user));
				
			}
			return foundUsers;
			
		} catch (Exception e) {
			logger.severe("Unable to query the database");
			throw new NdexException("Failed to search for users.\n" + e.getMessage());
			
		} 
		
	}
	
	/**************************************************************************
	    * Email a new password
	    * 
	    * @param accountName
	    *            accountName for the User
	    * @throws NdexException
	    *            Attempting to query the database
	    * @throws IllegalArgumentException
	    * 			 accountName is required
	    * @throws ObjectNotFoundException
	    * 			 user with account name does not exist
	    * @returns response
	    **************************************************************************/
	public Response emailNewPassword(String accountName)
			throws IllegalArgumentException, ObjectNotFoundException, NdexException {
		
		Preconditions.checkArgument(!Strings.isNullOrEmpty(accountName), 
				"An accountName is required");
		
		try {

			ODocument userToSave = _getUserByAccountName(accountName);

			final User authUser = _getUserFromDocument(userToSave);
			final String newPassword = Security.generatePassword();
			authUser.setPassword(Security.hashText(newPassword));
			userToSave.field("password", authUser.getPassword());

			final File forgotPasswordFile = new File(Configuration
					.getInstance().getProperty("Forgot-Password-File"));
			
			if (!forgotPasswordFile.exists()) {
				logger.severe("Could not retrieve forgot password file");
				throw new java.io.FileNotFoundException(
						"File containing forgot password email content doesn't exist.");
			}

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

			logger.info("Sent password recovery email to user " + accountName);
			return Response.ok().build();
			
		} catch (ObjectNotFoundException onfe) {
			
			throw onfe;
			
		} catch (Exception e) {
			
			throw new NdexException("Failed to recover your password: \n" + e.getMessage());
			
		}
	}
	
	/**************************************************************************
	    * Change a user's password
	    * 
	    * @param id
	    *            UUID for user
	    * @throws NdexException
	    *            Attempting to access the database
	    * @throws IllegalArgumentException
	    * 			 new password and user id are required
	    * @throws ObjectNotFoundException
	    * 			 user does not exist
	    * @returns response
	    **************************************************************************/
	public void changePassword(String password, UUID id)
			throws IllegalArgumentException, NdexException, ObjectNotFoundException {
		
		Preconditions.checkNotNull(id, 
				"A user id is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(password), 
				"A password is required");
		
		ODocument user =  _getUserById(id);
		
		try {
			// Remove quotes around the password
			if (password.startsWith("\""))
				password = password.substring(1);
			if (password.endsWith("\""))
				password = password.substring(0, password.length() - 1);
			
			user.field("password", Security.hashText(password.trim()));

			user.save();
			
			logger.info("Changed password for user with UUID " + id);
			
		} catch (Exception e) {
			
			logger.severe("An error occured while saving password for user with UUID " + id);
			throw new NdexException("Failed to change your password.\n" + e.getMessage());
			
		} 

	}
	
	/**************************************************************************
	    * Update a user
	    * 
	    * @param updatedUser
	    * 			 User with new information
	    * @param id
	    *            UUID for user
	    * @throws NdexException
	    *            Attempting to access the database
	    * @throws IllegalArgumentException
	    * 			 new password and user id are required
	    * @throws ObjectNotFoundException
	    * 			 user does not exist
	    * @return User object
	    **************************************************************************/
	public User updateUser(User updatedUser, UUID id)
			throws IllegalArgumentException, NdexException, ObjectNotFoundException {
		
			Preconditions.checkNotNull(id, 
					"A user id is required");
			Preconditions.checkNotNull(updatedUser, 
					"An updated user is required");
			Preconditions.checkNotNull(updatedUser.getEmailAddress(), 
					"An email is required");
			Preconditions.checkNotNull(updatedUser.getFirstName(), 
					"An first name is required");
			Preconditions.checkNotNull(updatedUser.getLastName(), 
					"An last name is required");
		
		ODocument user =  _getUserById(id);
		
		try {

			user.field("description", updatedUser.getDescription());
			user.field("websiteURL", updatedUser.getWebsite());
			user.field("imageURL", updatedUser.getImage());
			user.field("emailAddress", updatedUser.getEmailAddress());
			user.field("firstName", updatedUser.getFirstName());
			user.field("lastName", updatedUser.getLastName());

			user.save();
			logger.info("Updted user profile with UUID " + id);
			
			return _getUserFromDocument(user);
			
		} catch (Exception e) {
			
			logger.severe("An error occured while updating user profile with UUID " + id);
			throw new NdexException("Failed to change your password.\n" + e.getMessage());
			
		} 

	}
	
	private ODocument _getUserById(UUID id) 
			throws NdexException, ObjectNotFoundException {
		
		final List<ODocument> users;
		
		String query = "select from " + NdexClasses.User 
		 		+ " where UUID = ?";
 
		try {
			
		     users = db.command( new OCommandSQL(query))
					   .execute(id.toString());
		     
		} catch (Exception e) {
			
			logger.severe("An error occured while attempting to query the database");
			throw new NdexException(e.getMessage());
			 
		}
		
		if (users.isEmpty()) {
			
			logger.info("User with UUID " + id + " does not exist");
			throw new ObjectNotFoundException("User", id.toString());
			 
		}
		
		return users.get(0);
		
	}
	private ODocument _getUserByAccountName(String accountName) 
			throws NdexException, ObjectNotFoundException {

		final List<ODocument> users;

		String query = "select from " + NdexClasses.User 
		 		+ " where accountName = ?";
 
		try {

		     users = db.command( new OCommandSQL(query))
					   .execute(accountName);

		} catch (Exception e) {
			
			logger.severe("An error occured while attempting to query the database");
			throw new NdexException(e.getMessage());

		}

		if (users.isEmpty()) {

			logger.info("User with accountName " + accountName + " does not exist");
			throw new ObjectNotFoundException("User", accountName);

		}

		return users.get(0);
	}
	
	/*
	 * Convert the database results into our object model
	 * TODO should this be moved to util? being used by other classes, not really a data object but a helper class
	 */
	public static User _getUserFromDocument(ODocument n) {
		
		User result = new User();
		
		result.setExternalId(UUID.fromString((String)n.field("UUID")));
		result.setAccountName((String)n.field("accountName"));
		result.setEmailAddress((String)n.field("emailAddress"));
		result.setFirstName((String)n.field("firstName"));
		result.setLastName((String)n.field("lastName"));
		result.setWebsite((String)n.field("websiteURL"));
		result.setDescription((String)n.field("description"));
		result.setImage((String)n.field("imageURL"));
		
		return result;
	}
	
	/*
	 * Both a User's username and emailAddress must be unique in the database.
	 * Throw a DuplicateObjectException if that is not the case
	 */
	private void _checkForExistingUser(final User newUser) 
			throws DuplicateObjectException {
		
		List<ODocument> existingUsers = db.query(
				new OSQLSynchQuery<Object>(
						"SELECT FROM " + NdexClasses.User
						+ " WHERE accountName = '" + newUser.getAccountName() + "'"));
		
		if (!existingUsers.isEmpty()) {
			logger.info("User with accountName " + newUser.getAccountName() + " already exists");
			throw new DuplicateObjectException(
					CommonDAOValues.DUPLICATED_ACCOUNT_FLAG);
		}

		existingUsers =db.query(
				new OSQLSynchQuery<Object>(
						"SELECT FROM " + NdexClasses.User
						+ " WHERE emailAddress = '" + newUser.getEmailAddress() + "'"));
		
		if (!existingUsers.isEmpty()){
			logger.info("User with accountName " + newUser.getAccountName() + " already exists");
			throw new DuplicateObjectException(
					CommonDAOValues.DUPLICATED_EMAIL_FLAG);
		}
	}
	
}
