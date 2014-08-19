package org.ndexbio.common.models.dao.orientdb;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.Date;

import javax.ws.rs.core.Response;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.exceptions.DuplicateObjectException;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.helpers.Configuration;
import org.ndexbio.common.models.dao.CommonDAOValues;
import org.ndexbio.common.util.Email;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.common.util.Security;
import org.ndexbio.model.object.Membership;
import org.ndexbio.model.object.MembershipType;
import org.ndexbio.model.object.Permissions;
import org.ndexbio.model.object.Request;
import org.ndexbio.model.object.ResponseType;
import org.ndexbio.model.object.User;
import org.ndexbio.model.object.NewUser;
import org.ndexbio.model.object.SimpleUserQuery;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class UserDAO extends OrientdbDAO{

	private ODatabaseDocumentTx db;
	private OrientBaseGraph graph;
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
	    * @param graph
	    * 			OrientGraph instance for Graph API operations
	    **************************************************************************/
	public UserDAO (ODatabaseDocumentTx db, OrientBaseGraph graph) {
		super(db);
		this.db = db;
		this.graph = graph;
	}
	
	/**************************************************************************
	 * Authenticates a user trying to login.
	 * 
	 * @param accountName
	 *            The accountName.	
	 * @param password
	 *            The password.
	 * @throws SecurityException
	 *             Invalid accountName or password.
	 * @throws NdexException
	 *             Can't authenticate users against the database.
	 * @return The user, from NDEx Object Model.
	 **************************************************************************/
	public User authenticateUser(String accountName, String password) 
		throws SecurityException, NdexException {
		
			if (Strings.isNullOrEmpty(accountName) || Strings.isNullOrEmpty(password))
				throw new SecurityException("No accountName or password entered.");

			try {
				final ODocument OAuthUser = this.getRecordByAccountName(accountName, NdexClasses.User);
				if(!Security.authenticateUser(password, OAuthUser)) {
					throw new SecurityException("Invalid accountName or password.");
				}
				return UserDAO.getUserFromDocument(OAuthUser);
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
	    *            A User object, from the NDEx Object Model
	    * @throws NdexException
	    *            Attempting to save an ODocument to the database
	    * @throws IllegalArgumentException
	    * 			 The newUser does not contain proper fields
	    * @throws DuplicateObjectException
	    * 			 The account name and/or email already exist
	    * @returns User object, from the NDEx Object Model
	    **************************************************************************/
	public User createNewUser(NewUser newUser)
		throws NdexException, IllegalArgumentException, DuplicateObjectException {

		Preconditions.checkArgument(null != newUser, 
				"A user object is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty( newUser.getAccountName()),
				"A accountName is required" );
		Preconditions.checkArgument(!Strings.isNullOrEmpty( newUser.getPassword()),
				"A user password is required" );
		Preconditions.checkArgument(!Strings.isNullOrEmpty( newUser.getEmailAddress()),
				"A user email address is required" );
		
		this.checkForExistingUser(newUser);
			
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
		Preconditions.checkArgument(null != id, 
				"UUID required");
		
		ODocument user = this.getRecordById(id, NdexClasses.User);
			
		/*if( !this.getUserGroupMemberships(id, Permissions.ADMIN, 0, 5).isEmpty()
				|| !this.getUserNetworkMemberships(id, Permissions.ADMIN, 0, 5).isEmpty() ) {
			throw new NdexException("Cannot orphan networks or groups");
		}*/
		
		try {
			OrientVertex vUser = graph.getVertex(user);
			boolean safe = true;
			
			// TODO, simplfy by using actual edge directions and labels
			for(Edge e : vUser.getEdges( Direction.BOTH) ) {/*, 
					Permissions.ADMIN.toString().toLowerCase() 
					+ " " 
					+ Permissions.GROUPADMIN.toString().toLowerCase() ) ) {*/
				
				OrientVertex vResource = (OrientVertex) e.getVertex(Direction.IN);
				
				if( !( vResource.getRecord().getSchemaClass().getName().equals( NdexClasses.Group ) 
						|| vResource.getRecord().getSchemaClass().getName().equals( NdexClasses.Network ) ) )
					continue;
				safe = false;	
				
				for(Edge ee : vResource.getEdges( Direction.BOTH/*, Permissions.ADMIN.toString().toLowerCase()*/ ) ) {
					if( !( (OrientVertex) ee.getVertex(Direction.OUT) ).equals(vUser) ) {
						safe = true;
					}
				}
					
			}
				
			if(!safe)
				throw new NdexException("Cannot orphan groups or networks");
			
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
		throws NdexException, IllegalArgumentException, ObjectNotFoundException {
		
		Preconditions.checkArgument(null != id, 
				"UUID required");
		
		final ODocument user = this.getRecordById(id, NdexClasses.User);
	    return UserDAO.getUserFromDocument(user);
	     
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
		throws NdexException, IllegalArgumentException, ObjectNotFoundException {
		
		Preconditions.checkArgument(!Strings.isNullOrEmpty(accountName), 
				"accountName required");

		final ODocument user = this.getRecordByAccountName(accountName, NdexClasses.User);
	    return UserDAO.getUserFromDocument(user);

	}
	
	/**************************************************************************
	    * Find users
	    * 
	    * @param id
	    *            UUID for User
	    * @param skip
	    * 			amount of blocks to skip
	    * @param top
	    * 			block size
	    * @throws NdexException
	    *            Attempting to query the database
	    * @returns User object, from the NDEx Object Model
	    **************************************************************************/
	public List<User> findUsers(SimpleUserQuery simpleQuery, int skip, int top) 
			throws IllegalArgumentException,
			NdexException {
		Preconditions.checkArgument(simpleQuery != null , "Search parameters are required");
		
		OSQLSynchQuery<ODocument> query;
		Iterable<ODocument> users;
		final List<User> foundUsers = new ArrayList<User>();
		
		simpleQuery.setSearchString(simpleQuery.getSearchString()
					.toLowerCase().trim());
		final int startIndex = skip * top;
		
		try {
			
			if(!Strings.isNullOrEmpty(simpleQuery.getAccountName())) {
				OIndex<?> accountNameIdx = this.db.getMetadata().getIndexManager().getIndex("index-user-username");
				OIdentifiable nGroup = (OIdentifiable) accountNameIdx.get(simpleQuery.getAccountName()); // account to traverse by
				
				if(nGroup == null) 
					throw new NdexException("Invalid accountName to filter by");
				
				String traverseRID = nGroup.getIdentity().toString();
				query = new OSQLSynchQuery<ODocument>("SELECT FROM"
						+ " (TRAVERSE in_groupadmin, in_member FROM"
			  				+ " " + traverseRID
			  				+ " WHILE $depth <=1)"
			  			+ " WHERE @class = '"+ NdexClasses.User +"'"
			  			+ " AND accountName.toLowerCase() LIKE '%"
			  			+ simpleQuery.getSearchString() + "%'"
						+ "  OR lastName.toLowerCase() LIKE '%"
						+ simpleQuery.getSearchString() + "%'"
						+ "  OR firstName.toLowerCase() LIKE '%"
						+ simpleQuery.getSearchString() + "%'"
						+ " ORDER BY creation_date DESC " 
						+ " SKIP " + startIndex
						+ " LIMIT " + top );
				
				users = this.db.command(query).execute();
				
				if( !users.iterator().hasNext() ) {
					
					query = new OSQLSynchQuery<ODocument>("SELECT FROM"
							+ " (TRAVERSE in_groupadmin, in_member FROM"
				  				+ " " + traverseRID
				  				+ " WHILE $depth <=1)"
				  			+ " WHERE @class = '"+ NdexClasses.User +"'"
							+ " ORDER BY creation_date DESC " 
							+ " SKIP " + startIndex
							+ " LIMIT " + top );
					
					users = this.db.command(query).execute();
					
				}
				
				for (final ODocument user : users) {
					foundUsers.add(UserDAO.getUserFromDocument(user));
				}
				return foundUsers;
				
			
			} else {
				
				query = new OSQLSynchQuery<ODocument>("SELECT FROM "
						+ NdexClasses.User + " "
						+ "WHERE accountName.toLowerCase() LIKE '%"
						+ simpleQuery.getSearchString() + "%'"
						+ "  OR lastName.toLowerCase() LIKE '%"
						+ simpleQuery.getSearchString() + "%'"
						+ "  OR firstName.toLowerCase() LIKE '%"
						+ simpleQuery.getSearchString() + "%'"
						+ "  ORDER BY creation_date DESC " + " SKIP " + startIndex
						+ " LIMIT " + top);
				
				users = this.db.command(query).execute();
				
				if( !users.iterator().hasNext() ) 
					users = db.browseClass(NdexClasses.User).setLimit(top);
				
				for (final ODocument user : users) {
					foundUsers.add(UserDAO.getUserFromDocument(user));
				}
				return foundUsers;
				
			}
			
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

			ODocument userToSave = this.getRecordByAccountName(accountName, NdexClasses.User);

			final User authUser = UserDAO.getUserFromDocument(userToSave);
			final String newPassword = Security.generatePassword();
			final String password = Security.hashText(newPassword);
			userToSave.field("password", password);

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
	    * @param password
	    * 			 new password for user
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
		
		ODocument user =  this.getRecordById(id, NdexClasses.User);
		
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
	    * Change a user's email Address
	    * 
	    * @param id
	    *            UUID for user
	    * @param emailAddress 
	    * 		     new email address
	    * @throws NdexException
	    *            Attempting to access the database
	    * @throws IllegalArgumentException
	    * 			 new password and user id are required
	    * @throws ObjectNotFoundException
	    * 			 user does not exist
	    * @returns response
	    **************************************************************************/
	public Response changeEmailAddress(String emailAddress, UUID id)
			throws IllegalArgumentException, NdexException, ObjectNotFoundException, DuplicateObjectException {
		
		Preconditions.checkNotNull(id, 
				"A user id is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(emailAddress), 
				"A password is required");
		
		ODocument user =  this.getRecordById(id, NdexClasses.User);
		
		try {
			
			// check for unique emailAddress
			String query = "select emailAddress from " + NdexClasses.User + " where emailAddress = ?";
			List<ODocument> existingUser = db.command( new OCommandSQL(query))
			   .execute(emailAddress);
			
			if(!existingUser.isEmpty()) {
				logger.severe("Email address already exists in the database.");
				throw new NdexException("email address is taken");
			}
			
			final String oldEmail = (String) user.field("emailAddress");
			user.field("emailAddress", emailAddress);
			user.save();
			
			//send emails to new and old address
			final File ChangeEmailFile = new File(Configuration
					.getInstance().getProperty("Change-Email-File"));
			
			if (!ChangeEmailFile.exists()) {
				logger.severe("Could not retrieve change email file");
				throw new java.io.FileNotFoundException(
						"File containing change email content doesn't exist.");
			}

			final BufferedReader fileReader = Files.newBufferedReader(
					ChangeEmailFile.toPath(), Charset.forName("US-ASCII"));
			
			final StringBuilder changeEmailText = new StringBuilder();

			String lineOfText = null;
			while ((lineOfText = fileReader.readLine()) != null)
				changeEmailText.append(lineOfText.replace("{oldEmail}",
						oldEmail).replace("{newEmail}", emailAddress));

			Email.sendEmail(
					Configuration.getInstance().getProperty(
							"Change-Email-Email"),
					oldEmail, "Email Change",
					changeEmailText.toString());
			Email.sendEmail(
					Configuration.getInstance().getProperty(
							"Change-Email-Email"),
					emailAddress, "Email Change",
					changeEmailText.toString());

			logger.info("Changed email address for user with UUID " + id);
			
			return Response.ok().build();
			
		} catch (Exception e) {
			
			logger.severe("An error occured while changing email for user with UUID " + id);
			throw new NdexException("Failed to change your email.\n" + e.getMessage());
			
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
		
			Preconditions.checkArgument(id != null, 
					"A user id is required");
			Preconditions.checkArgument(updatedUser != null, 
					"An updated user is required");
		
		ODocument user =  this.getRecordById(id, NdexClasses.User);
		
		try {
			//updatedUser.getDescription().isEmpty();
			if(!Strings.isNullOrEmpty(updatedUser.getDescription())) user.field("description", updatedUser.getDescription());
			if(!Strings.isNullOrEmpty(updatedUser.getWebsite())) user.field("websiteURL", updatedUser.getWebsite());
			if(!Strings.isNullOrEmpty(updatedUser.getImage())) user.field("imageURL", updatedUser.getImage());
			if(!Strings.isNullOrEmpty(updatedUser.getFirstName())) user.field("firstName", updatedUser.getFirstName());
			if(!Strings.isNullOrEmpty(updatedUser.getLastName())) user.field("lastName", updatedUser.getLastName());
			user.field("modificationDate", updatedUser.getModificationDate());

			user.save();
			logger.info("Updated user profile with UUID " + id);
			
			return UserDAO.getUserFromDocument(user);
			
		} catch (Exception e) {
			
			logger.severe("An error occured while updating user profile with UUID " + id);
			throw new NdexException(e.getMessage());
			
		} 

	}
	
	/**************************************************************************
	    * getUserNetworkMemberships
	    *
	    * @param userId
	    *            UUID for associated user
	    * @param permission
	    * 			Type of memberships to retrieve, ADMIN, WRITE, or READ
	    * @param skipBlocks
	    * 			amount of blocks to skip
	    * @param blockSize
	    * 			The size of blocks to be skipped and retrieved
	    * @throws NdexException
	    *            Invalid parameters or an error occurred while accessing the database
	    * @throws ObjectNotFoundException
	    * 			Invalid userId
	    **************************************************************************/
	
	public List<Membership> getUserNetworkMemberships(UUID userId, Permissions permission, int skipBlocks, int blockSize) 
			throws ObjectNotFoundException, NdexException {
		
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId.toString()),
				"A user UUID is required");
		Preconditions.checkArgument( (permission == Permissions.ADMIN)
				|| (permission == Permissions.READ)
				|| (permission == Permissions.WRITE),
				"Valid permissions required");
		
		ODocument user = this.getRecordById(userId, NdexClasses.User);
		
		final int startIndex = skipBlocks * blockSize;
		
		try {
			List<Membership> memberships = new ArrayList<Membership>();
			
			String userRID = user.getIdentity().toString();
			
			//TODO change to include get networks within a group
			OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>(
		  			"SELECT FROM"
		  			+ " (TRAVERSE "+ NdexClasses.User +".out_"+ permission.name().toString().toLowerCase() +" FROM"
		  				+ " " + userRID
		  				+ "  WHILE $depth <=1)"
		  			+ " WHERE @class = '" + NdexClasses.Network + "'"
		 			+ " ORDER BY creation_date DESC " + " SKIP " + startIndex
		 			+ " LIMIT " + blockSize);
			
			List<ODocument> records = this.db.command(query).execute(); 
			for(ODocument network: records) {
				
				Membership membership = new Membership();
				membership.setMembershipType( MembershipType.NETWORK );
				membership.setMemberAccountName( (String) user.field("accountName") ); 
				membership.setMemberUUID( userId );
				membership.setPermissions( permission );
				membership.setResourceName( (String) network.field("name") );
				membership.setResourceUUID( UUID.fromString( (String) network.field("UUID") ) );
				
				memberships.add(membership);
			}
			
			
			logger.info("Successfuly retrieved user-network memberships");
			return memberships;
			
		} catch(Exception e) {
			logger.severe("An unexpected error occured while retrieving user-network memberships");
			throw new NdexException(e.getMessage());
		}
	}
	
	
	/**************************************************************************
	    * getUsergroupMemberships
	    *
	    * @param userId
	    *            UUID for associated user
	    * @param permission
	    * 			Type of memberships to retrieve, ADMIN, WRITE, or READ
	    * @param skipBlocks
	    * 			amount of blocks to skip
	    * @param blockSize
	    * 			The size of blocks to be skipped and retrieved
	    * @throws NdexException
	    *            Invalid parameters or an error occurred while accessing the database
	    * @throws ObjectNotFoundException
	    * 			Invalid userId
	    **************************************************************************/
	
	public List<Membership> getUserGroupMemberships(UUID userId, Permissions permission, int skipBlocks, int blockSize) 
			throws ObjectNotFoundException, NdexException {
		
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId.toString()),
				"A user UUID is required");
		Preconditions.checkArgument( (permission.equals( Permissions.GROUPADMIN) )
				|| (permission.equals( Permissions.MEMBER ) ),
				"Valid permissions required");
		
		ODocument user = this.getRecordById(userId, NdexClasses.User);
		
		final int startIndex = skipBlocks
				* blockSize;
		
		try {
			List<Membership> memberships = new ArrayList<Membership>();
			
			String userRID = user.getIdentity().toString();
			OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>(
		  			"SELECT FROM"
		  			+ " (TRAVERSE "+ NdexClasses.User +".out_"+ permission.name().toString().toLowerCase() +" FROM"
		  				+ " " + userRID
		  				+ "  WHILE $depth <=1)"
		  			+ " WHERE @class = '" + NdexClasses.Group + "'"
		 			+ " ORDER BY creation_date DESC " + " SKIP " + startIndex
		 			+ " LIMIT " + blockSize);
			
			List<ODocument> records = this.db.command(query).execute(); 
			for(ODocument group: records) {
				
				Membership membership = new Membership();
				membership.setMembershipType( MembershipType.GROUP );
				membership.setMemberAccountName( (String) user.field("accountName") ); 
				membership.setMemberUUID( userId );
				membership.setPermissions( permission );
				membership.setResourceName( (String) group.field("organizationName") );
				membership.setResourceUUID( UUID.fromString( (String) group.field("UUID") ) );
				
				memberships.add(membership);
			}
			
			
			logger.info("Successfuly retrieved user-group memberships");
			return memberships;
			
		} catch(Exception e) {
			logger.severe("An unexpected error occured while retrieving user-group memberships");
			throw new NdexException(e.getMessage());
		}
	}
	
	/**************************************************************************
	    * getSentRequest
	    *
	    * @param account
	    *           User object
	    * @param skipBlocks
	    * 			amount of blocks to skip
	    * @param blockSize
	    * 			The size of blocks to be skipped and retrieved
	    * @throws NdexException
	    *            An error occurred while accessing the database
	    * @throws ObjectNotFoundException
	    * 			Invalid userId
	    **************************************************************************/
	
	public List<Request> getSentRequest(User account, int skipBlocks, int blockSize) 
			throws ObjectNotFoundException, NdexException {
		Preconditions.checkArgument(account != null,
				"Must be logged in");
		//TODO May possibly add extra parameter to specify type of request to return
		
		final List<Request> requests = new ArrayList<Request>();
		
		ODocument user = this.getRecordById(account.getExternalId(), NdexClasses.User);
		final int startIndex = skipBlocks * blockSize;
		
		try {
			
			OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>(
		  			"SELECT FROM"
		  			+ " (TRAVERSE out_requests FROM"
		  				+ " " + user.getIdentity().toString()
		  				+ "  WHILE $depth <=1)"
		  			+ " WHERE @class = '" + NdexClasses.Request + "'"
		 			+ " ORDER BY creation_date DESC " + " SKIP " + startIndex
		 			+ " LIMIT " + blockSize);
		
			List<ODocument> records = this.db.command(query).execute(); 
			
			for(ODocument request: records) {
				requests.add( RequestDAO.getRequestFromDocument( request ) );
			}
			
			return requests;
		} catch(Exception e){
			logger.severe("Unable to retrieve requests : " + e.getMessage());
			throw new NdexException("Unable to retrieve sent requests");
		}
	}
	
	/**************************************************************************
	    * getPendingRequest
	    *
	    * @param account
	    *           User object
	    * @param skipBlocks
	    * 			amount of blocks to skip
	    * @param blockSize
	    * 			The size of blocks to be skipped and retrieved
	    * @throws NdexException
	    *            An error occurred while accessing the database
	    * @throws ObjectNotFoundException
	    * 			Invalid userId
	    **************************************************************************/
	public List<Request> getPendingRequest(User account, int skipBlocks, int blockSize) 
			throws ObjectNotFoundException, NdexException {
		Preconditions.checkArgument(account != null,
				"Must be logged in");
		//TODO May possibly add extra parameter to specify type of request to return
		
		final List<Request> requests = new ArrayList<Request>();
		
		ODocument user = this.getRecordById(account.getExternalId(), NdexClasses.User);
		final int startIndex = skipBlocks * blockSize;
		
		try {
			OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>(
		  			"SELECT FROM"
		  			+ " (TRAVERSE in_requests FROM"
		  				+ " " + user.getIdentity().toString()
		  				+ "  WHILE $depth <=1)"
		  			+ " WHERE @class = '" + NdexClasses.Request + "'"
		  			+ " AND response = '" + ResponseType.PENDING +"'"
		 			+ " ORDER BY creation_date DESC " + " SKIP " + startIndex
		 			+ " LIMIT " + blockSize);
		
			List<ODocument> records = this.db.command(query).execute(); 
			
			for(ODocument request: records) {
				requests.add( RequestDAO.getRequestFromDocument( request ) );
			}
		
		return requests;
		} catch(Exception e){
			throw new NdexException("Unable to retrieve sent requests");
		}
	}
	
	
	/*
	 * Convert the database results into our object model
	 * TODO should this be moved to util? being used by other classes, not really a data object but a helper class
	 */
	public static User getUserFromDocument(ODocument n) {
		
		User result = new User();
		
		result.setExternalId(UUID.fromString((String)n.field("UUID")));
		result.setAccountName((String)n.field("accountName"));
		result.setEmailAddress((String)n.field("emailAddress"));
		result.setFirstName((String)n.field("firstName"));
		result.setLastName((String)n.field("lastName"));
		result.setWebsite((String)n.field("websiteURL"));
		result.setDescription((String)n.field("description"));
		result.setImage((String)n.field("imageURL"));
		result.setCreationDate((Date)n.field("creationDate"));
		result.setModificationDate((Date)n.field("modificationDate"));
		
		return result;
	}
	
	/*
	 * Both a User's AccountName and emailAddress must be unique in the database.
	 * Throw a DuplicateObjectException if that is not the case
	 */

	
	protected void checkForExistingUser(final NewUser newUser) 
			throws DuplicateObjectException, NdexException {
		try {
		OIndex<?> Idx = this.db.getMetadata().getIndexManager().getIndex("index-user-username");
		OIdentifiable user = (OIdentifiable) Idx.get(newUser.getAccountName()); // account to traverse by
		
		if(user != null) {
			logger.info("User with accountName " + newUser.getAccountName() + " already exists");
			throw new DuplicateObjectException(
					CommonDAOValues.DUPLICATED_ACCOUNT_FLAG);
		}
		OIndex<?> emailIdx = this.db.getMetadata().getIndexManager().getIndex("index-user-emailAddress");
		user = (OIdentifiable) emailIdx.get(newUser.getEmailAddress()); // account to traverse by
			
		if(user != null) {
			logger.info("User with emailAddress " + newUser.getEmailAddress() + " already exists");
			throw new DuplicateObjectException(
					CommonDAOValues.DUPLICATED_EMAIL_FLAG);
		}
		} catch (DuplicateObjectException e) {
			throw e;
		} catch (Exception e) {
			logger.info("Unexpected error on existing user check");
			throw new NdexException(e.getMessage());
		}
		
	}
	
}
