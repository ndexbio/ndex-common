package org.ndexbio.common.models.dao.orientdb;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ndexbio.common.access.NetworkAOrientDBDAO;
import org.ndexbio.common.exceptions.DuplicateObjectException;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.helpers.Configuration;
import org.ndexbio.common.helpers.IdConverter;
import org.ndexbio.common.models.dao.NetworkDAO;
import org.ndexbio.common.models.dao.orientdb.helper.EquivalenceFinder;
import org.ndexbio.common.models.dao.orientdb.helper.IdEquivalenceFinder;
import org.ndexbio.common.models.dao.orientdb.helper.NetworkUtility;
import org.ndexbio.common.models.data.IBaseTerm;
import org.ndexbio.common.models.data.ICitation;
import org.ndexbio.common.models.data.IEdge;
import org.ndexbio.common.models.data.IFunctionTerm;
import org.ndexbio.common.models.data.INamespace;
import org.ndexbio.common.models.data.INetwork;
import org.ndexbio.common.models.data.INetworkMembership;
import org.ndexbio.common.models.data.INode;
import org.ndexbio.common.models.data.IReifiedEdgeTerm;
import org.ndexbio.common.models.data.ISupport;
import org.ndexbio.common.models.data.ITask;
import org.ndexbio.common.models.data.ITerm;
import org.ndexbio.common.models.data.IUser;
import org.ndexbio.common.models.object.BaseTerm;
import org.ndexbio.common.models.object.Citation;
import org.ndexbio.common.models.object.Edge;
import org.ndexbio.common.models.object.FunctionTerm;
import org.ndexbio.common.models.object.Membership;
import org.ndexbio.common.models.object.MetaParameter;
import org.ndexbio.common.models.object.Namespace;
import org.ndexbio.common.models.object.Network;
import org.ndexbio.common.models.object.NetworkQueryParameters;
import org.ndexbio.common.models.object.Node;
import org.ndexbio.common.models.object.Permissions;
import org.ndexbio.common.models.object.Priority;
import org.ndexbio.common.models.object.ReifiedEdgeTerm;
import org.ndexbio.common.models.object.SearchParameters;
import org.ndexbio.common.models.object.Status;
import org.ndexbio.common.models.object.Support;
import org.ndexbio.common.models.object.TaskType;
import org.ndexbio.common.models.object.Term;
import org.ndexbio.common.models.object.UploadedFile;
import org.ndexbio.common.models.object.User;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.impls.orient.OrientElement;
import com.tinkerpop.frames.VertexFrame;

public class NetworkOrientdbDAO extends OrientdbDAO implements NetworkDAO {

	private static final Logger logger = Logger.getLogger(NetworkOrientdbDAO.class.getName());
	
	private NetworkOrientdbDAO() { super();}
	
	static NetworkOrientdbDAO createInstance() { return new NetworkOrientdbDAO() ; }

