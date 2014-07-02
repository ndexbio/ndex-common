package org.ndexbio.common.models.dao;

import java.util.List;

import javax.ws.rs.core.Response;

import org.ndexbio.common.exceptions.DuplicateObjectException;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.models.object.SearchParameters;
import org.ndexbio.common.models.object.UploadedFile;
import org.ndexbio.common.models.object.network.Network;
import org.ndexbio.model.object.NewUser;
import org.ndexbio.model.object.User;

public interface UserDAO {

	/**************************************************************************
	 * Adds a network to the user's Work Surface.
	 * 
	 * @param networkId
	 *            The user's ID.
	 *
	 * @param userId
	 *            The network ID.
	 *
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws ObjectNotFoundException
	 *             The network wasn't found.
	 * @throws NdexException
	 *             Failed to add the network in the database.
	 **************************************************************************/
	public abstract Iterable<Network> addNetworkToWorkSurface(String networkId, String userId)
			throws IllegalArgumentException, ObjectNotFoundException,
			NdexException;

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
	 * @return The authenticated user's information.
	 **************************************************************************/
	public abstract User authenticateUser(String username, String password)
			throws SecurityException, NdexException;

	/**************************************************************************
	 * Changes a user's password.
	 * 
	 * @param userId
	 *            The user ID.
	 * @param password
	 *            The new password.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws NdexException
	 *             Failed to change the password in the database.
	 **************************************************************************/
	public abstract void changePassword(String password, String userId)
			throws IllegalArgumentException, NdexException;

	/**************************************************************************
	 * Changes a user's profile/background image.
	 * 
	 * @param imageType
	 *            The image type.
	 * @param uploadedImage
	 *            The uploaded image.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws NdexException
	 *             Failed to save the image.
	 **************************************************************************/
	public abstract void changeProfileImage(String imageType,
			UploadedFile uploadedImage, String userId) throws IllegalArgumentException,
			NdexException;

	/**************************************************************************
	 * Creates a user.
	 * 
	 * @param newUser
	 *            The user to create.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws DuplicateObjectException
	 *             A user with the same username/email address already exists.
	 * @throws NdexException
	 *             Failed to create the user in the database.
	 * @return The new user's profile.
	 **************************************************************************/
	public abstract User createUser(NewUser newUser)
			throws IllegalArgumentException, DuplicateObjectException,
			NdexException;

	/**************************************************************************
	 * Deletes a network from a user's Work Surface.
	 * 
	 * @param networkToDelete
	 *            The network being removed.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws ObjectNotFoundException
	 *             The network doesn't exist.
	 * @throws NdexException
	 *             Failed to remove the network in the database.
	 **************************************************************************/
	public abstract Iterable<Network> deleteNetworkFromWorkSurface(
			String networkId, String userId) throws IllegalArgumentException,
			ObjectNotFoundException, NdexException;

	/**************************************************************************
	 * Deletes a user.
	 * 
	 * @throws NdexException
	 *             Failed to delete the user from the database.
	 **************************************************************************/
	public abstract void deleteUser(String userId) throws NdexException;

	/**************************************************************************
	 * Emails the user a new randomly generated password.
	 * 
	 * @param username
	 *            The username of the user.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws NdexException
	 *             Failed to change the password in the database, or failed to
	 *             send the email.
	 **************************************************************************/
	public abstract Response emailNewPassword(String username)
			throws IllegalArgumentException, NdexException;

	/**************************************************************************
	 * Finds users based on the search parameters.
	 * 
	 * @param searchParameters
	 *            The search parameters.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws NdexException
	 *             Failed to query the database.
	 **************************************************************************/
	public abstract List<User> findUsers(SearchParameters searchParameters,
			String searchOperator) throws IllegalArgumentException,
			NdexException;

	/**************************************************************************
	 * Gets a user by ID or username.
	 * 
	 * @param userId
	 *            The ID or username of the user.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws NdexException
	 *             Failed to change the password in the database.
	 **************************************************************************/
	@Deprecated
	public abstract User getUser(String userId)
			throws IllegalArgumentException, NdexException;
	
	
	/**
	 * Get User object from its accountName
	 * @param accountName
	 * @return The User object if found. Null if the given user is not found.
	 */
	public User getUserByAccountName(String accountName);
	

	/**************************************************************************
	 * Updates a user.
	 * 
	 * @param updatedUser
	 *            The updated user information.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws SecurityException
	 *             Users trying to update someone else.
	 * @throws NdexException
	 *             Failed to update the user in the database.
	 **************************************************************************/
	public abstract void updateUser(User updatedUser, String userId)
			throws IllegalArgumentException, SecurityException, NdexException;

}