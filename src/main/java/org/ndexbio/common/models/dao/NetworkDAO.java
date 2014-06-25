package org.ndexbio.common.models.dao;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.ndexbio.common.exceptions.DuplicateObjectException;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.models.data.IEdge;
import org.ndexbio.common.models.data.INetwork;
import org.ndexbio.common.models.data.INode;
import org.ndexbio.common.models.object.NetworkQueryParameters;
import org.ndexbio.common.models.object.SearchParameters;
import org.ndexbio.common.models.object.UploadedFile;
import org.ndexbio.common.models.object.network.BaseTerm;
import org.ndexbio.common.models.object.network.Citation;
import org.ndexbio.common.models.object.network.Namespace;
import org.ndexbio.common.models.object.network.Network;
import org.ndexbio.common.models.object.privilege.Membership;


public interface NetworkDAO {

	/**************************************************************************
	 * Suggests terms that start with the partial term.
	 * 
	 * @param networkId
	 *            The network ID.
	 * @param partialTerm
	 *            The partially entered term.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws NdexException
	 *             Failed to query the database.
	 * @return A collection of terms that start with the partial term.
	 **************************************************************************/
	public abstract Collection<String> autoSuggestTerms(String userId, String networkId,
			String partialTerm) throws IllegalArgumentException, NdexException;

	/**************************************************************************
	 * Creates a network.
	 * 
	 * @param ownerId
	 *            The ID of the user creating the group.
	 * @param newNetwork
	 *            The network to create.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws DuplicateObjectException
	 *             The user already has a network with the same title.
	 * @throws NdexException
	 *             Failed to create the network in the database.
	 * @return The newly created network.
	 **************************************************************************/
	public abstract Network createNetwork(String userId, Network newNetwork)
			throws IllegalArgumentException, DuplicateObjectException,
			NdexException;

	/**************************************************************************
	 * Creates a network.
	 * 
	 * @param ownerId
	 *            The ID of the user creating the group.
	 * @param sourceNetwork
	 *            The network to create.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws DuplicateObjectException
	 *             The user already has a network with the same title.
	 * @throws NdexException
	 *             Failed to create the network in the database.
	 * @return The newly created network.
	 **************************************************************************/
	public abstract Network addNetwork(String userId, String networkId,
			String equivalenceMethod, Network sourceNetwork)
			throws IllegalArgumentException, DuplicateObjectException,
			NdexException;

	/**************************************************************************
	 * Deletes a network.
	 * 
	 * @param networkId
	 *            The ID of the network to delete.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws ObjectNotFoundException
	 *             The network doesn't exist.
	 * @throws NdexException
	 *             Failed to delete the network from the database.
	 **************************************************************************/
	public abstract void deleteNetwork(String userId, String networkId)
			throws IllegalArgumentException, ObjectNotFoundException,
			NdexException;

	/**************************************************************************
	 * Searches for a network.
	 * 
	 * @param searchParameters
	 *            The search parameters.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws NdexException
	 *             Failed to query the database.
	 * @return Networks that match the search parameters.
	 **************************************************************************/
	public abstract List<Network> findNetworks(String userId,
			SearchParameters searchParameters, String searchOperator)
			throws IllegalArgumentException, NdexException;

	/**************************************************************************
	 * Gets a network by ID.
	 * 
	 * @param networkId
	 *            The ID of the network.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws SecurityException
	 *             The user doesn't have access to the network.
	 * @throws NdexException
	 *             Failed to query the database.
	 * @return The network.
	 **************************************************************************/
	public abstract Network getNetwork(String userId, String networkId)
			throws IllegalArgumentException, SecurityException, NdexException;

	/**************************************************************************
	 * Gets a page of edges for the specified network, along with the supporting
	 * nodes, terms, namespaces, supports, and citations.
	 * 
	 * @param networkId
	 *            The network ID.
	 * @param skip
	 *            The number of edges to skip.
	 * @param top
	 *            The number of edges to retrieve.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws NdexException
	 *             Failed to query the database.
	 * @return The edges of the network.
	 **************************************************************************/
	public abstract Network getEdges(String userId, String networkId, int skip, int top)
			throws IllegalArgumentException, NdexException;

	/**************************************************************************
	 * Gets a subnetwork with one block of nodes that are
	 * (String userId,1) contained in the queried network but
	 * (String userId,2) neither the subject nor object of a given edge. 
	 * 
	 * includes all terms, namespaces, supports, and citations linked to those nodes.
	 * 
	 * @param networkId
	 *            The network ID.
	 * @param skip
	 *            The number of blocks of nodes to skip.
	 * @param top
	 *            The number of nodes in a block to.
	 *            
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws NdexException
	 *             Failed to query the database.
	 * @return The edges of the network.
	 **************************************************************************/
	public abstract Network getNetworkByNonEdgeNodes(String userId, String networkId,
			int skip, int top) throws IllegalArgumentException, NdexException;

	/**************************************************************************
	 * Gets a subnetwork of a network corresponding to a page of edges for a specified 
	 * set of citations in the network.
	 * 
	 * POST DATA: citations - list of strings
	 * 
	 * @param networkId
	 *            The network ID.
	 * @param skip
	 *            The number of edges to skip.
	 * @param top
	 *            The number of edges to retrieve.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws NdexException
	 *             Failed to query the database.
	 * @return The edges of the network.
	 **************************************************************************/
	public abstract Network getEdgesByCitations(String userId, String networkId, int skip,
			int top, String[] citations) throws IllegalArgumentException,
			NdexException;