	@Override
	public Collection<String> autoSuggestTerms(String userId, String networkId,
			String partialTerm) throws IllegalArgumentException, NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId),
				"A user id is required ");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId),
				"A network id is required ");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(partialTerm)
				&& partialTerm.length() > 2,
				"The partial term is missing or invalid ");

		try {
			setupDatabase();

			final INetwork network = _orientDbGraph.getVertex(
					IdConverter.toRid(networkId), INetwork.class);
			if (network == null)
				return null;

			final List<String> foundTerms = Lists.newArrayList();
			for (final ITerm networkTerm : network.getTerms()) {
					if (networkTerm instanceof IBaseTerm) {
						if (((IBaseTerm) networkTerm).getName().toLowerCase()
								.startsWith(partialTerm.toLowerCase())) {
							foundTerms.add(((IBaseTerm) networkTerm).getName());

							if (foundTerms.size() == 20)
								return foundTerms;
						}
					}
			}

			return foundTerms;
		} catch (Exception e) {
			logger.severe("Failed to retrieve auto-suggest data for: "
					+ partialTerm + "." + e.getMessage());
			throw new NdexException(
					"Failed to retrieve auto-suggest data for: " + partialTerm
							+ ".");
		} finally {
			teardownDatabase();
		}
	}

	@Override
	public Network createNetwork(String userId, Network newNetwork)
			throws IllegalArgumentException, DuplicateObjectException,
			NdexException {
		Preconditions.checkArgument(null != newNetwork,
				"A network object is required");
		Preconditions.checkArgument(
				!Strings.isNullOrEmpty(newNetwork.getName()),
				"A network name is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId),
				"A user id is required");

		try {
			
			
			setupDatabase();
			final IUser networkOwner = this.findIuserById(userId);
			checkForExistingNetwork(newNetwork, networkOwner);

			final Map<String, VertexFrame> networkIndex = Maps.newHashMap();

			final INetwork network = _orientDbGraph.addVertex("class:network",
					INetwork.class);
			network.setIsComplete(true);
			network.setIsLocked(false);
			network.setIsPublic(newNetwork.getIsPublic());
			network.setName(newNetwork.getName());
			network.setMetadata(newNetwork.getMetadata());
			network.setMetaterms(new HashMap<String, IBaseTerm>());
			network.setNdexEdgeCount(newNetwork.getEdges().size());
			network.setNdexNodeCount(newNetwork.getNodes().size());

			createNetworkMembers(newNetwork, networkOwner, network);

			// Namespaces must be created first since they can be referenced by
			// terms.
			createNamespaces(network, newNetwork, networkIndex);
			logger.info(networkIndex.values().size()
					+ " entries in map after Namespaces");

			// Terms must be created second since they reference other terms
			// and are also referenced by nodes/edges.
			Map<ReifiedEdgeTerm, IReifiedEdgeTerm> newReifiedEdgeTermMap = createTerms(
					network, newNetwork, networkIndex);
			logger.info(networkIndex.values().size()
					+ " entries in map after Terms");

			// Citations must be created next as they're
			// referenced by supports and edges.
			createCitations(network, newNetwork, networkIndex);
			logger.info(networkIndex.values().size()
					+ " entries in map after Citations");

			// Supports and Nodes must be created next as they're
			// referenced by edges.
			createSupports(network, newNetwork, networkIndex);
			logger.info(networkIndex.values().size()
					+ " entries in map after Supports");

			createNodes(network, newNetwork, networkIndex);
			logger.info(networkIndex.values().size()
					+ " entries in map after Nodes");

			// Finally, we create edges
			createEdges(network, newNetwork, networkIndex);
			logger.info(networkIndex.values().size()
					+ " entries in map after Edges");

			// Finally, we populate reifiedEdgeTerms, now that the edges exist
			for (final Entry<ReifiedEdgeTerm, IReifiedEdgeTerm> entry : newReifiedEdgeTermMap
					.entrySet()) {
				populateReifiedEdgeTerm(entry.getKey(), entry.getValue(),
						networkIndex);
			}

			return new Network(network);

		} finally {
			teardownDatabase();
		}
	}

	@Override
	public Network addNetwork(String userId, String networkId,
			String equivalenceMethod, Network sourceNetwork)
			throws IllegalArgumentException, DuplicateObjectException,
			NdexException {
		Preconditions.checkArgument(null != sourceNetwork,
				"A source network structure is required");
		final ORID networkRid = IdConverter.toRid(networkId);

		try {
			setupDatabase();

			final INetwork targetNetwork = _orientDbGraph.getVertex(networkRid,
					INetwork.class);
			if (targetNetwork == null)
				throw new ObjectNotFoundException("Network", networkId);
			else if (!hasPermission(userId, new Network(targetNetwork),
					Permissions.ADMIN))
				throw new SecurityException(
						"Insufficient privileges to add content to this network.");

			final Map<String, VertexFrame> networkIndex = Maps.newHashMap();

			final EquivalenceFinder equivalenceFinder = getEquivalenceFinder(
					equivalenceMethod, targetNetwork, networkIndex);

			// Namespaces not found in target must be created
			// first since they can be referenced by terms.
			createNamespaces(sourceNetwork, equivalenceFinder);

			logger.info(networkIndex.values().size()
					+ " entries in map after Namespaces");

			// Terms not found in target are then created.
			// They can reference other terms
			// and are also referenced by nodes and edges.
			Map<ReifiedEdgeTerm, IReifiedEdgeTerm> newReifiedEdgeTermMap = createTerms(
					sourceNetwork, equivalenceFinder);

			logger.info(networkIndex.values().size()
					+ " entries in map after Terms");

			// Citations not found in target are then created
			// They can be referenced by supports and edges.
			createCitations(sourceNetwork, equivalenceFinder);

			logger.info(networkIndex.values().size()
					+ " entries in map after Citations");

			// Supports not found in target are then created
			// They can be referenced by edges.
			createSupports(sourceNetwork, equivalenceFinder);

			logger.info(networkIndex.values().size()
					+ " entries in map after Supports");

			// Nodes not found in target are then created
			// They can be referenced by edges.
			createNodes(sourceNetwork, equivalenceFinder);

			logger.info(networkIndex.values().size()
					+ " entries in map after Nodes");

			// edges not found in target are created
			createEdges(sourceNetwork, equivalenceFinder);

			// Finally, we populate reifiedEdgeTerms, now that the edges exist
			for (final Entry<ReifiedEdgeTerm, IReifiedEdgeTerm> entry : newReifiedEdgeTermMap
					.entrySet()) {
				populateReifiedEdgeTerm(entry.getKey(), entry.getValue(),
						networkIndex);
			}

			logger.info(networkIndex.values().size()
					+ " entries in map after Edges");

			updateNetworkStatistics(targetNetwork);
			return new Network(targetNetwork);

		} finally {
			teardownDatabase();
		}
	}

	@Override
	public void deleteNetwork(String userId, String networkId)
			throws IllegalArgumentException, ObjectNotFoundException,
			NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId),
				"A user id is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId),
				"A network id is required");

		final ORID networkRid = IdConverter.toRid(networkId);

		try {
			setupDatabase();

			final INetwork networkToDelete = _orientDbGraph.getVertex(
					networkRid, INetwork.class);
			if (networkToDelete == null)
				throw new ObjectNotFoundException("Network", networkId);
			else if (!hasPermission(userId, new Network(networkToDelete),
					Permissions.ADMIN))
				throw new SecurityException(
						"Insufficient privileges to delete the network.");

			final List<ODocument> adminCount = _ndexDatabase
					.query(new OSQLSynchQuery<Integer>(
							"SELECT COUNT(@RID) FROM Membership WHERE in_members = "
									+ networkRid + " AND permissions = 'ADMIN'"));
			if (adminCount == null || adminCount.isEmpty())
				throw new NdexException("Unable to count ADMIN members.");
			else if ((long) adminCount.get(0).field("COUNT") > 1)
				throw new NdexException(
						"Cannot delete a network that contains other ADMIN members.");

			for (INetworkMembership networkMembership : networkToDelete
					.getMembers())
				_orientDbGraph.removeVertex(networkMembership.asVertex());

			final List<ODocument> networkChildren = _ndexDatabase
					.query(new OSQLSynchQuery<Object>(
							"SELECT @RID FROM (TRAVERSE * FROM "
									+ networkRid
									+ " WHILE @class <> 'network' AND @class <> 'user' AND @class <> 'group')"));
			for (ODocument networkChild : networkChildren) {
				final OrientElement element = _orientDbGraph.getBaseGraph()
						.getElement(networkChild.field("rid", OType.LINK));
				if (element != null)
					element.remove();
			}

			_orientDbGraph.removeVertex(networkToDelete.asVertex());
			_orientDbGraph.getBaseGraph().commit();
		} catch (SecurityException | NdexException ne) {
			System.out.println(ne.getMessage());
			throw ne;
		}finally {
			teardownDatabase();
		}

	}

	@Override
	public List<Network> findNetworks(String userId,
			SearchParameters searchParameters, String searchOperator)
			throws IllegalArgumentException, NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId),
				"A user id is required");
		Preconditions.checkArgument(null != searchParameters,
				"Search Parameters are required");
		Preconditions.checkArgument(null != searchParameters.getSearchString()
				|| !Strings.isNullOrEmpty(searchParameters.getSearchString()),
				"No search string was specified.");

		searchParameters.setSearchString(searchParameters.getSearchString()
				.trim());

		final List<Network> foundNetworks = new ArrayList<Network>();
		

		try {
			
			setupDatabase();
			final String query = buildSearchQuery(searchParameters, searchOperator,userId);
			final List<ODocument> networks = _ndexDatabase
					.query(new OSQLSynchQuery<ODocument>(query));
			for (final ODocument network : networks)
				foundNetworks.add(new Network(_orientDbGraph.getVertex(network,
						INetwork.class)));

			return foundNetworks;
		} catch (Exception e) {
			logger.severe("Failed to search networks. " +  e.getMessage());
			throw new NdexException("Failed to search networks.");
		} finally {
			teardownDatabase();
		}
	}

	@Override
	public Network getNetwork(String userId, String networkId)
			throws IllegalArgumentException, SecurityException, NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId), "A user id is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId),
				"No network ID was specified.");

		try {
			
			setupDatabase();
			IUser iuser = this.findIuserById(userId);
			final INetwork network = _orientDbGraph.getVertex(
					IdConverter.toRid(networkId), INetwork.class);
			if (network == null)
				return null;
			else if (!network.getIsPublic() && null != iuser) {
				User user = new User(iuser,true);
				for (Membership userMembership : user
						.getNetworks()) {
					if (userMembership.equals(networkId))
						return new Network(network);
				}

				throw new SecurityException(
						"You do not have access to that network.");
			} else
				return new Network(network);
		} finally {
			teardownDatabase();
		}
	}

	@Override
	public Network getEdges(String userId, String networkId, int skip, int top)
			throws IllegalArgumentException, NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId), "A user id is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), "A network id is required");
		top = Math.max(1, top);
		
		try {
			setupDatabase();

			final INetwork network = _orientDbGraph.getVertex(
					IdConverter.toRid(networkId), INetwork.class);
			if (network == null)
				throw new ObjectNotFoundException("Network", networkId);

			int counter = 0;
			final Set<IEdge> foundIEdges = new TreeSet<IEdge>();
			final int startIndex = skip * top;

			for (final IEdge networkEdge : network.getNdexEdges()) {
				if (counter >= startIndex)
					foundIEdges.add(networkEdge);

				counter++;
				if (counter >= startIndex + top)
					break;
			}
			return getNetworkBasedOnFoundEdges(foundIEdges, network);
		} catch (ObjectNotFoundException onfe) {
			throw onfe;
		} catch (Exception e) {
			logger.severe("Failed to query network: " + networkId + ". " + e.getMessage());
			throw new NdexException(e.getMessage());
		} finally {
			teardownDatabase();
		}
	}

	@Override
	public Network getNetworkByNonEdgeNodes(String userId, String networkId,
			int skip, int top) throws IllegalArgumentException, NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId), "A user id is required" );
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), "A network id is required" );
		top = Math.max(1,top);

		try {
			setupDatabase();

			final INetwork network = _orientDbGraph.getVertex(
					IdConverter.toRid(networkId), INetwork.class);
			if (network == null)
				throw new ObjectNotFoundException("Network", networkId);
			
			final int startIndex = skip * top;

			final String nodeQuery = "SELECT FROM (TRAVERSE out_networkNodes from " 
					+ network.asVertex().getId() 
					+ " while $depth < 2) where $depth > 0 and out_edgeSubject is null and in_edgeObject is null"
					+ " SKIP " + startIndex + " LIMIT " + top;
			final List<ODocument> foundNodes = _ndexDatabase
					.query(new OSQLSynchQuery<ODocument>(nodeQuery));

			List<INode> requiredNodes = new ArrayList<INode>();
			for (ODocument n : foundNodes){
				final INode iNode = _orientDbGraph.getVertex(n, INode.class);
				requiredNodes.add(iNode);				
			}
			return getNetworkBasedOnNonEdgeNodes(requiredNodes, network);
		} catch (ObjectNotFoundException onfe) {
			throw onfe;
		} catch (Exception e) {
			logger.severe("Failed to query network: " + networkId + ". " + e.getMessage());
			throw new NdexException(e.getMessage());
		} finally {
			teardownDatabase();
		}
	}

	@Override
	public Network getEdgesByCitations(String userId, String networkId,
			int skip, int top, String[] citations)
			throws IllegalArgumentException, NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId), "A user id is required" );
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), "A network id is required" );
		top = Math.max(1,top);
		
		try {
			setupDatabase();

			final INetwork network = _orientDbGraph.getVertex(
					IdConverter.toRid(networkId), INetwork.class);
			if (network == null)
				throw new ObjectNotFoundException("Network", networkId);
			
			// Check that all citations are elements of the network
			final String citationIdCsv = IdConverter.toRidCsv(citations);
			final String citationQuery = "SELECT FROM (TRAVERSE out_networkCitations from " + network.asVertex().getId()
					+ " ) WHERE @RID in [ " + citationIdCsv + " ]";
			final List<ODocument> foundCitations = _ndexDatabase
					.query(new OSQLSynchQuery<ODocument>(citationQuery));
			if (null == foundCitations || foundCitations.size() != citations.length)
				throw new ObjectNotFoundException("One or more citations with ids in [" + citationIdCsv + "] was not found in network " + networkId);
			
			List<ICitation> requiredCitations = new ArrayList<ICitation>();
			for (ODocument cit : foundCitations){
				final ICitation iCitation = _orientDbGraph.getVertex(
						cit, ICitation.class);
				requiredCitations.add(iCitation);
				
			}
			return getNetworkBasedOnCitations(requiredCitations, network);

			
		} catch (ObjectNotFoundException onfe) {
			throw onfe;
		} catch (Exception e) {
			logger.severe("Failed to query network by citations : " + networkId + ". " + e.getMessage());
			throw new NdexException(e.getMessage());
		} finally {
			teardownDatabase();
		}
	}

	@Override
	public List<BaseTerm> getTermsInNamespaces(String userId, String networkId,
			String[] namespaces) throws IllegalArgumentException, NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId), "A user id is required" );
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), "A network id is required" );

		final List<BaseTerm> foundBaseTerms = new ArrayList<BaseTerm>();

		String joinedNamespaces = "";
		for (final String namespace : namespaces)
			joinedNamespaces += "'" + namespace + "',";

		joinedNamespaces = joinedNamespaces.substring(0,
				joinedNamespaces.length() - 1);

		final ORID networkRid = IdConverter.toRid(networkId);
		final String query = "SELECT FROM Namespace\n"
				+ "WHERE in_networkNamespaces = " + networkRid + "\n"
				+ "  AND prefix IN [" + joinedNamespaces + "]\n"
				+ "ORDER BY prefix";

		try {
			setupDatabase();

			final INetwork network = _orientDbGraph.getVertex(networkRid,
					INetwork.class);
			if (network == null)
				throw new ObjectNotFoundException("Network", networkId);

			final List<ODocument> namespacesFound = _ndexDatabase
					.query(new OSQLSynchQuery<ODocument>(query));
			for (final ODocument namespace : namespacesFound) {
				ORID namespaceId = namespace.getIdentity();
				final String termQuery = "SElECT FROM ( TRAVERSE in_baseTermNamespace from "
						+ namespaceId + " ) WHERE termType = 'Base' ";
				
				final List<ODocument> baseTermsFound = _ndexDatabase
						.query(new OSQLSynchQuery<ODocument>(termQuery));
				for (final ODocument baseTerm : baseTermsFound) {
					final IBaseTerm iBaseTerm = _orientDbGraph.getVertex(
							baseTerm, IBaseTerm.class);
					final BaseTerm bt = new BaseTerm(iBaseTerm);
					foundBaseTerms.add(bt);
				}
			}
			return foundBaseTerms;
		} catch (ObjectNotFoundException onfe) {
			throw onfe;
		} catch (Exception e) {
			logger.severe("Failed to query network: " + networkId + ". " + e.getMessage());
			throw new NdexException(e.getMessage());
		} finally {
			teardownDatabase();
		}
	}

	@Override
	public Iterable<BaseTerm> getIntersectingTerms(String userId,
			String networkId, String[] terms) throws IllegalArgumentException,
			NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId), "A user id is required" );
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), "A network id is required" );

		final List<BaseTerm> foundTerms = new ArrayList<BaseTerm>();

		String joinedTerms = "";
		for (final String baseTerm : terms)
			joinedTerms += "'" + baseTerm + "',";

		joinedTerms = joinedTerms.substring(0, joinedTerms.length() - 2);

		final ORID networkRid = IdConverter.toRid(networkId);
		final String query = "SELECT FROM BaseTerm\n"
				+ "WHERE in_networkTerms = " + networkRid + "\n"
				+ "  AND name IN [" + joinedTerms + "]\n" + "ORDER BY name";

		try {
			setupDatabase();

			final INetwork network = _orientDbGraph.getVertex(networkRid,
					INetwork.class);
			if (network == null)
				throw new ObjectNotFoundException("Network", networkId);

			final List<ODocument> baseTerms = _ndexDatabase
					.query(new OSQLSynchQuery<ODocument>(query));
			for (final ODocument baseTerm : baseTerms)
				foundTerms.add(new BaseTerm(_orientDbGraph.getVertex(baseTerm,
						IBaseTerm.class)));

			return foundTerms;
		} catch (ObjectNotFoundException onfe) {
			throw onfe;
		} catch (Exception e) {
			logger.severe("Failed to query network: " + networkId + ". " + e.getMessage());
			throw new NdexException(e.getMessage());
		} finally {
			teardownDatabase();
		}
	}

	@Override
	public Iterable<Namespace> getNamespaces( String networkId,
			int skip, int top) throws IllegalArgumentException, NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), "A network id is required" );
		top = Math.max(1, top);

		final List<Namespace> foundNamespaces = new ArrayList<Namespace>();

		final int startIndex = skip * top;
		final ORID networkRid = IdConverter.toRid(networkId);
		final String query = "SELECT FROM Namespace\n"
				+ "WHERE in_networkNamespaces = " + networkRid + "\n"
				+ "ORDER BY prefix\n" + "SKIP " + startIndex + "\n" + "LIMIT "
				+ top;

		try {
			setupDatabase();

			final INetwork network = _orientDbGraph.getVertex(networkRid,
					INetwork.class);
			if (network == null)
				throw new ObjectNotFoundException("Network", networkId);

			final List<ODocument> namespaces = _ndexDatabase
					.query(new OSQLSynchQuery<ODocument>(query));
			for (final ODocument namespace : namespaces)
				foundNamespaces.add(new Namespace(_orientDbGraph.getVertex(
						namespace, INamespace.class)));

			return foundNamespaces;
		} catch (ObjectNotFoundException onfe) {
			throw onfe;
		} catch (Exception e) {
			logger.severe("Failed to query network: " + networkId + ". " + e.getMessage());
			throw new NdexException(e.getMessage());
		} finally {
			teardownDatabase();
		}
	}

	@Override
	public Iterable<BaseTerm> getTerms(String networkId,
			int skip, int top) throws IllegalArgumentException, NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), "A network id is required" );
		top = Math.max(1, top);

		final List<BaseTerm> foundTerms = new ArrayList<BaseTerm>();

		final int startIndex = skip * top;
		final ORID networkRid = IdConverter.toRid(networkId);
		final String query = "SELECT FROM BaseTerm\n"
				+ "WHERE in_networkTerms = " + networkRid + "\n"
				+ "ORDER BY name\n" + "SKIP " + startIndex + "\n" + "LIMIT "
				+ top;

		try {
			setupDatabase();

			final INetwork network = _orientDbGraph.getVertex(networkRid,
					INetwork.class);
			if (network == null)
				throw new ObjectNotFoundException("Network", networkId);

			final List<ODocument> baseTerms = _ndexDatabase
					.query(new OSQLSynchQuery<ODocument>(query));
			for (final ODocument baseTerm : baseTerms)
				foundTerms.add(new BaseTerm(_orientDbGraph.getVertex(baseTerm,
						IBaseTerm.class)));

			return foundTerms;
		} catch (ObjectNotFoundException onfe) {
			throw onfe;
		} catch (Exception e) {
			logger.severe("Failed to query network: " + networkId + ". " + e.getMessage());
			throw new NdexException(e.getMessage());
		} finally {
			teardownDatabase();
		}
	}

	@Override
	public List<Citation> getCitations(String networkId,
			int skip, int top) throws IllegalArgumentException, NdexException {
		
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), "A network id is required" );
		top = Math.max(1, top);
			final List<Citation> foundCitations = new ArrayList<Citation>();

			final int startIndex = skip * top;
			final ORID networkRid = IdConverter.toRid(networkId);
			final String citationQuery = "SELECT FROM (TRAVERSE out_networkCitations from " + networkRid
					+ " while $depth < 2) WHERE @class = 'citation' SKIP " + startIndex + "\n" + "LIMIT "
					+ top;

			try {
				setupDatabase();

				final INetwork network = _orientDbGraph.getVertex(networkRid,
						INetwork.class);
				if (network == null)
					throw new ObjectNotFoundException("Network", networkId);

				final List<ODocument> citations = _ndexDatabase
						.query(new OSQLSynchQuery<ODocument>(citationQuery));
				for (final ODocument citation : citations)
					foundCitations.add(new Citation(_orientDbGraph.getVertex(citation,
							ICitation.class)));

				return foundCitations;
			} catch (ObjectNotFoundException onfe) {
				throw onfe;
			} catch (Exception e) {
				logger.severe("Failed to query network: " + networkId + ". " + e.getMessage());
				throw new NdexException(e.getMessage());
			} finally {
				teardownDatabase();
			}
	}

	@Override
	public Network queryNetwork( String networkId,
			NetworkQueryParameters queryParameters)
			throws IllegalArgumentException, NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), "A network id is required" );

		NetworkAOrientDBDAO dao = NetworkAOrientDBDAO.getInstance();
		Network network = dao.queryForSubnetwork(null, networkId, queryParameters, 0,1000);
	
		return network;
	}

