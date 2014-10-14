package org.ndexbio.common.models.dao.orientdb;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.Date;

import org.apache.commons.lang.StringEscapeUtils;
import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.exceptions.DuplicateObjectException;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.util.NdexUUIDFactory;
import org.ndexbio.common.util.Security;
import org.ndexbio.model.object.Membership;
import org.ndexbio.model.object.MembershipType;
import org.ndexbio.model.object.Permissions;
import org.ndexbio.model.object.Request;
import org.ndexbio.model.object.ResponseType;
import org.ndexbio.model.object.Status;
import org.ndexbio.model.object.Task;
import org.ndexbio.model.object.User;
import org.ndexbio.model.object.NewUser;
import org.ndexbio.model.object.SimpleUserQuery;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

public class UserDAO extends OrientdbDAO {

	private OrientGraph graph;
	private static final Logger logger = Logger.getLogger(UserDAO.class
			.getName());

	/*
	 * User operations can be achieved with Orient Document API methods. The
	 * constructor will need to accept a OrientGraph object if we wish to use
	 * the Graph API.
	 */
	/**************************************************************************
	 * UserDAO
	 * 
	 * @param db
	 *            Database instance from the Connection pool, should be opened
	 * @param graph
	 *            OrientGraph instance for Graph API operations
	 **************************************************************************/
	@Deprecated
	public UserDAO(ODatabaseDocumentTx db, OrientGraph graph) {
		super(db);
		//this.db = graph.getRawGraph();
		this.graph = graph;
	}

	public UserDAO(ODatabaseDocumentTx db) {
		super(db);
		//this.db = db;
		this.graph = new OrientGraph(db, false);
	}
	
	/*
	public UserDAO(ODatabaseDocumentTx db, boolean autoStartTx) {
		super(db);
		this.graph = new OrientGraph(db);
		//this.db = this.graph.getRawGraph();
	} */

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

		if (Strings.isNullOrEmpty(accountName)
				|| Strings.isNullOrEmpty(password))
			throw new SecurityException("No accountName or password entered.");