	/**************************************************************************
	 * Gets all BaseTerms in the network that are in Namespaces identified by a
	 * list of Namespace prefixes
	 * 
	 * @param networkId
	 *            The network ID.
	 * @param namespaces
	 *            A list of namespace prefixes, i.e. HGNC, UniProt, etc.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws NdexException
	 *             Failed to query the database.
	 * @return The BaseTerms in the found the Namespaces
	 **************************************************************************/
	public abstract List<BaseTerm> getTermsInNamespaces(String userId, String networkId,
			String namespaces[]) throws IllegalArgumentException, NdexException;

	/**************************************************************************
	 * Gets all terms in the network that intersect with a list of terms.
	 * 
	 * @param networkId
	 *            The network ID.
	 * @param terms
	 *            A list of terms being sought.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws NdexException
	 *             Failed to query the database.
	 * @return The edges of the network.
	 **************************************************************************/
	public abstract Iterable<BaseTerm> getIntersectingTerms(String userId, String networkId,
			String terms[]) throws IllegalArgumentException, NdexException;

	/**************************************************************************
	 * Gets a page of namespaces for the specified network.
	 * 
	 * @param networkId
	 *            The network ID.
	 * @param skip
	 *            The number of namespaces to skip.
	 * @param top
	 *            The number of namespaces to retrieve.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws NdexException
	 *             Failed to query the database.
	 * @return The edges of the network.
	 **************************************************************************/
	public abstract Iterable<Namespace> getNamespaces( String networkId,
			int skip, int top) throws IllegalArgumentException, NdexException;

	/**************************************************************************
	 * Gets a page of terms for the specified network.
	 * 
	 * @param networkId
	 *            The network ID.
	 * @param skip
	 *            The number of terms to skip.
	 * @param top
	 *            The number of terms to retrieve.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws NdexException
	 *             Failed to query the database.
	 * @return The edges of the network.
	 **************************************************************************/
	public abstract Iterable<BaseTerm> getTerms( String networkId, int skip,
			int top) throws IllegalArgumentException, NdexException;

	/**************************************************************************
	 * Gets a page of citations for the specified network.
	 * 
	 * @param networkId
	 *            The network ID.
	 * @param skip
	 *            The number of terms to skip.
	 * @param top
	 *            The number of terms to retrieve.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws NdexException
	 *             Failed to query the database.
	 * @return The an iterable of Citation objects
	 **************************************************************************/
	public abstract List<Citation> getCitations(String networkId, int skip,
			int top) throws IllegalArgumentException, NdexException;

	/**************************************************************************
	 * Gets a subnetwork of a network based on network query parameters.
	 * 
	 * @param networkId
	 *            The network ID.
	 * @param queryParameters
	 *            The query parameters.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws NdexException
	 *             Failed to query the database.
	 * @return A subnetwork of the network.
	 **************************************************************************/
	public abstract Network queryNetwork( String networkId,
			NetworkQueryParameters queryParameters)
			throws IllegalArgumentException, NdexException;

	
	/**************************************************************************
	 * Gets a subnetwork network based on network query parameters.
	 * 
	 * @param networkId
	 *            The network ID.
	 * @param queryParameters
	 *            The query parameters.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws NdexException
	 *             Failed to query the database.
	 * @return A subnetwork of the network.
	 **************************************************************************/
/*	public abstract Network queryNetwork2( String networkId,
			NetworkQueryParameters queryParameters)
			throws IllegalArgumentException, NdexException;
*/
	/**************************************************************************
	 * Removes a member from a network.
	 * 
	 * @param networkId
	 *            The network ID.
	 * @param userId
	 *            The ID of the member to remove.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws ObjectNotFoundException
	 *             The network or member doesn't exist.
	 * @throws SecurityException
	 *             The user doesn't have access to change members.
	 * @throws NdexException
	 *             Failed to query the database.
	 **************************************************************************/
	public abstract void removeMember(String userId, String networkId)
			throws IllegalArgumentException, ObjectNotFoundException,
			SecurityException, NdexException;

	/**************************************************************************
	 * Changes a member's permissions to a network.
	 * 
	 * @param networkId
	 *            The network ID.
	 * @param networkMember
	 *            The member being updated.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws ObjectNotFoundException
	 *             The network or member doesn't exist.
	 * @throws SecurityException
	 *             The user doesn't have access to change members.
	 * @throws NdexException
	 *             Failed to query the database.
	 **************************************************************************/
	public abstract void updateMember( String userId, String networkId, Membership networkMember)
			throws IllegalArgumentException, ObjectNotFoundException,
			SecurityException, NdexException;

	/**************************************************************************
	 * Updates a network.
	 * 
	 * @param updatedNetwork
	 *            The updated network information.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws SecurityException
	 *             The user doesn't have permissions to update the network.
	 * @throws NdexException
	 *             Failed to update the network in the database.
	 **************************************************************************/
	public abstract void updateNetwork( String userId, Network updatedNetwork)
			throws IllegalArgumentException, SecurityException, NdexException;

	/**************************************************************************
	 * Saves an uploaded network file. Determines the type of file uploaded,
	 * saves the file, and creates a task.
	 * 
	 * @param uploadedNetwork
	 *            The uploaded network file.
	 * @throws IllegalArgumentException
	 *             Bad input.
	 * @throws NdexException
	 *             Failed to parse the file, or create the network in the
	 *             database.
	 **************************************************************************/
	public abstract void uploadNetwork(String userId, UploadedFile uploadedNetwork)
			throws IllegalArgumentException, SecurityException, NdexException;

	
}