/*	@Override
	public Network queryNetwork2(String networkId,
			NetworkQueryParameters queryParameters)
			throws IllegalArgumentException, NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), "A network id is required" );
		
		NetworkAOrientDBDAO dao = NetworkAOrientDBDAO.getInstance();
		Network network = dao.queryForSubnetwork(null, networkId, queryParameters, 0,0);
	
		return network;
	} */

	@Override
	public void removeMember(String userId, String networkId)
			throws IllegalArgumentException, ObjectNotFoundException,
			SecurityException, NdexException {
		
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId),
				"A network id is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId),
				"A user id is required");

		try {
			setupDatabase();

			final ORID networkRid = IdConverter.toRid(networkId);
			final INetwork network = _orientDbGraph.getVertex(networkRid,
					INetwork.class);

			if (network == null)
				throw new ObjectNotFoundException("Network", networkId);
			else if (!hasPermission(userId, new Network(network), Permissions.ADMIN))
				throw new SecurityException("Access denied.");

			final IUser user = _orientDbGraph.getVertex(
					IdConverter.toRid(userId), IUser.class);
			if (user == null)
				throw new ObjectNotFoundException("User", userId);

			for (INetworkMembership networkMember : network.getMembers()) {
				String memberId = IdConverter.toJid((ORID) networkMember
						.getMember().asVertex().getId());
				if (memberId.equals(userId)) {
					if (countAdminMembers(networkRid) < 2)
						throw new SecurityException(
								"Cannot remove the only ADMIN member.");

					network.removeMember(networkMember);
					user.removeNetwork(networkMember);
					_orientDbGraph.getBaseGraph().commit();
					return;
				}
			}
		} catch (ObjectNotFoundException | SecurityException ne) {
			throw ne;
		} catch (Exception e) {
			if (e.getMessage().indexOf("cluster: null") > -1) {
				throw new ObjectNotFoundException("Network", networkId);
			}

			logger.severe("Failed to remove member. " + e.getMessage());

			throw new NdexException("Failed to remove member.");
		} finally {
			teardownDatabase();
		}

	}

	@Override
	public void updateMember( String userId, String networkId,
			Membership networkMember) throws IllegalArgumentException,
			ObjectNotFoundException, SecurityException, NdexException {
		
			Preconditions.checkArgument(!Strings.isNullOrEmpty(userId),
					"A user id is required");
			Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId),
					"A network id is required");
			Preconditions.checkArgument(null != networkMember,
					"A network member is required");
			Preconditions.checkArgument(
					!Strings.isNullOrEmpty(networkMember.getResourceId()),
					"The network member must have a resource id");

		try {
			setupDatabase();

			final ORID networkRid = IdConverter.toRid(networkId);
			final INetwork network = _orientDbGraph.getVertex(networkRid,
					INetwork.class);

			if (network == null)
				throw new ObjectNotFoundException("Network", networkId);
			else if (!hasPermission(userId, new Network(network), Permissions.ADMIN))
				throw new SecurityException("Access denied.");

			final IUser user = _orientDbGraph.getVertex(
					IdConverter.toRid(networkMember.getResourceId()),
					IUser.class);
			if (user == null)
				throw new ObjectNotFoundException("User",
						networkMember.getResourceId());

			for (INetworkMembership networkMembership : network.getMembers()) {
				String memberId = IdConverter.toJid((ORID) networkMembership
						.getMember().asVertex().getId());
				if (memberId.equals(networkMember.getResourceId())) {
					if (countAdminMembers(networkRid) < 2)
						throw new SecurityException(
								"Cannot change the permissions on the only ADMIN member.");

					networkMembership.setPermissions(networkMember
							.getPermissions());
					_orientDbGraph.getBaseGraph().commit();
					return;
				}
			}
		} catch (ObjectNotFoundException | SecurityException ne) {
			throw ne;
		} catch (Exception e) {
			if (e.getMessage().indexOf("cluster: null") > -1)
				throw new ObjectNotFoundException("Network", networkId);

			logger.severe(
					"Failed to update member: "
							+ networkMember.getResourceName() + ". "+ e.getMessage());

			throw new NdexException("Failed to update member: "
					+ networkMember.getResourceName() + ".");
		} finally {
			teardownDatabase();
		}

	}

	@Override
	public void updateNetwork( String userId, Network updatedNetwork)
			throws IllegalArgumentException, SecurityException, NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId),  "A user id is required");
		Preconditions.checkArgument(null != updatedNetwork, "A Network is required");

		try {
			setupDatabase();

			final INetwork networkToUpdate = _orientDbGraph.getVertex(
					IdConverter.toRid(updatedNetwork.getId()), INetwork.class);
			if (networkToUpdate == null)
				throw new ObjectNotFoundException("Network",
						updatedNetwork.getId());
			else if (!hasPermission(userId, updatedNetwork, Permissions.WRITE))
				throw new SecurityException("Access denied.");

			if (updatedNetwork.getDescription() != null
					&& !updatedNetwork.getDescription().equals(
							networkToUpdate.getDescription()))
				networkToUpdate.setDescription(updatedNetwork.getDescription());

			if (updatedNetwork.getIsLocked() != networkToUpdate.getIsLocked())
				networkToUpdate.setIsLocked(updatedNetwork.getIsLocked());

			if (updatedNetwork.getIsPublic() != networkToUpdate.getIsPublic())
				networkToUpdate.setIsPublic(updatedNetwork.getIsPublic());

			if (updatedNetwork.getName() != null
					&& !updatedNetwork.getName().equals(
							networkToUpdate.getName()))
				networkToUpdate.setName(updatedNetwork.getName());

			if (updatedNetwork.getMetadata() != null
					&& !updatedNetwork.getMetadata().equals(
							networkToUpdate.getMetadata()))
				networkToUpdate.setMetadata(updatedNetwork.getMetadata());

		} catch (SecurityException | ObjectNotFoundException onfe) {
			throw onfe;
		} catch (Exception e) {
			if (e.getMessage().indexOf("cluster: null") > -1)
				throw new ObjectNotFoundException("Network",
						updatedNetwork.getId());
			logger.severe(
					"Failed to update network: " + updatedNetwork.getName()
							+ ". " + e.getMessage());
			throw new NdexException("Failed to update the network.");
		} finally {
			teardownDatabase();
		}

	}

	@Override
	public void uploadNetwork(String userId, UploadedFile uploadedNetwork)
			throws IllegalArgumentException, SecurityException, NdexException {
		try {
			Preconditions
					.checkNotNull(uploadedNetwork, "A network is required");
			Preconditions.checkState(
					!Strings.isNullOrEmpty(uploadedNetwork.getFilename()),
					"A file name containg the network data is required");
			Preconditions.checkNotNull(uploadedNetwork.getFileData(),
					"Network file data is required");
			Preconditions.checkState(uploadedNetwork.getFileData().length > 0,
					"The file data is empty");
		} catch (Exception e1) {
			throw new IllegalArgumentException(e1);
		}

		final File uploadedNetworkPath = new File(Configuration.getInstance()
				.getProperty("Uploaded-Networks-Path"));
		if (!uploadedNetworkPath.exists())
			uploadedNetworkPath.mkdir();

		final File uploadedNetworkFile = new File(
				uploadedNetworkPath.getAbsolutePath() + "/"
						+ uploadedNetwork.getFilename());

		try {
			if (!uploadedNetworkFile.exists())
				uploadedNetworkFile.createNewFile();

			final FileOutputStream saveNetworkFile = new FileOutputStream(
					uploadedNetworkFile);
			saveNetworkFile.write(uploadedNetwork.getFileData());
			saveNetworkFile.flush();
			saveNetworkFile.close();

			

			
			setupDatabase();
			final IUser taskOwner = this.findIuserById(userId);
			final String fn = uploadedNetwork.getFilename().toLowerCase();

			if (fn.endsWith(".sif") || fn.endsWith(".xbel")
					|| fn.endsWith(".xgmml") || fn.endsWith(".xls")
					|| fn.endsWith(".xlsx")) {
				ITask processNetworkTask = _orientDbGraph.addVertex(
						"class:task", ITask.class);
				processNetworkTask.setDescription("Process uploaded network");
				processNetworkTask.setType(TaskType.PROCESS_UPLOADED_NETWORK);
				processNetworkTask.setOwner(taskOwner);
				processNetworkTask.setPriority(Priority.LOW);
				processNetworkTask.setProgress(0);
				processNetworkTask.setResource(uploadedNetworkFile
						.getAbsolutePath());
				processNetworkTask.setStartTime(new Date());
				processNetworkTask.setStatus(Status.QUEUED);

				_orientDbGraph.getBaseGraph().commit();
			} else {
				uploadedNetworkFile.delete();
				throw new IllegalArgumentException(
						"The uploaded file type is not supported; must be Excel, XGMML, SIF, OR XBEL.");
			}
		} catch (IllegalArgumentException iae) {
			throw iae;
		} catch (Exception e) {
			logger.severe("Failed to process uploaded network: "
					+ uploadedNetwork.getFilename() + ". " + e.getMessage());

			throw new NdexException(e.getMessage());
		}
	}

	// private methods
	private void createNetworkMembers(final Network newNetwork,
			final IUser networkOwner, final INetwork network) {
		if (newNetwork.getMembers() == null
				|| newNetwork.getMembers().size() == 0) {
			final INetworkMembership membership = _orientDbGraph.addVertex(
					"class:networkMembership", INetworkMembership.class);
			membership.setPermissions(Permissions.ADMIN);
			membership.setMember(networkOwner);
			membership.setNetwork(network);
		} else {
			for (final Membership member : newNetwork.getMembers()) {
				final IUser networkMember = _orientDbGraph.getVertex(
						IdConverter.toRid(member.getResourceId()), IUser.class);

				final INetworkMembership membership = _orientDbGraph.addVertex(
						"class:networkMembership", INetworkMembership.class);
				membership.setPermissions(member.getPermissions());
				membership.setMember(networkMember);
				membership.setNetwork(network);
			}
		}
	}

	private void checkForExistingNetwork(final Network newNetwork,
			final IUser networkOwner) throws DuplicateObjectException {
		final List<ODocument> existingNetworks = _ndexDatabase
				.query(new OSQLSynchQuery<Object>(
						"SELECT @RID FROM Network WHERE out_networkMemberships.in_accountNetworks.username = '"
								+ networkOwner.getUsername()
								+ "' AND name = '"
								+ newNetwork.getName() + "'"));
		if (!existingNetworks.isEmpty())
			throw new DuplicateObjectException(
					"You already have a network titled: "
							+ newNetwork.getName());
	}

	private EquivalenceFinder getEquivalenceFinder(String equivalenceMethod,
			INetwork target, Map<String, VertexFrame> networkIndex) {
		if ("JDEX_ID".equals(equivalenceMethod))
			return new IdEquivalenceFinder(target, networkIndex, _ndexDatabase,
					_orientDbGraph);
		throw new IllegalArgumentException("Unknown EquivalenceMethod: "
				+ equivalenceMethod);
	}

	private void updateNetworkStatistics(INetwork network) throws NdexException {
		String query = "SELECT count(*) from (traverse out_networkEdges from "
				+ network.asVertex().getId()
				+ " while $depth < 2) where $depth > 0  ";
		final List<ODocument> edgeResults = _ndexDatabase
				.query(new OSQLSynchQuery<ODocument>(query));
		final ODocument edgeResult = edgeResults.get(0);
		if (null == edgeResult)
			throw new NdexException(
					"Unexpected null result getting count of edges in network "
							+ network.asVertex().getId());
		int count = NetworkUtility.safeLongToInt((Long) edgeResult
				.field("count"));
		network.setNdexEdgeCount(count);
		logger.info("found " + count + " edges");

		query = "SELECT count(*) from (traverse out_networkNodes from "
				+ network.asVertex().getId()
				+ " while $depth < 2) where $depth > 0  ";
		final List<ODocument> nodeResults = _ndexDatabase
				.query(new OSQLSynchQuery<ODocument>(query));
		final ODocument nodeResult = nodeResults.get(0);
		if (null == nodeResult)
			throw new NdexException(
					"Unexpected null result getting count of nodes in network "
							+ network.asVertex().getId());
		count = NetworkUtility.safeLongToInt((Long) nodeResult.field("count"));
		network.setNdexNodeCount(count);
		logger.info("found " + count + " nodes");
	}

	private List<INode> getNodesFromTerms(List<IBaseTerm> baseTerms,
			boolean includeAliases) {
		List<INode> result = new ArrayList<INode>();
		String termIdCsv = NetworkUtility.joinBaseTermIdsToCsv(baseTerms);

		String traverseEdgeTypes = null;
		if (includeAliases) {
			traverseEdgeTypes = "in_functionTermParameters, in_nodeRepresents, in_nodeRelationshipAliases, in_nodeUnificationAliases";
		} else {
			traverseEdgeTypes = "in_functionTermParameters, in_nodeRepresents";
		}

		final String query = "SELECT FROM (traverse " + traverseEdgeTypes
				+ " from \n[" + termIdCsv + "] \n" + "WHILE $depth < 10) \n"
				+ "WHERE @CLASS='node' ";
		// logger.info("node query: " + query);
		final List<ODocument> nodes = _ndexDatabase
				.query(new OSQLSynchQuery<ODocument>(query));
		for (final ODocument node : nodes)
			result.add(_orientDbGraph.getVertex(node, INode.class));
		return result;
	}

	private List<IBaseTerm> getBaseTermsByNames(INetwork network,
			List<String> baseTermNames) throws NdexException {
		final List<IBaseTerm> foundTerms = new ArrayList<IBaseTerm>();
		String termNameCsv = NetworkUtility.joinStringsToCsv(baseTermNames);

		// select from (traverse out_networkTerms from #24:733
		// while $depth < 2) where @CLASS='baseTerm' and name like "AKT%" limit
		// 10
		final String query = "SELECT FROM (traverse out_networkTerms from "
				+ network.asVertex().getId() + " \n" + "WHILE $depth < 2) \n"
				+ "WHERE @CLASS='baseTerm' AND name IN [" + termNameCsv + "] ";

		final List<ODocument> baseTerms = _ndexDatabase
				.query(new OSQLSynchQuery<ODocument>(query));
		for (final ODocument baseTerm : baseTerms)
			foundTerms.add(_orientDbGraph.getVertex(baseTerm, IBaseTerm.class));

		return foundTerms;
	}

	
	
	private String buildSearchQuery(final SearchParameters searchParameters,
			final String searchOperator, final String userId) {
		String query = "";
		String username = "";
		IUser user = this.findIuserById(userId);
		if (null != user) {
			username = Objects.firstNonNull(user.getUsername(), "");
		}
		
		if (searchParameters.getSearchString().contains("terms:")) {
			// query += parseTermParameters(searchParameters);
			query = "SELECT FROM "
					+ getTermSearchExpression(searchParameters
							.getSearchString());

			if (!Strings.isNullOrEmpty(username)) {
				query += "WHERE isComplete = true\n"
						+ "  AND (isPublic = true"
						+ " OR out_networkMemberships.in_accountNetworks.username = '"
						+ username + "') \n";
			} else
				query += "WHERE isComplete = true AND isPublic = true\n";

		} else {
			// CASE: Name and Description Text Search
			// With option: Metadata Parameter Search
			// Replace all multiple spaces (left by previous parsing) with a
			// single
			// space

			final Pattern metadataRegex = Pattern
					.compile("\\[(.+)\\]([:~=])(\".+\")");
			final ArrayList<MetaParameter> metadataParameters = NetworkUtility
					.parseMetaParameters(searchParameters, metadataRegex);

			final Pattern metatermRegex = Pattern
					.compile("\\<(.+)\\>([:~=])(\".+\")");
			final ArrayList<MetaParameter> metatermParameters = NetworkUtility
					.parseMetaParameters(searchParameters, metatermRegex);

			searchParameters.setSearchString(searchParameters.getSearchString()
					.replace("  ", " ").toLowerCase().trim());

			query = "SELECT FROM Network\n";

			if (!Strings.isNullOrEmpty(username)) {
				query += "WHERE isComplete = true\n"
						+ "  AND (isPublic = true"
						+ " OR out_networkMemberships.in_accountNetworks.username = '"
						+ username + "') \n";
			} else
				query += "WHERE isComplete = true AND isPublic = true\n";

			if (searchParameters.getSearchString().contains("-desc")) {
				searchParameters.getSearchString().replace(" -desc", "");
				query += "  AND name.toLowerCase() LIKE '"
						+ searchParameters.getSearchString() + "%' \n";
			}

			if (searchParameters.getSearchString() != null
					&& !searchParameters.getSearchString().isEmpty()) {
				if (searchOperator.equals("exact-match")) {
					query += "  AND (name.toLowerCase() = '"
							+ searchParameters.getSearchString() + "'"
							+ " OR description.toLowerCase() = '"
							+ searchParameters.getSearchString() + "') \n";
				} else if (searchOperator.equals("contains")) {
					query += "  AND (name.toLowerCase() LIKE '%"
							+ searchParameters.getSearchString() + "%'"
							+ " OR description.toLowerCase() LIKE '%"
							+ searchParameters.getSearchString() + "%') \n";
				} else {
					query += "  AND (name.toLowerCase() LIKE '"
							+ searchParameters.getSearchString() + "%'"
							+ " OR description.toLowerCase() LIKE '"
							+ searchParameters.getSearchString() + "%') \n";
				}
			}

			for (final MetaParameter metadataParameter : metadataParameters)
				query += "  AND metadata['" + metadataParameter.getKey() + "']"
						+ metadataParameter.toString() + " \n";

			for (final MetaParameter metatermParameter : metatermParameters)
				query += "  AND metadata['" + metatermParameter.getKey() + "']"
						+ metatermParameter.toString() + " \n";

			final int startIndex = searchParameters.getSkip()
					* searchParameters.getTop();
			query += "ORDER BY name DESC\n" + "SKIP " + startIndex + "\n"
					+ "LIMIT " + searchParameters.getTop();

		}

		logger.fine(query);
		return query;
	}

	private String getTermSearchExpression(final String searchString) {
		final Pattern termRegex = Pattern.compile("terms:\\{(.+)\\}");
		final Matcher termMatcher = termRegex.matcher(searchString);
		Set<String> termNameStrings = new HashSet<String>();
		Set<String> namespacePrefixStrings = new HashSet<String>();

		if (termMatcher.find()) {
			final String terms[] = termMatcher.group(1).split(",");

			for (final String term : terms) {
				final String namespaceAndTerm[] = term.split(":");
				if (namespaceAndTerm.length != 2)
					throw new IllegalArgumentException(
							"Error parsing terms from: \""
									+ termMatcher.group(0) + "\".");
				termNameStrings.add(namespaceAndTerm[1]);
				namespacePrefixStrings.add(namespaceAndTerm[0]);
			}
			String termNames = NetworkUtility.joinStringsToCsv(termNameStrings);
			String namespacePrefixes = NetworkUtility.joinStringsToCsv(namespacePrefixStrings);
			
			final String searchExpression = " (TRAVERSE out_baseTermNamespace, in_networkNamespaces FROM "
					+ "(SELECT FROM baseTerm where name in ["
					+ termNames
					+ "]) "
					+ "WHILE $depth < 3 or ($depth = 1 and prefix IN ["
					+ namespacePrefixes + "])) ";

			return searchExpression;

		}

		return null;
	}

	/**************************************************************************
	 * Count the number of administrative members in the network.
	 **************************************************************************/
	private long countAdminMembers(final ORID networkRid) throws NdexException {
		final List<ODocument> adminCount = _ndexDatabase
				.query(new OSQLSynchQuery<Integer>(
						"SELECT COUNT(@RID) FROM NetworkMembership WHERE in_userNetworks = "
								+ networkRid + " AND permissions = 'ADMIN'"));
		if (adminCount == null || adminCount.isEmpty())
			throw new NdexException("Unable to count ADMIN members.");

		return (long) adminCount.get(0).field("COUNT");
	}

	private void createNamespaces(final INetwork domainNetwork,
			final Network objectNetwork,
			final Map<String, VertexFrame> networkIndex) {
		for (final Entry<String, Namespace> namespaceEntry : objectNetwork
				.getNamespaces().entrySet()) {
			final Namespace namespace = namespaceEntry.getValue();
			final String jdexId = namespaceEntry.getKey();
			createNamespace(domainNetwork, namespace, jdexId, networkIndex);
		}
	}

	private void createNamespaces(final Network objectNetwork,
			EquivalenceFinder equivalenceFinder) throws NdexException {
		for (final Entry<String, Namespace> namespaceEntry : objectNetwork
				.getNamespaces().entrySet()) {
			final Namespace namespace = namespaceEntry.getValue();
			final String jdexId = namespaceEntry.getKey();
			INamespace ns = equivalenceFinder.getNamespace(namespace, jdexId);
			if (null == ns)
				createNamespace(equivalenceFinder.getTargetNetwork(),
						namespace, jdexId, equivalenceFinder.getNetworkIndex());
		}
	}

	private void createNamespace(final INetwork newNetwork,
			final Namespace namespace, final String jdexId,
			final Map<String, VertexFrame> networkIndex) {
		final INamespace newNamespace = _orientDbGraph.addVertex(
				"class:namespace", INamespace.class);
		newNamespace.setJdexId(jdexId);

		final String prefix = namespace.getPrefix();
		if (prefix != null && !prefix.isEmpty())
			newNamespace.setPrefix(prefix);

		newNamespace.setUri(namespace.getUri());
		newNetwork.addNamespace(newNamespace);
		networkIndex.put(namespace.getJdexId(), newNamespace);
	}

	

	private void createBaseTerm(INetwork target, Network networkToCreate,
			BaseTerm term, String jdexId, Map<String, VertexFrame> networkIndex)
			throws NdexException {
		final IBaseTerm newBaseTerm = _orientDbGraph.addVertex(
				"class:baseTerm", IBaseTerm.class);
		newBaseTerm.setName(((BaseTerm) term).getName());
		newBaseTerm.setJdexId(jdexId);

		String namespaceJdexId = ((BaseTerm) term).getNamespace();

		String nsPrefix = "";
		if (namespaceJdexId != null && !namespaceJdexId.isEmpty()) {
			final INamespace namespace = (INamespace) networkIndex
					.get(namespaceJdexId);
			if (null == namespace)
				throw new NdexException("Namespace " + namespaceJdexId
						+ " referenced by BaseTerm " + jdexId
						+ " was not found in networkIndex cache");

			newBaseTerm.setTermNamespace(namespace);
			nsPrefix = namespace.getPrefix();
		}

		target.addTerm(newBaseTerm);
		networkIndex.put(newBaseTerm.getJdexId(), newBaseTerm);


	}

	private IFunctionTerm createFunctionTerm(INetwork target, String jdexId,
			Map<String, VertexFrame> networkIndex) throws NdexException {
		final IFunctionTerm domainTerm = _orientDbGraph.addVertex(
				"class:functionTerm", IFunctionTerm.class);
		domainTerm.setJdexId(jdexId);
		target.addTerm(domainTerm);
		networkIndex.put(domainTerm.getJdexId(), domainTerm);
		// logger.info("Domain Model: added FunctionTerm " +
		// domainTerm.getJdexId());
		return domainTerm;

	}

	private void populateFunctionTerm(FunctionTerm objectTerm,
			IFunctionTerm domainTerm, Map<String, VertexFrame> networkIndex)
			throws NdexException {

		if (null == domainTerm)
			throw new NdexException("domain FunctionTerm null for "
					+ objectTerm.getTermFunction());

		// populate term function
		final IBaseTerm function = (IBaseTerm) networkIndex.get(objectTerm
				.getTermFunction());
		if (null == function)
			throw new NdexException("BaseTerm " + objectTerm.getTermFunction()
					+ " referenced as function of FunctionTerm "
					+ domainTerm.getJdexId()
					+ " was not found in networkIndex cache");

		domainTerm.setTermFunc(function);

		// populate parameters
		List<ITerm> iParameters = new ArrayList<ITerm>();
		List<String> orderedParameterIds = Lists.newArrayList();
		// Ensure that the parameters are ordered by the integer value of their
		// keys
		TreeMap<Integer, String> sortedParameterMap = new TreeMap<Integer, String>();
		for (Entry<String, String> entry : objectTerm.getParameters()
				.entrySet()) {
			Integer parameterIndex = Integer.parseInt(entry.getKey());
			sortedParameterMap.put(parameterIndex, entry.getValue());
		}

		for (Integer index : sortedParameterMap.keySet()) {
			// All Terms mentioned as parameters should exist
			// (found or created) prior to the current term -
			// it is a requirement of a JDEx format file.
			String parameterJdexId = sortedParameterMap.get(index);
			ITerm parameter = (ITerm) networkIndex.get(parameterJdexId);
			if (null == parameter)
				throw new NdexException("Term " + parameterJdexId
						+ " referenced as parameter of FunctionTerm "
						+ domainTerm.getJdexId()
						+ " was not found in networkIndex cache");

			iParameters.add(parameter);
			orderedParameterIds.add(parameterJdexId);

		}

		domainTerm.setTermParameters(iParameters);
		domainTerm.setTermOrderedParameterIds(orderedParameterIds);
	}

	private IReifiedEdgeTerm createReifiedEdgeTerm(INetwork target,
			String jdexId, Map<String, VertexFrame> networkIndex)
			throws NdexException {
		final IReifiedEdgeTerm domainTerm = _orientDbGraph.addVertex(
				"class:reifiedEdgeTerm", IReifiedEdgeTerm.class);
		domainTerm.setJdexId(jdexId);
		target.addTerm(domainTerm);
		networkIndex.put(domainTerm.getJdexId(), domainTerm);
		return domainTerm;

	}

	private void populateReifiedEdgeTerm(ReifiedEdgeTerm objectTerm,
			IReifiedEdgeTerm domainTerm, Map<String, VertexFrame> networkIndex)
			throws NdexException {

		if (null == domainTerm)
			throw new NdexException("domain ReifiedEdgeTerm null for "
					+ objectTerm.getTermEdge());

		// populate term edge
		final IEdge edge = (IEdge) networkIndex.get(objectTerm.getTermEdge());
		if (null == edge)
			throw new NdexException("Edge " + objectTerm.getTermEdge()
					+ " referenced as edge of ReifiedEdgeTerm "
					+ domainTerm.getJdexId()
					+ " was not found in networkIndex cache");

		domainTerm.setEdge(edge);

	}

	private boolean isFunctionTerm(Term term) {
		if (term.getTermType().equals("Function"))
			return true;
		return false;
	}

	private boolean isReifiedEdgeTerm(Term term) {
		if (term.getTermType().equals("ReifiedEdge"))
			return true;
		return false;
	}

	private boolean isBaseTerm(Term term) {
		if (term.getTermType() == null || term.getTermType().isEmpty()
				|| term.getTermType().equals("Base"))
			return true;
		return false;
	}

	private Map<ReifiedEdgeTerm, IReifiedEdgeTerm> createTerms(
			final Network sourceNetwork,
			final EquivalenceFinder equivalenceFinder) throws NdexException {

		List<String> sortedTermIds = new ArrayList<String>(sourceNetwork
				.getTerms().keySet());
		// Collections.sort(sortedTermIds, tdc);
		// Pass1 - create base terms and function term shells
		Map<FunctionTerm, IFunctionTerm> newFunctionTermMap = new HashMap<FunctionTerm, IFunctionTerm>();
		Map<ReifiedEdgeTerm, IReifiedEdgeTerm> newReifiedEdgeTermMap = new HashMap<ReifiedEdgeTerm, IReifiedEdgeTerm>();
		for (final String jdexId : sortedTermIds) {
			final Term term = sourceNetwork.getTerms().get(jdexId);
			if (isBaseTerm(term)) {
				IBaseTerm iBaseTerm = equivalenceFinder.getBaseTerm(
						(BaseTerm) term, jdexId);
				if (null == iBaseTerm)
					createBaseTerm(equivalenceFinder.getTargetNetwork(),
							sourceNetwork, (BaseTerm) term, jdexId,
							equivalenceFinder.getNetworkIndex());
			} else if (isFunctionTerm(term)) {
				IFunctionTerm iFunctionTerm = equivalenceFinder
						.getFunctionTerm((FunctionTerm) term, jdexId);
				if (null == iFunctionTerm) {
					iFunctionTerm = createFunctionTerm(
							equivalenceFinder.getTargetNetwork(), jdexId,
							equivalenceFinder.getNetworkIndex());
					newFunctionTermMap.put((FunctionTerm) term, iFunctionTerm);
				}
			} else if (isReifiedEdgeTerm(term)) {
				IReifiedEdgeTerm iReifiedEdgeTerm = equivalenceFinder
						.getReifiedEdgeTerm((ReifiedEdgeTerm) term, jdexId);
				if (null == iReifiedEdgeTerm) {
					iReifiedEdgeTerm = createReifiedEdgeTerm(
							equivalenceFinder.getTargetNetwork(), jdexId,
							equivalenceFinder.getNetworkIndex());
					newReifiedEdgeTermMap.put((ReifiedEdgeTerm) term,
							iReifiedEdgeTerm);
				}
			}
		}
		// Pass2 - populate new function terms with function and parameter
		// references
		for (final Entry<FunctionTerm, IFunctionTerm> entry : newFunctionTermMap
				.entrySet()) {
			populateFunctionTerm(entry.getKey(), entry.getValue(),
					equivalenceFinder.getNetworkIndex());
		}

		// Return the map of terms we will need to populate after we make the
		// edges...
		return newReifiedEdgeTermMap;
	}

	/**************************************************************************
	 * Creating terms for an new network
	 * 
	 * No pre-existing terms, therefore no need to check for equivalent terms
	 * pre-existing in network.
	 * 
	 * Terms must be created in order of dependency.
	 * 
	 * Hence they must be created in order of jdexId
	 * 
	 * @throws NdexException
	 * 
	 **************************************************************************/
	private Map<ReifiedEdgeTerm, IReifiedEdgeTerm> createTerms(
			final INetwork targetNetwork, final Network sourceNetwork,
			final Map<String, VertexFrame> networkIndex) throws NdexException {
		
		List<String> sortedTermIds = new ArrayList<String>(sourceNetwork
				.getTerms().keySet());
		// Collections.sort(sortedTermIds, tdc);
		// Pass1 - create base terms and function term shells
		Map<FunctionTerm, IFunctionTerm> newFunctionTermMap = new HashMap<FunctionTerm, IFunctionTerm>();
		Map<ReifiedEdgeTerm, IReifiedEdgeTerm> newReifiedEdgeTermMap = new HashMap<ReifiedEdgeTerm, IReifiedEdgeTerm>();
		for (final String jdexId : sortedTermIds) {
			final Term term = sourceNetwork.getTerms().get(jdexId);
			if (isBaseTerm(term)) {
				createBaseTerm(targetNetwork, sourceNetwork, (BaseTerm) term,
						jdexId, networkIndex);
			} else if (isFunctionTerm(term)) {
				IFunctionTerm iFunctionTerm = createFunctionTerm(targetNetwork,
						jdexId, networkIndex);
				newFunctionTermMap.put((FunctionTerm) term, iFunctionTerm);
			} else if (isReifiedEdgeTerm(term)) {
				IReifiedEdgeTerm iReifiedEdgeTerm = createReifiedEdgeTerm(
						targetNetwork, jdexId, networkIndex);
				newReifiedEdgeTermMap.put((ReifiedEdgeTerm) term,
						iReifiedEdgeTerm);
			}
		}
		// Pass2 - populate new function terms with function and parameter
		// references
		for (final Entry<FunctionTerm, IFunctionTerm> entry : newFunctionTermMap
				.entrySet()) {
			populateFunctionTerm(entry.getKey(), entry.getValue(), networkIndex);
		}
		// Return the map of terms we will need to populate after we make the
		// edges...
		return newReifiedEdgeTermMap;
	}

	private void validateTermMap(Map<String, Term> termMap)
			throws NdexException {
		Set<String> parameterIds = new HashSet<String>();
		for (Entry<String, Term> entry : termMap.entrySet()) {
			String jdexId = entry.getKey();
			Term term = entry.getValue();
			if (term.getTermType().equals("Function")) {
				for (String parameterId : ((FunctionTerm) term).getParameters()
						.values()) {
					if (!termMap.containsKey(parameterId)) {
						throw new NdexException("term " + jdexId
								+ "missing parameter term for id "
								+ parameterId);
					}
					parameterIds.add(parameterId);
				}
			} else {
				logger.info("BaseTerm " + jdexId + " "
						+ ((BaseTerm) term).getName());

			}
		}

		for (String parameterId : parameterIds) {
			logger.info("Found term " + parameterId + " as parameter");
		}

	}

	private void createCitations(final INetwork targetNetwork,
			final Network sourceNetwork,
			final Map<String, VertexFrame> networkIndex) {
		for (final Entry<String, Citation> citationEntry : sourceNetwork
				.getCitations().entrySet()) {
			final Citation citation = citationEntry.getValue();
			final String jdexId = citationEntry.getKey();
			createCitation(targetNetwork, citation, jdexId, networkIndex);
		}
	}

	private void createCitations(final Network sourceNetwork,
			final EquivalenceFinder equivalenceFinder) throws NdexException {
		for (final Entry<String, Citation> citationEntry : sourceNetwork
				.getCitations().entrySet()) {
			final Citation citation = citationEntry.getValue();
			final String jdexId = citationEntry.getKey();
			ICitation iCitation = equivalenceFinder.getCitation(citation,
					jdexId);
			if (null == iCitation)
				createCitation(equivalenceFinder.getTargetNetwork(), citation,
						jdexId, equivalenceFinder.getNetworkIndex());
		}
	}

	private void createCitation(final INetwork targetNetwork,
			final Citation citation, final String jdexId,
			final Map<String, VertexFrame> networkIndex) {
		final ICitation newCitation = _orientDbGraph.addVertex(
				"class:citation", ICitation.class);
		newCitation.setJdexId(jdexId);
		newCitation.setTitle(citation.getTitle());
		newCitation.setIdentifier(citation.getIdentifier());
		newCitation.setType(citation.getType());
		newCitation.setContributors(citation.getContributors());

		targetNetwork.addCitation(newCitation);
		networkIndex.put(newCitation.getJdexId(), newCitation);
	}

	private void createSupports(final INetwork targetNetwork,
			final Network sourceNetwork,
			final Map<String, VertexFrame> networkIndex) throws NdexException {
		for (final Entry<String, Support> supportEntry : sourceNetwork
				.getSupports().entrySet()) {
			final Support support = supportEntry.getValue();
			final String jdexId = supportEntry.getKey();
			createSupport(targetNetwork, support, jdexId, networkIndex);

		}
	}

	private void createSupports(final Network sourceNetwork,
			final EquivalenceFinder equivalenceFinder) throws NdexException {
		for (final Entry<String, Support> supportEntry : sourceNetwork
				.getSupports().entrySet()) {
			final Support support = supportEntry.getValue();
			final String jdexId = supportEntry.getKey();
			ISupport iSupport = equivalenceFinder.getSupport(support, jdexId);
			if (null == iSupport)
				createSupport(equivalenceFinder.getTargetNetwork(), support,
						jdexId, equivalenceFinder.getNetworkIndex());

		}
	}

	private void createSupport(final INetwork targetNetwork,
			final Support support, final String jdexId,
			final Map<String, VertexFrame> networkIndex) throws NdexException {
		final ISupport newSupport = _orientDbGraph.addVertex("class:support",
				ISupport.class);
		newSupport.setJdexId(jdexId);
		newSupport.setText(support.getText());

		if (null != support.getCitation()) {
			ICitation iCitation = (ICitation) networkIndex.get(support
					.getCitation());
			if (null == iCitation)
				throw new NdexException("Citation " + support.getCitation()
						+ " referenced by support " + jdexId
						+ " was not found in networkIndex cache");
			newSupport.setSupportCitation(iCitation);
		}

		targetNetwork.addSupport(newSupport);
		networkIndex.put(newSupport.getJdexId(), newSupport);

	}

	private void createNodes(final Network sourceNetwork,
			final EquivalenceFinder equivalenceFinder) throws NdexException {
		int nodeCount = 0;

		for (final Entry<String, Node> nodeEntry : sourceNetwork.getNodes()
				.entrySet()) {
			final Node node = nodeEntry.getValue();
			final String jdexId = nodeEntry.getKey();
			INode iNode = equivalenceFinder.getNode(node, jdexId);
			if (null == iNode) {
				createNode(equivalenceFinder.getTargetNetwork(), node, jdexId,
						equivalenceFinder.getNetworkIndex());
				nodeCount++;
			}
		}
		equivalenceFinder.getTargetNetwork().setNdexNodeCount(nodeCount);
	}

	private void createNodes(final INetwork targetNetwork,
			final Network sourceNetwork,
			final Map<String, VertexFrame> networkIndex) throws NdexException {
		int nodeCount = 0;

		for (final Entry<String, Node> nodeEntry : sourceNetwork.getNodes()
				.entrySet()) {
			final Node node = nodeEntry.getValue();
			final String jdexId = nodeEntry.getKey();

			createNode(targetNetwork, node, jdexId, networkIndex);
			nodeCount++;
		}

		targetNetwork.setNdexNodeCount(nodeCount);
	}

	private void createNode(final INetwork targetNetwork, final Node node,
			final String jdexId, final Map<String, VertexFrame> networkIndex)
			throws NdexException {
		final INode iNode = _orientDbGraph.addVertex("class:node", INode.class);
		iNode.setJdexId(jdexId);
		if (null != node.getName())
			iNode.setName(node.getName());

		final ITerm representedITerm = (ITerm) networkIndex.get(node
				.getRepresents());
		if (null == representedITerm)
			throw new NdexException("Term " + node.getRepresents()
					+ " referenced by node " + jdexId
					+ " was not found in networkIndex cache");

		iNode.setRepresents(representedITerm);

		targetNetwork.addNdexNode(iNode);
		networkIndex.put(iNode.getJdexId(), iNode);

	}

	
	private void createEdges(final Network sourceNetwork,
			final EquivalenceFinder equivalenceFinder) throws NdexException {
		int edgeCount = 0;

		for (final Entry<String, Edge> edgeEntry : sourceNetwork.getEdges()
				.entrySet()) {
			final Edge edge = edgeEntry.getValue();
			final String jdexId = edgeEntry.getKey();
			IEdge iEdge = equivalenceFinder.getEdge(edge, jdexId);
			if (null == iEdge) {
				createEdge(equivalenceFinder.getTargetNetwork(), edge, jdexId,
						equivalenceFinder.getNetworkIndex());
				edgeCount++;
			}
		}
		equivalenceFinder.getTargetNetwork().setNdexEdgeCount(edgeCount);
	}

	private void createEdges(final INetwork targetNetwork,
			final Network sourceNetwork,
			final Map<String, VertexFrame> networkIndex) throws NdexException {
		int edgeCount = 0;

		for (final Entry<String, Edge> edgeEntry : sourceNetwork.getEdges()
				.entrySet()) {
			final Edge edge = edgeEntry.getValue();
			final String jdexId = edgeEntry.getKey();
			createEdge(targetNetwork, edge, jdexId, networkIndex);
			edgeCount++;
		}
		targetNetwork.setNdexEdgeCount(edgeCount);
	}

	private void createEdge(final INetwork targetNetwork, final Edge edge,
			final String jdexId, final Map<String, VertexFrame> networkIndex)
			throws NdexException {
		final IEdge newEdge = _orientDbGraph.addVertex("class:edge",
				IEdge.class);
		newEdge.setJdexId(jdexId);

		final INode subjectNode = (INode) networkIndex.get(edge.getS());
		if (null == subjectNode)
			throw new NdexException("Node " + edge.getS()
					+ " referenced as subject  of Edge " + jdexId
					+ " was not found in networkIndex cache");
		newEdge.setSubject(subjectNode);

		final IBaseTerm predicateTerm = (IBaseTerm) networkIndex.get(edge
				.getP());
		if (null == predicateTerm)
			throw new NdexException("BaseTerm " + edge.getP()
					+ " referenced as predicate  of Edge " + jdexId
					+ " was not found in networkIndex cache");
		newEdge.setPredicate(predicateTerm);

		final INode objectNode = (INode) networkIndex.get(edge.getO());
		if (null == objectNode)
			throw new NdexException("Node " + edge.getO()
					+ " referenced as object  of Edge " + jdexId
					+ " was not found in networkIndex cache");
		newEdge.setObject(objectNode);

		for (final String citationId : edge.getCitations())
			newEdge.addCitation((ICitation) networkIndex.get(citationId));

		for (final String supportId : edge.getSupports())
			newEdge.addSupport((ISupport) networkIndex.get(supportId));

		targetNetwork.addNdexEdge(newEdge);
		networkIndex.put(newEdge.getJdexId(), newEdge);
	}

	/**************************************************************************
	 * 
	 * Finds terms in network by name
	 * 
	 * TODO: review implementation
	 * 
	 * 
	 **************************************************************************/
	private List<Term> getBaseTermsByName(INetwork network, String baseTermName)
			throws NdexException {
		final List<Term> foundTerms = new ArrayList<Term>();
		for (final ITerm networkTerm : network.getTerms()) {
			if (networkTerm instanceof IBaseTerm) {
				if (baseTermName.equals(((IBaseTerm) networkTerm).getName())) {
					final Term term = new BaseTerm((IBaseTerm) networkTerm);
					foundTerms.add(term);
				}
			}
		}

		return foundTerms;
	}

	/**************************************************************************
	 * 
	 * Constructs and returns a self-sufficient network based on a set of edges
	 * 
	 * Finds all referenced nodes, terms, supports, and citations for the edges.
	 * 
	 * 
	 **************************************************************************/
	private Network getNetworkBasedOnFoundEdges(
			final Collection<IEdge> requiredIEdges, final INetwork network)
			throws NdexException {
		if (requiredIEdges.size() == 0)
			return new Network();
		expandEdgeListToIncludeReifiedEdges(requiredIEdges);
		final Set<INode> requiredINodes = NetworkUtility
				.getEdgeNodes(requiredIEdges);
		final Set<ITerm> requiredITerms = NetworkUtility.getEdgeTerms(
				requiredIEdges, requiredINodes);
		final Set<ISupport> requiredISupports = NetworkUtility
				.getEdgeSupports(requiredIEdges);
		final Set<ICitation> requiredICitations = NetworkUtility
				.getEdgeCitations(requiredIEdges, requiredISupports);
		final Set<INamespace> requiredINamespaces = NetworkUtility
				.getTermNamespaces(requiredITerms);

		return createOutputNetwork(requiredINamespaces, requiredITerms,
				requiredICitations, requiredISupports, requiredINodes,
				requiredIEdges, network);
	}

	private static Network createOutputNetwork(
			final Collection<INamespace> requiredINamespaces,
			final Collection<ITerm> requiredITerms,
			final Collection<ICitation> requiredICitations,
			final Collection<ISupport> requiredISupports,
			final Collection<INode> requiredINodes,
			final Collection<IEdge> requiredIEdges, final INetwork network) {

		// Now create the output network
		final Network outputNetwork = new Network();
		outputNetwork.setDescription(network.getDescription());
		outputNetwork.setMetadata(network.getMetadata());
		outputNetwork.setName(network.getName());

		if (network.getMetaterms() != null) {
			for (Entry<String, IBaseTerm> metaterm : network.getMetaterms()
					.entrySet())
				outputNetwork.getMetaterms().put(metaterm.getKey(),
						new BaseTerm(metaterm.getValue()));
		}

		if (null != requiredIEdges) {
			for (final IEdge edge : requiredIEdges)
				outputNetwork.getEdges().put(edge.getJdexId(), new Edge(edge));

			outputNetwork.setEdgeCount(requiredIEdges.size());
		}

		for (final INode node : requiredINodes)
			outputNetwork.getNodes().put(node.getJdexId(), new Node(node));

		outputNetwork.setNodeCount(requiredINodes.size());

		for (final ITerm term : requiredITerms) {
			if (term instanceof IBaseTerm)
				outputNetwork.getTerms().put(term.getJdexId(),
						new BaseTerm((IBaseTerm) term));
			else if (term instanceof IFunctionTerm)
				outputNetwork.getTerms().put(term.getJdexId(),
						new FunctionTerm((IFunctionTerm) term));
			else if (term instanceof IReifiedEdgeTerm) {

				outputNetwork.getTerms().put(term.getJdexId(),
						new ReifiedEdgeTerm((IReifiedEdgeTerm) term));
			}
		}

		for (final INamespace namespace : requiredINamespaces)
			outputNetwork.getNamespaces().put(namespace.getJdexId(),
					new Namespace(namespace));

		for (final ISupport support : requiredISupports)
			outputNetwork.getSupports().put(support.getJdexId(),
					new Support(support));

		for (final ICitation citation : requiredICitations)
			outputNetwork.getCitations().put(citation.getJdexId(),
					new Citation(citation));

		return outputNetwork;
	}

	private Network getNetworkBasedOnNonEdgeNodes(List<INode> requiredINodes,
			INetwork network) throws NdexException {
		final Set<ITerm> requiredITerms = NetworkUtility.getEdgeTerms(null,
				requiredINodes);
		final Set<INamespace> requiredINamespaces = NetworkUtility
				.getTermNamespaces(requiredITerms);
		final String iNodeCsv = NetworkUtility.joinNodeIdsToCsv(requiredINodes);
		final Set<ISupport> requiredISupports = getNodeSupports(iNodeCsv);
		final Set<ICitation> requiredICitations = getNodeCitations(iNodeCsv);
		return createOutputNetwork(requiredINamespaces, requiredITerms,
				requiredICitations, requiredISupports, requiredINodes, null,
				network);
	}

	private Network getNetworkBasedOnCitations(
			final List<ICitation> requiredICitations, final INetwork network)
			throws NdexException {
		final String citationIdCsv = NetworkUtility
				.joinCitationIdsToCsv(requiredICitations);
		final Set<ISupport> requiredISupports = getCitationSupports(citationIdCsv);
		final Set<INode> requiredINodes = getCitationNodes(citationIdCsv);
		final Set<IEdge> requiredIEdges = getCitationEdges(citationIdCsv);
		final Set<ITerm> requiredITerms = NetworkUtility.getEdgeTerms(
				requiredIEdges, requiredINodes);
		final Set<INamespace> requiredINamespaces = NetworkUtility
				.getTermNamespaces(requiredITerms);
		return createOutputNetwork(requiredINamespaces, requiredITerms,
				requiredICitations, requiredISupports, requiredINodes,
				requiredIEdges, network);
	}

	private Set<ISupport> getCitationSupports(String citationIdCsv) {
		Set<ISupport> foundISupports = new HashSet<ISupport>();
		final String supportQuery = "SELECT FROM (TRAVERSE out_citationSupports from [ "
				+ citationIdCsv + " ]) WHERE @class = 'support'";

		final List<ODocument> supportsFound = _ndexDatabase
				.query(new OSQLSynchQuery<ODocument>(supportQuery));

		for (final ODocument support : supportsFound) {
			foundISupports.add(_orientDbGraph
					.getVertex(support, ISupport.class));
		}
		return foundISupports;
	}

	private Set<IEdge> getCitationEdges(String citationIdCsv) {
		Set<IEdge> foundIEdges = new HashSet<IEdge>();
		final String edgeQuery = "SELECT FROM (TRAVERSE in_edgeCitations, out_citationSupports, in_edgeSupports, out_edgeObject, out_nodeRepresents, out_reifiedEdgeTermEdge from [ "
				+ citationIdCsv + " ]) WHERE @class = 'edge'";

		final List<ODocument> edgesFound = _ndexDatabase
				.query(new OSQLSynchQuery<ODocument>(edgeQuery));

		for (final ODocument edge : edgesFound) {
			foundIEdges.add(_orientDbGraph.getVertex(edge, IEdge.class));
		}
		return foundIEdges;
	}

	private Set<INode> getCitationNodes(String citationIdCsv) {
		Set<INode> foundINodes = new HashSet<INode>();
		final String nodeQuery = "SELECT FROM (TRAVERSE in_nodeCitations, out_citationSupports, in_nodeSupports, in_edgeCitations, in_edgeSupports, in_edgeSubject, out_edgeObject, out_nodeRepresents, out_reifiedEdgeTermEdge from [ "
				+ citationIdCsv + " ]) WHERE @class = 'node'";

		final List<ODocument> nodesFound = _ndexDatabase
				.query(new OSQLSynchQuery<ODocument>(nodeQuery));

		for (final ODocument node : nodesFound) {
			foundINodes.add(_orientDbGraph.getVertex(node, INode.class));
		}
		return foundINodes;
	}

	private Set<ICitation> getNodeCitations(String nodeIdCsv) {
		Set<ICitation> foundICitations = new HashSet<ICitation>();
		final String citationQuery = "SELECT FROM (TRAVERSE out_nodeCitations, out_nodeSupports, out_supportCitation from [ "
				+ nodeIdCsv + " ]) WHERE @class = 'citation'";

		final List<ODocument> citationsFound = _ndexDatabase
				.query(new OSQLSynchQuery<ODocument>(citationQuery));

		for (final ODocument citation : citationsFound) {
			foundICitations.add(_orientDbGraph.getVertex(citation,
					ICitation.class));
		}
		return foundICitations;
	}

	private Set<ISupport> getNodeSupports(String nodeIdCsv) {
		Set<ISupport> foundISupports = new HashSet<ISupport>();
		final String supportQuery = "SELECT FROM (TRAVERSE out_nodeSupports from [ "
				+ nodeIdCsv + " ]) WHERE @class = 'support'";

		final List<ODocument> supportsFound = _ndexDatabase
				.query(new OSQLSynchQuery<ODocument>(supportQuery));

		for (final ODocument support : supportsFound) {
			foundISupports.add(_orientDbGraph
					.getVertex(support, ISupport.class));
		}
		return foundISupports;
	}

	private void expandEdgeListToIncludeReifiedEdges(
			final Collection<IEdge> edges) {
		if (edges.size() == 0)
			return;
		logger.info("Edges to expand = " + edges.size());
		
		final String edgeIdCsv = NetworkUtility.joinEdgeIdsToCsv(edges);
		final String edgeQuery = "SELECT FROM (TRAVERSE in_edgeSubject, out_edgeObject, out_nodeRepresents, out_reifiedEdgeTermEdge from [ "
				+ edgeIdCsv
				+ " ] while $depth < 4) where @class ='edge' and $depth > 0 limit 1000";

		final List<ODocument> edgesFound = _ndexDatabase
				.query(new OSQLSynchQuery<ODocument>(edgeQuery));

		logger.info("Additional reified edges found = " + edgesFound.size());
		for (final ODocument edge : edgesFound) {
			edges.add(_orientDbGraph.getVertex(edge, IEdge.class));
		}

	}

	/**************************************************************************
	 * Determines if the logged in user has sufficient permissions to a network.
	 * 
	 * @param targetNetwork
	 *            The network to test for permissions.
	 * @return True if the member has permission, false otherwise.
	 **************************************************************************/
	private boolean hasPermission(String userId, Network targetNetwork,
			Permissions requiredPermissions) {
		if (null != this.findIuserById(userId)) {
			User user = new User(this.findIuserById(userId), true);
			for (Membership networkMembership : user.getNetworks()) {
				if (networkMembership.getResourceId().equals(
						targetNetwork.getId())
						&& networkMembership.getPermissions().compareTo(
								requiredPermissions) > -1)
					return true;
			}
		}
		return false;
	}


	

	/**************************************************************************
	 * Parses (base) terms from the search parameters.
	 * 
	 * @param searchParameters
	 *            The search parameters.
	 * @return A string containing additional SQL to add to the WHERE clause.
	 **************************************************************************/
	private String parseTermParameters(final SearchParameters searchParameters) {
		final Pattern termRegex = Pattern.compile("terms:\\{(.+)\\}");
		final Matcher termMatcher = termRegex.matcher(searchParameters
				.getSearchString());

		if (termMatcher.find()) {
			final String terms[] = termMatcher.group(1).split(",");
			final StringBuilder termConditions = new StringBuilder();
			termConditions
					.append("  AND @RID IN (SELECT in_networkTerms FROM (TRAVERSE out_networkTerms FROM Network) \n");

			for (final String term : terms) {
				final String namespaceAndTerm[] = term.split(":");
				if (namespaceAndTerm.length != 2)
					throw new IllegalArgumentException(
							"Error parsing terms from: \""
									+ termMatcher.group(0) + "\".");

				searchParameters.setSearchString(searchParameters
						.getSearchString().replace(termMatcher.group(0), ""));

				if (termConditions.length() < 100)
					termConditions.append("    WHERE (");
				else
					// TODO: Originally the idea here was to perform a UNION
					// query against the network get all terms, unfortunately,
					// while OrientDB has a UNION function, it's more of an
					// array concatenation as opposed to merging query results.
					// Instead it seems that OrientDB has a CONTAINS operator
					// that might do the trick, otherwise multi-term searching
					// will be very, very tricky. Another alternative would be
					// using custom Java functions, which OrientDB supports as
					// being usable within a SQL query. Once a solution has
					// been discovered, replace the commented line below with
					// whatever is needed to join all the conditions together.
					break;
				// termConditions.append("\n      AND ");

				termConditions
						.append("out_baseTermNamespace.prefix.toLowerCase() = '");
				termConditions.append(namespaceAndTerm[0].trim().toLowerCase());
				termConditions.append("' AND name.toLowerCase() = '");
				termConditions.append(namespaceAndTerm[1].trim().toLowerCase());
				termConditions.append("') ");
			}

			termConditions.append(") \n");
			return termConditions.toString();
		}

		return null;
	}

	

}