		try {
			final ODocument OAuthUser = this.getRecordByAccountName(
					accountName, NdexClasses.User);
			if (!Security.authenticateUser(password, OAuthUser)) {
				throw new SecurityException("Invalid accountName or password.");
			}
			return UserDAO.getUserFromDocument(OAuthUser);
		} catch (SecurityException se) {
			logger.info("Authentication failed: " + se.getMessage());
			throw se;
		} catch (ObjectNotFoundException e) {
			throw new SecurityException(e.getMessage());
		} catch (Exception e) {
			throw new NdexException(
					"There's a problem with the authentication server. Please try again later."+e.getMessage());
		}
	}

	/**************************************************************************
	 * Create a new user
	 * 
	 * @param newUser
	 *            A User object, from the NDEx Object Model
	 * @throws NdexException
	 *             Attempting to save an ODocument to the database
	 * @throws IllegalArgumentException
	 *             The newUser does not contain proper fields
	 * @throws DuplicateObjectException
	 *             The account name and/or email already exist
	 * @returns User object, from the NDEx Object Model
	 **************************************************************************/
	public User createNewUser(NewUser newUser) throws NdexException,
			IllegalArgumentException, DuplicateObjectException {

		Preconditions.checkArgument(null != newUser,
				"A user object is required");
		Preconditions.checkArgument(
				!Strings.isNullOrEmpty(newUser.getAccountName()),
				"A accountName is required");
		Preconditions.checkArgument(
				!Strings.isNullOrEmpty(newUser.getPassword()),
				"A user password is required");
		Preconditions.checkArgument(
				!Strings.isNullOrEmpty(newUser.getEmailAddress()),
				"A user email address is required");

		this.checkForExistingUser(newUser);

		try {

			ODocument user = new ODocument(NdexClasses.User);
			user.field("description", newUser.getDescription());
			user.field("websiteURL", newUser.getWebsite());
			user.field("imageURL", newUser.getImage());
			user.field("emailAddress", newUser.getEmailAddress());
			user.field("firstName", newUser.getFirstName());
			user.field("lastName", newUser.getLastName());
			user.field("accountName", newUser.getAccountName());
			user.field("password", Security.hashText(newUser.getPassword()));
			user.field(NdexClasses.ExternalObj_ID, NdexUUIDFactory.INSTANCE.getNDExUUID());
			user.field(NdexClasses.ExternalObj_cTime, new Date());
			user.field(NdexClasses.ExternalObj_mTime, new Date());

			user = user.save();

			logger.info("A new user with accountName "
					+ newUser.getAccountName() + " has been created");

			return UserDAO.getUserFromDocument(user);
		} catch (Exception e) {
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
	 *             Attempting to access and delete an ODocument from the
	 *             database
	 **************************************************************************/
	public void deleteUserById(UUID id) throws NdexException,
			ObjectNotFoundException {
		Preconditions.checkArgument(null != id, "UUID required");

		ODocument user = this.getRecordById(id, NdexClasses.User);

		/*
		 * if( !this.getUserGroupMemberships(id, Permissions.ADMIN, 0,
		 * 5).isEmpty() || !this.getUserNetworkMemberships(id,
		 * Permissions.ADMIN, 0, 5).isEmpty() ) { throw new
		 * NdexException("Cannot orphan networks or groups"); }
		 */

		try {
			OrientVertex vUser = graph.getVertex(user);
			boolean safe = true;

			// TODO, simplfy by using actual edge directions and labels
			for (Edge e : vUser.getEdges(Direction.BOTH)) {/*
															 * ,
															 * Permissions.ADMIN
															 * .toString().
															 * toLowerCase() +
															 * " " +
															 * Permissions.
															 * GROUPADMIN
															 * .toString
															 * ().toLowerCase()
															 * ) ) {
															 */

				OrientVertex vResource = (OrientVertex) e
						.getVertex(Direction.IN);

				if (!(vResource.getRecord().getSchemaClass().getName()
						.equals(NdexClasses.Group) || vResource.getRecord()
						.getSchemaClass().getName().equals(NdexClasses.Network)))
					continue;
				safe = false;

				for (Edge ee : vResource.getEdges(Direction.BOTH/*
																 * ,
																 * Permissions.
																 * ADMIN
																 * .toString
																 * ().toLowerCase
																 * ()
																 */)) {
					if (!((OrientVertex) ee.getVertex(Direction.OUT))
							.equals(vUser)) {
						safe = true;
					}
				}

			}

			if (!safe)
				throw new NdexException("Cannot orphan groups or networks");

			user.delete();
		} catch (Exception e) {
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
	 *             Attempting to query the database
	 * @returns User object, from the NDEx Object Model
	 **************************************************************************/

	public User getUserById(UUID id) throws NdexException,
			IllegalArgumentException, ObjectNotFoundException {

		Preconditions.checkArgument(null != id, "UUID required");

		final ODocument user = this.getRecordById(id, NdexClasses.User);
		return UserDAO.getUserFromDocument(user);

	}

	/**************************************************************************
	 * Get a user
	 * 
	 * @param accountName
	 *            accountName for User
	 * @throws NdexException
	 *             Attempting to query the database
	 * @returns User object, from the NDEx Object Model
	 **************************************************************************/
	public User getUserByAccountName(String accountName) throws NdexException,
			IllegalArgumentException, ObjectNotFoundException {

		Preconditions.checkArgument(!Strings.isNullOrEmpty(accountName),
				"accountName required");

		final ODocument user = this.getRecordByAccountName(accountName,
				NdexClasses.User);
		return UserDAO.getUserFromDocument(user);

	}

	/**************************************************************************
	 * Find users
	 * 
	 * @param id
	 *            UUID for User
	 * @param skip
	 *            amount of blocks to skip
	 * @param top
	 *            block size
	 * @throws NdexException
	 *             Attempting to query the database
	 * @returns User object, from the NDEx Object Model
	 **************************************************************************/
	public List<User> findUsers(SimpleUserQuery simpleQuery, int skip, int top)
			throws IllegalArgumentException, NdexException {
		Preconditions.checkArgument(simpleQuery != null,
				"Search parameters are required");

		String traversePermission;
		OSQLSynchQuery<ODocument> query;
		Iterable<ODocument> users;
		final List<User> foundUsers = new ArrayList<>();
		
		String searchStr = simpleQuery.getSearchString().toLowerCase();
		
		if (searchStr.equals("*") )
			searchStr = "";
		
		if (simpleQuery.getPermission() == null)
			traversePermission = "in_groupadmin, in_member";
		else
			traversePermission = "in_"
					+ simpleQuery.getPermission().name().toLowerCase();

		//StringEscapeUtils.escapeJava()
		searchStr = Helper.escapeOrientDBSQL(searchStr.toLowerCase().trim());
		final int startIndex = skip * top;

		try {

			if (!Strings.isNullOrEmpty(simpleQuery.getAccountName())) {
				ODocument nGroup = this.getRecordByAccountName(simpleQuery.getAccountName(), NdexClasses.Group);

				if (nGroup == null)
					throw new NdexException("Invalid accountName to filter by");

				String traverseRID = nGroup.getIdentity().toString();
				query = new OSQLSynchQuery<>("SELECT FROM"
						+ " (TRAVERSE "
						+ traversePermission
						+ " FROM"
						+ " "
						+ traverseRID
						+ " WHILE $depth <=1)"
						+ " WHERE @class = '"
						+ NdexClasses.User
						+ "'"
						+ " AND accountName.toLowerCase() LIKE '%"
						+ searchStr
						+ "%'"
						+ "  OR lastName.toLowerCase() LIKE '%"
						+ searchStr
						+ "%'"
						+ "  OR firstName.toLowerCase() LIKE '%"
						+ searchStr
						+ "%'"
						+ " ORDER BY " + NdexClasses.ExternalObj_cTime + " DESC "
						+ " SKIP "
						+ startIndex + " LIMIT " + top);

				users = this.db.command(query).execute();

		/*		if (!users.iterator().hasNext()  && simpleQuery.getSearchString().equals("")) {

					query = new OSQLSynchQuery<>("SELECT FROM"
							+ " (TRAVERSE " + traversePermission + " FROM"
							+ " " + traverseRID + " WHILE $depth <=1)"
							+ " WHERE @class = '" + NdexClasses.User + "'"
							+ " ORDER BY " + NdexClasses.ExternalObj_cTime + " DESC " + " SKIP "
							+ startIndex + " LIMIT " + top);

					users = this.db.command(query).execute();

				} */

				for (final ODocument user : users) {
					foundUsers.add(UserDAO.getUserFromDocument(user));
				}
				return foundUsers;

			} 
				
			query = new OSQLSynchQuery<>("SELECT FROM "
						+ NdexClasses.User + " "
						+ "WHERE accountName.toLowerCase() LIKE '%"
						+ searchStr + "%'"
						+ "  OR lastName.toLowerCase() LIKE '%"
						+ searchStr + "%'"
						+ "  OR firstName.toLowerCase() LIKE '%"
						+ searchStr + "%'"
						+ "  ORDER BY " + NdexClasses.ExternalObj_cTime + " DESC " + " SKIP "
						+ startIndex + " LIMIT " + top);

			users = this.db.command(query).execute();

			for (final ODocument user : users) {
					foundUsers.add(UserDAO.getUserFromDocument(user));
			}
			return foundUsers;

		} catch (Exception e) {
			logger.severe("Unable to query the database");
			throw new NdexException("Failed to search for users.\n"
					+ e.getMessage());

		}

	}

	/**************************************************************************
	 * Email a new password
	 * 
	 * @param accountName
	 *            accountName for the User
	 * @throws NdexException
	 *             Attempting to query the database
	 * @throws IllegalArgumentException
	 *             accountName is required
	 * @throws ObjectNotFoundException
	 *             user with account name does not exist
	 * @returns response
	 **************************************************************************/
	public String setNewPassword(String accountName)
			throws IllegalArgumentException, ObjectNotFoundException,
			NdexException {

		Preconditions.checkArgument(!Strings.isNullOrEmpty(accountName),
				"An accountName is required");

		try {

			ODocument userToSave = this.getRecordByAccountName(accountName,
					NdexClasses.User);

	//		final User authUser = UserDAO.getUserFromDocument(userToSave);
			final String newPassword = Security.generatePassword();
			final String password = Security.hashText(newPassword);
			userToSave.field("password", password).save();
            
			return newPassword;
			
/*			final File forgotPasswordFile = new File(Configuration
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
			return Response.ok().build(); */

		} catch (ObjectNotFoundException onfe) {

			throw onfe;

		} catch (Exception e) {

			throw new NdexException("Failed to recover your password: \n"
					+ e.getMessage());

		}
	}

	/**************************************************************************
	 * Change a user's password
	 * 
	 * @param id
	 *            UUID for user
	 * @param password
	 *            new password for user
	 * @throws NdexException
	 *             Attempting to access the database
	 * @throws IllegalArgumentException
	 *             new password and user id are required
	 * @throws ObjectNotFoundException
	 *             user does not exist
	 * @returns response
	 **************************************************************************/
	public void changePassword(String password, UUID id)
			throws IllegalArgumentException, NdexException,
			ObjectNotFoundException {

		Preconditions.checkNotNull(id, "A user id is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(password),
				"A password is required");

		ODocument user = this.getRecordById(id, NdexClasses.User);

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

			logger.severe("An error occured while saving password for user with UUID "
					+ id);
			throw new NdexException("Failed to change your password.\n"
					+ e.getMessage());

		}

	}

	/**************************************************************************
	 * Change a user's email Address
	 * 
	 * @param id
	 *            UUID for user
	 * @param emailAddress
	 *            new email address
	 * @throws NdexException
	 *             Attempting to access the database
	 * @throws IllegalArgumentException
	 *             new password and user id are required
	 * @throws ObjectNotFoundException
	 *             user does not exist
	 * @returns response
	 **************************************************************************/
/*	public Response changeEmailAddress(String emailAddress, UUID id)
			throws IllegalArgumentException, NdexException,
			ObjectNotFoundException, DuplicateObjectException {

		Preconditions.checkNotNull(id, "A user id is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(emailAddress),
				"A password is required");

		ODocument user = this.getRecordById(id, NdexClasses.User);

		try {

			// check for unique emailAddress
			String query = "select emailAddress from " + NdexClasses.User
					+ " where emailAddress = ?";
			List<ODocument> existingUser = db.command(new OCommandSQL(query))
					.execute(emailAddress);

			if (!existingUser.isEmpty()) {
				logger.severe("Email address already exists in the database.");
				throw new NdexException("email address is taken");
			}

			final String oldEmail = (String) user.field("emailAddress");
			user.field("emailAddress", emailAddress);
			user.save();

			// send emails to new and old address
			final File ChangeEmailFile = new File(Configuration.getInstance()
					.getProperty("Change-Email-File"));

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
							"Change-Email-Email"), oldEmail, "Email Change",
					changeEmailText.toString());
			Email.sendEmail(
					Configuration.getInstance().getProperty(
							"Change-Email-Email"), emailAddress,
					"Email Change", changeEmailText.toString());

			logger.info("Changed email address for user with UUID " + id);

			return Response.ok().build();

		} catch (Exception e) {

			logger.severe("An error occured while changing email for user with UUID "
					+ id);
			throw new NdexException("Failed to change your email.\n"
					+ e.getMessage());

		}

	}
*/
	/**************************************************************************
	 * Update a user
	 * 
	 * @param updatedUser
	 *            User with new information
	 * @param id
	 *            UUID for user
	 * @throws NdexException
	 *             Attempting to access the database
	 * @throws IllegalArgumentException
	 *             new password and user id are required
	 * @throws ObjectNotFoundException
	 *             user does not exist
	 * @return User object
	 **************************************************************************/
	public User updateUser(User updatedUser, UUID id)
			throws IllegalArgumentException, NdexException,
			ObjectNotFoundException {

		Preconditions.checkArgument(id != null, "A user id is required");
		Preconditions.checkArgument(updatedUser != null,
				"An updated user is required");

		ODocument user = this.getRecordById(id, NdexClasses.User);

		try {
			// updatedUser.getDescription().isEmpty();
			if (!Strings.isNullOrEmpty(updatedUser.getDescription()))
				user.field("description", updatedUser.getDescription());
			if (!Strings.isNullOrEmpty(updatedUser.getWebsite()))
				user.field("websiteURL", updatedUser.getWebsite());
			if (!Strings.isNullOrEmpty(updatedUser.getImage()))
				user.field("imageURL", updatedUser.getImage());
			if (!Strings.isNullOrEmpty(updatedUser.getFirstName()))
				user.field("firstName", updatedUser.getFirstName());
			if (!Strings.isNullOrEmpty(updatedUser.getLastName()))
				user.field("lastName", updatedUser.getLastName());
			user.field(NdexClasses.ExternalObj_mTime, updatedUser.getModificationTime());

			user = user.save();
			logger.info("Updated user profile with UUID " + id);

			return UserDAO.getUserFromDocument(user);

		} catch (Exception e) {

			logger.severe("An error occured while updating user profile with UUID "
					+ id);
			throw new NdexException(e.getMessage());

		}

	}

	/**************************************************************************
	 * getUserNetworkMemberships
	 * 
	 * @param userId
	 *            UUID for associated user
	 * @param permission
	 *            Type of memberships to retrieve, ADMIN, WRITE, or READ
	 * @param skipBlocks
	 *            amount of blocks to skip
	 * @param blockSize
	 *            The size of blocks to be skipped and retrieved
	 * @throws NdexException
	 *             Invalid parameters or an error occurred while accessing the
	 *             database
	 * @throws ObjectNotFoundException
	 *             Invalid userId
	 **************************************************************************/

	public List<Membership> getUserNetworkMemberships(UUID userId,
			Permissions permission, int skipBlocks, int blockSize)
			throws ObjectNotFoundException, NdexException {

		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId.toString()),
				"A user UUID is required");
		Preconditions.checkArgument((permission == Permissions.ADMIN)
				|| (permission == Permissions.READ)
				|| (permission == Permissions.WRITE),
				"Valid permissions required");

		ODocument user = this.getRecordById(userId, NdexClasses.User);

		final int startIndex = skipBlocks * blockSize;

		try {
			List<Membership> memberships = new ArrayList<>();

			String userRID = user.getIdentity().toString();

			OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>(
					"SELECT FROM" + " (TRAVERSE out_"
							+ Permissions.GROUPADMIN.name().toLowerCase()
							+ ", out_"
							+ permission.name().toString().toLowerCase()
							+ " FROM" + " " + userRID + "  WHILE $depth <=2)"
							+ " WHERE @class = '" + NdexClasses.Network + "'"
							+ " ORDER BY " + NdexClasses.ExternalObj_cTime + " DESC " + " SKIP "
							+ startIndex + " LIMIT " + blockSize);

			List<ODocument> records = this.db.command(query).execute();
			for (ODocument network : records) {

				Membership membership = new Membership();
				membership.setMembershipType(MembershipType.NETWORK);
				membership.setMemberAccountName((String) user
						.field("accountName"));
				membership.setMemberUUID(userId);
				membership.setPermissions(permission);
				membership.setResourceName((String) network.field("name"));
				membership.setResourceUUID(UUID.fromString((String) network
						.field("UUID")));

				memberships.add(membership);
			}

			logger.info("Successfuly retrieved user-network memberships");
			return memberships;

		} catch (Exception e) {
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
	 *            Type of memberships to retrieve, ADMIN, WRITE, or READ
	 * @param skipBlocks
	 *            amount of blocks to skip
	 * @param blockSize
	 *            The size of blocks to be skipped and retrieved
	 * @throws NdexException
	 *             Invalid parameters or an error occurred while accessing the
	 *             database
	 * @throws ObjectNotFoundException
	 *             Invalid userId
	 **************************************************************************/

	public List<Membership> getUserGroupMemberships(UUID userId,
			Permissions permission, int skipBlocks, int blockSize)
			throws ObjectNotFoundException, NdexException {

		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId.toString()),
				"A user UUID is required");
		Preconditions.checkArgument((permission.equals(Permissions.GROUPADMIN))
				|| (permission.equals(Permissions.MEMBER)),
				"Valid permissions required");

		ODocument user = this.getRecordById(userId, NdexClasses.User);

		final int startIndex = skipBlocks * blockSize;

		try {
			List<Membership> memberships = new ArrayList<>();

			String userRID = user.getIdentity().toString();
			OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>(
					"SELECT FROM" + " (TRAVERSE " + NdexClasses.User + ".out_"
							+ permission.name().toString().toLowerCase()
							+ " FROM" + " " + userRID + "  WHILE $depth <=1)"
							+ " WHERE @class = '" + NdexClasses.Group + "'"
							+ " ORDER BY " + NdexClasses.ExternalObj_cTime + " DESC " + " SKIP "
							+ startIndex + " LIMIT " + blockSize);

			List<ODocument> records = this.db.command(query).execute();
			for (ODocument group : records) {

				Membership membership = new Membership();
				membership.setMembershipType(MembershipType.GROUP);
				membership.setMemberAccountName((String) user
						.field("accountName"));
				membership.setMemberUUID(userId);
				membership.setPermissions(permission);
				membership.setResourceName((String) group
						.field("organizationName"));
				membership.setResourceUUID(UUID.fromString((String) group
						.field("UUID")));

				memberships.add(membership);
			}

			logger.info("Successfuly retrieved user-group memberships");
			return memberships;

		} catch (Exception e) {
			logger.severe("An unexpected error occured while retrieving user-group memberships");
			throw new NdexException(e.getMessage());
		}
	}

	/**************************************************************************
	 * getMembership
	 * 
	 * @param account
	 *            UUID for user or group
	 * @param resource
	 *            UUID for resource
	 * @throws NdexException
	 *             Invalid parameters or an error occurred while accessing the
	 *             database
	 * @throws ObjectNotFoundException
	 *             Invalid userId
	 **************************************************************************/

	public Membership getMembership(UUID account, UUID resource, int depth)
			throws IllegalArgumentException, ObjectNotFoundException,
			NdexException {

		Preconditions.checkArgument(account != null, "Account UUID required");
		Preconditions.checkArgument(resource != null, "Resource UUID required");
		Preconditions.checkArgument(depth > 0 && depth < 3, "Depth range: [1,2]");

		ODocument OAccount = this.getRecordById(account, NdexClasses.Account);
		ODocument OResource = this.getRecordById(resource, null);

		Permissions permission = null;
		Membership membership = new Membership();

		if (OResource.getClassName().equals(NdexClasses.Group)) {
			if (this.checkPermission(OAccount.getIdentity(),
					OResource.getIdentity(), Direction.OUT, depth,
					Permissions.GROUPADMIN))
				permission = Permissions.GROUPADMIN;
			if (this.checkPermission(OAccount.getIdentity(),
					OResource.getIdentity(), Direction.OUT, depth,
					Permissions.MEMBER))
				permission = Permissions.MEMBER;

			membership.setMemberAccountName((String) OAccount
					.field("accountName"));
			membership.setMemberUUID(account);
			membership.setResourceName((String) OResource
					.field("organizationName"));
			membership.setResourceUUID(resource);
			membership.setPermissions(permission);
			membership.setMembershipType(MembershipType.GROUP);

		} else {
			// order allows us to return most permissive permission
			if (this.checkPermission(OAccount.getIdentity(),
					OResource.getIdentity(), Direction.OUT, depth, 
					Permissions.READ, Permissions.GROUPADMIN, Permissions.MEMBER))
				permission = Permissions.READ;
			if (this.checkPermission(OAccount.getIdentity(),
					OResource.getIdentity(), Direction.OUT, depth,
					Permissions.WRITE, Permissions.GROUPADMIN, Permissions.MEMBER))
				permission = Permissions.WRITE;
			if (this.checkPermission(OAccount.getIdentity(),
					OResource.getIdentity(), Direction.OUT, depth,
					Permissions.ADMIN, Permissions.GROUPADMIN, Permissions.MEMBER))
				permission = Permissions.ADMIN;

			membership.setMemberAccountName((String) OAccount
					.field("accountName"));
			membership.setMemberUUID(account);
			membership.setResourceName((String) OResource.field("name"));
			membership.setResourceUUID(resource);
			membership.setPermissions(permission);
			membership.setMembershipType(MembershipType.NETWORK);

		}

		if (permission != null)
			return membership;

		return null;
	}

	/**************************************************************************
	 * getSentRequest
	 * 
	 * @param account
	 *            User object
	 * @param skipBlocks
	 *            amount of blocks to skip
	 * @param blockSize
	 *            The size of blocks to be skipped and retrieved
	 * @throws NdexException
	 *             An error occurred while accessing the database
	 * @throws ObjectNotFoundException
	 *             Invalid userId
	 **************************************************************************/

	public List<Request> getSentRequest(User account, int skipBlocks,
			int blockSize) throws ObjectNotFoundException, NdexException {
		Preconditions.checkArgument(account != null, "Must be logged in");
		// TODO May possibly add extra parameter to specify type of request to
		// return

		final List<Request> requests = new ArrayList<>();

		ODocument user = this.getRecordById(account.getExternalId(),
				NdexClasses.User);
		final int startIndex = skipBlocks * blockSize;

		try {

			OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>(
					"SELECT FROM" + " (TRAVERSE out_requests FROM" + " "
							+ user.getIdentity().toString()
							+ "  WHILE $depth <=1)" + " WHERE @class = '"
							+ NdexClasses.Request + "'"
							+ " ORDER BY " + NdexClasses.ExternalObj_cTime + " DESC " + " SKIP "
							+ startIndex + " LIMIT " + blockSize);

			List<ODocument> records = this.db.command(query).execute();

			for (ODocument request : records) {
				requests.add(RequestDAO.getRequestFromDocument(request));
			}

			return requests;
		} catch (Exception e) {
			logger.severe("Unable to retrieve requests : " + e.getMessage());
			throw new NdexException("Unable to retrieve sent requests" + e.getMessage());
		}
	}

	/**************************************************************************
	 * getPendingRequest
	 * 
	 * @param account
	 *            User object
	 * @param skipBlocks
	 *            amount of blocks to skip
	 * @param blockSize
	 *            The size of blocks to be skipped and retrieved
	 * @throws NdexException
	 *             An error occurred while accessing the database
	 * @throws ObjectNotFoundException
	 *             Invalid userId
	 **************************************************************************/
	public List<Request> getPendingRequest(User account, int skipBlocks,
			int blockSize) throws ObjectNotFoundException, NdexException {
		Preconditions.checkArgument(account != null, "Must be logged in");
		// TODO May possibly add extra parameter to specify type of request to
		// return

		final List<Request> requests = new ArrayList<>();

		ODocument user = this.getRecordById(account.getExternalId(),
				NdexClasses.User);
		final int startIndex = skipBlocks * blockSize;

		try {
			OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>(
					"SELECT FROM" + " (TRAVERSE in_requests FROM" + " "
							+ user.getIdentity().toString()
							+ "  WHILE $depth <=1)" + " WHERE @class = '"
							+ NdexClasses.Request + "'" + " AND response = '"
							+ ResponseType.PENDING + "'"
							+ " ORDER BY " + NdexClasses.ExternalObj_cTime + " DESC " + " SKIP "
							+ startIndex + " LIMIT " + blockSize);

			List<ODocument> records = this.db.command(query).execute();

			for (ODocument request : records) {
				requests.add(RequestDAO.getRequestFromDocument(request));
			}

			return requests;
		} catch (Exception e) {
			throw new NdexException("Unable to retrieve sent requests");
		}
	}

	public List<Task> getTasks(User account, Status status,
			int skipBlocks, int blockSize) throws ObjectNotFoundException,
			NdexException {

		Preconditions.checkArgument(account != null, "Must be logged in");

		final List<Task> tasks = new ArrayList<>();

		ODocument user = this.getRecordById(account.getExternalId(),
				NdexClasses.User);
		final int startIndex = skipBlocks * blockSize;
		
		String statusFilter = "";
		if (status != Status.ALL){
			statusFilter = " status = '" + status + "'";
		}

		try {
			OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>(
					"SELECT FROM" + " (TRAVERSE in_ownedBy FROM" + " "
							+ user.getIdentity().toString()
							+ "  WHILE $depth <=1)"
							+ " WHERE @class = '" + NdexClasses.Task + "'" 
							+ statusFilter
							+ " ORDER BY " + NdexClasses.ExternalObj_cTime + " DESC " 
							+ " SKIP " + startIndex 
							+ " LIMIT " + blockSize);

			List<ODocument> records = this.db.command(query).execute();

			for (ODocument task : records) {
				tasks.add(TaskDAO.getTaskFromDocument(task));
			}

			return tasks;

		} catch (Exception e) {
			throw new NdexException("Unable to retrieve user tasks");
		}
	}
	
	public void begin() {
		this.graph.getRawGraph().begin();
	}

	public void commit() {
		this.graph.commit();
	}
	
	public void rollback() {
		this.graph.rollback();
	}
	
	public void close() {
		//this.graph.getRawGraph().close(); // closing raw graph will prevent commit
		this.graph.shutdown();
		//if(!this.db.isClosed())  // needed to empty pool?
			//this.db.close();
	}
	
	/*
	 * Convert the database results into our object model 
	 */
	public static User getUserFromDocument(ODocument n) {

		User result = new User();

		Helper.populateExternalObjectFromDoc (result, n);

		result.setAccountName((String) n.field("accountName"));
		result.setEmailAddress((String) n.field("emailAddress"));
		result.setFirstName((String) n.field("firstName"));
		result.setLastName((String) n.field("lastName"));
		result.setWebsite((String) n.field("websiteURL"));
		result.setDescription((String) n.field("description"));
		result.setImage((String) n.field("imageURL"));

		return result;
	}

	/*
	 * Both a User's AccountName and emailAddress must be unique in the
	 * database. Throw a DuplicateObjectException if that is not the case
	 */

	protected void checkForExistingUser(final NewUser newUser)
			throws DuplicateObjectException, NdexException {
		try {
			OIndex<?> Idx = this.db.getMetadata().getIndexManager()
					.getIndex("index-user-username");
			OIdentifiable user = (OIdentifiable) Idx.get(newUser
					.getAccountName()); // account to traverse by

			if (user != null) {
				logger.info("User with accountName " + newUser.getAccountName()
						+ " already exists");
				throw new DuplicateObjectException(
						CommonDAOValues.DUPLICATED_ACCOUNT_FLAG);
			}
			OIndex<?> emailIdx = this.db.getMetadata().getIndexManager()
					.getIndex("index-user-emailAddress");
			user = (OIdentifiable) emailIdx.get(newUser.getEmailAddress()); // account
																			// to
																			// traverse
																			// by

			if (user != null) {
				logger.info("User with emailAddress "
						+ newUser.getEmailAddress() + " already exists");
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
