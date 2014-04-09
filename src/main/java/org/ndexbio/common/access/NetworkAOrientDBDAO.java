package org.ndexbio.common.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.helpers.IdConverter;
import org.ndexbio.common.models.data.IBaseTerm;
import org.ndexbio.common.models.data.IEdge;
import org.ndexbio.common.models.data.INode;
import org.ndexbio.common.models.object.BaseTerm;
import org.ndexbio.common.models.object.Edge;
import org.ndexbio.common.models.object.Network;
import org.ndexbio.common.models.object.NetworkQueryParameters;
import org.ndexbio.common.models.object.Node;
import org.ndexbio.common.models.object.Term;
import org.ndexbio.common.models.object.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orientechnologies.orient.core.id.OClusterPositionFactory;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

public class NetworkAOrientDBDAO extends NdexAOrientDBDAO implements NetworkADAO {

	private static final Logger _logger = LoggerFactory
			.getLogger(NetworkAOrientDBDAO.class);

	public NetworkAOrientDBDAO() {
		super();
	}

	/*
	 * Returns a block of BaseTerm objects in the network specified by
	 * networkId. 'blockSize' specified the number of terms to retrieve in the
	 * block, 'skipBlocks' specifies the number of blocks to skip.")
	 */
	/* (non-Javadoc)
	 * @see org.ndexbio.common.access.NetworkADAO#getTerms(java.lang.String, int, int)
	 */
	@Override
	public List<BaseTerm> getTerms(User user, final String networkId,
			final int skipBlocks, final int blockSize)
			throws IllegalArgumentException, NdexException {
		final ORID networkRid = checkAndConvertNetworkId(networkId);
		checkBlockSize(blockSize);

		final List<BaseTerm> foundTerms = new ArrayList<BaseTerm>();
		final int startIndex = skipBlocks * blockSize;

		final String query = "SELECT name, jdexId, out_baseTermNamespace.jdexId as namespaceId FROM (TRAVERSE out_networkTerms FROM "
				+ networkRid
				+ " \n "
				+ "WHILE $depth < 2) WHERE @class='baseTerm' "
				+ "ORDER BY name \n"
				+ "SKIP "
				+ startIndex
				+ " \n"
				+ "LIMIT "
				+ blockSize;

		try {
			setup();

			checkNetworkExists(networkRid, networkId);

			final List<ODocument> baseTerms = _ndexDatabase
					.query(new OSQLSynchQuery<ODocument>(query));
			for (final ODocument doc : baseTerms) {
				String name = doc.field("name");
				String namespaceId = doc.field("namespaceId");
				String jdexId = doc.field("jdexId");

				foundTerms.add(new BaseTerm(name, jdexId, namespaceId));
			}
			return foundTerms;
		} catch (ObjectNotFoundException onfe) {
			throw onfe;
		} catch (Exception e) {
			_logger.error("Failed to query network: " + networkId + ".", e);
			throw new NdexException(e.getMessage());
		} finally {
			teardown();
		}
	}
	
	public Network getEdges(
			User loggedInUser, 
			String networkId,
			int skipBlocks, 
			int blockSize) 
	
		throws IllegalArgumentException, NdexException {
			try {
				setup();

				final ORID networkRid = checkAndConvertNetworkId(networkId);
				checkBlockSize(blockSize);
				final long startTime = System.currentTimeMillis();
				Set<ORID> edgeRids = getEdgeRids(networkRid, skipBlocks, blockSize);
				
				final long getEdgesTime = System.currentTimeMillis();
				Network network =  getSubnetworkByEdgeRids(networkRid, edgeRids);
				final long getSubnetworkTime = System.currentTimeMillis();
				System.out.println("  Network Nodes : " + network.getNodes().values().size());
				System.out.println("  Network Edges : " + network.getEdges().values().size());
				System.out.println("  Getting Edges : " + (getEdgesTime - startTime));
				System.out.println("Getting Network : " + (getSubnetworkTime - getEdgesTime));
				return network;
			} catch (Exception e) {
				e.printStackTrace();
				throw new NdexException("Error in getEdges: " + e.getLocalizedMessage());
				
			} finally {
				teardown();
			}
			
	}




	/* (non-Javadoc)
	 * @see org.ndexbio.common.access.NetworkADAO#queryForEdges(java.lang.String, org.ndexbio.common.models.object.NetworkQueryParameters, int, int)
	 */
	@Override
	public List<Edge> queryForEdges(final User user, final String networkId,
			final NetworkQueryParameters parameters, final int skipBlocks,
			final int blockSize) throws IllegalArgumentException, NdexException {
		try {
			setup();

			final ORID networkRid = checkAndConvertNetworkId(networkId);
			checkBlockSize(blockSize);
			
			Set<ORID> edgeRids = queryForEdgeRids(networkRid, parameters,
					skipBlocks, blockSize);
			
			return getEdgesByEdgeRids(edgeRids);
		} catch (Exception e) {
			e.printStackTrace();
			throw new NdexException("Error in queryForEdges: " + e.getLocalizedMessage());
		} finally {
			teardown();
		}
		
	}

	/* (non-Javadoc)
	 * @see org.ndexbio.common.access.NetworkADAO#queryForSubnetwork(java.lang.String, org.ndexbio.common.models.object.NetworkQueryParameters, int, int)
	 */
	@Override
	public Network queryForSubnetwork(final User user, final String networkId,
			final NetworkQueryParameters parameters, final int skipBlocks,
			final int blockSize) throws IllegalArgumentException, NdexException {

		try {
			setup();

			final ORID networkRid = checkAndConvertNetworkId(networkId);
			checkBlockSize(blockSize);
			final long startTime = System.currentTimeMillis();
			Set<ORID> edgeRids = queryForEdgeRids(
					networkRid, 
					parameters,
					skipBlocks, 
					blockSize);
			final long getEdgesTime = System.currentTimeMillis();
			Network network =  getSubnetworkByEdgeRids(networkRid, edgeRids);
			final long getSubnetworkTime = System.currentTimeMillis();
			System.out.println("  Network Nodes : " + network.getNodes().values().size());
			System.out.println("  Network Edges : " + network.getEdges().values().size());
			System.out.println("  Getting Edges : " + (getEdgesTime - startTime));
			System.out.println("Getting Network : " + (getSubnetworkTime - getEdgesTime));
			return network;
		} catch (Exception e) {
			e.printStackTrace();
			throw new NdexException("Error in queryForSubnetwork: " + e.getLocalizedMessage());
		} finally {
			teardown();
		}
	}
	
	private Set<ORID> getEdgeRids(ORID networkRid, int skipBlocks, int blockSize) {
		Set<ORID> result = new HashSet<ORID>();

		final String query = 
				"SELECT @rid as rid FROM "
				+ "(TRAVERSE out_networkEdges FROM " 
				+ networkRid 
				+ " WHILE $depth < 2) WHERE @class = 'edge' LIMIT " + blockSize + " SKIP " + skipBlocks;
		// System.out.println("node query: " + query);
		final List<ODocument> edges = _ndexDatabase
				.query(new OSQLSynchQuery<ODocument>(query));
		for (final ODocument doc : edges) {
			ORID rid = doc.field("rid", ORID.class);
			result.add(rid);
		}
		return result;
	}


	private Set<ORID> queryForEdgeRids(ORID networkRid,
			NetworkQueryParameters parameters, int skipBlocks, int blockSize) {
		// Select query handler depending on parameters
		// TODO validate parameters
		if ("INTERCONNECT" == parameters.getSearchType()) {
			List<ORID> sourceNodeRids = getNodeRidsFromTermNames(networkRid,
					parameters.getStartingTermStrings(), true); // change to
																// nodes
																// later...
			List<ORID> targetNodeRids = sourceNodeRids;
			List<ORID> includedPredicateRids = new ArrayList<ORID>();
			// getBaseTermRids(networkRid,
			// parameters.getIncludedPredicateIds());
			int maxDepth = parameters.getSearchDepth();
			
			System.out.println("-----------------------------------------\n");
			System.out.println(" Interconnect Query ");
			System.out.println("-----------------------------------------\n");

			return edgeIdsBySourceTargetTraversal(networkRid, sourceNodeRids,
					targetNodeRids, includedPredicateRids, maxDepth);

		} else if ("NEIGHBORHOOD" == parameters.getSearchType()) {
			List<ORID> sourceNodeRids = getNodeRidsFromTermNames(networkRid,
					parameters.getStartingTermStrings(), true); // change to
																// nodes
			List<ORID> includedPredicateRids = new ArrayList<ORID>();
			// getBaseTermRids(networkRid,parameters.getIncludedPredicateIds());
			int maxDepth = parameters.getSearchDepth();
			
			System.out.println("-----------------------------------------\n");
			System.out.println(" Neighborhood Query ");
			System.out.println("-----------------------------------------\n");

			return edgeIDsByTraversalFromSourceNode(networkRid, sourceNodeRids,
					includedPredicateRids, maxDepth);
		}
		return null;
	}

	private List<ORID> getBaseTermRidsFromIds(ORID networkRid,
			List<String> baseTermIds) {
		// TODO Auto-generated method stub
		return null;
	}

	private List<ORID> getBaseTermRidsFromNames(ORID networkRid,
			List<String> termNames) {

		String termNameCsv = stringsToCsv(termNames);
		List<ORID> result = new ArrayList<ORID>();

		// select from (traverse out_networkTerms from #24:733
		// while $depth < 2) where @CLASS='baseTerm' and name like "AKT%" limit
		// 10
		final String query = "SELECT @rid as rid "
				+ "FROM (traverse out_networkTerms from " + networkRid + " \n"
				+ "WHILE $depth < 2) \n"
				+ "WHERE @CLASS='baseTerm' AND name IN [" + termNameCsv + "] ";

		final List<ODocument> baseTerms = _ndexDatabase
				.query(new OSQLSynchQuery<ODocument>(query));
		for (final ODocument doc : baseTerms) {
			ORID rid = doc.field("rid", ORID.class);
			result.add(rid);
		}
		return result;

	}

	private List<ORID> getNodeRidsFromTermNames(ORID networkRid,
			List<String> termNames, boolean includeAliases) {
		return getNodeRidsFromTermRids(networkRid,
				getBaseTermRidsFromNames(networkRid, termNames), includeAliases);
	}

	private List<ORID> getNodeRidsFromTermRids(ORID networkRid,
			List<ORID> termRids, boolean includeAliases) {
		List<ORID> result = new ArrayList<ORID>();
		String termIdCsv = ridsToCsv(termRids);

		// Example query tested:
		// select @RID from (traverse in_functionTermParameters,
		// in_nodeRepresents from #15:277536, #15:273306 while $depth < 4) where
		// @CLASS='node' limit 10

		String traverseEdgeTypes = null;
		if (includeAliases) {
			traverseEdgeTypes = "in_functionTermParameters, in_nodeRepresents, in_nodeRelationshipAliases, in_nodeUnificationAliases";
		} else {
			traverseEdgeTypes = "in_functionTermParameters, in_nodeRepresents";
		}

		final String query = "SELECT @rid as rid FROM (traverse "
				+ traverseEdgeTypes + " from \n[" + termIdCsv + "] \n"
				+ "WHILE $depth < 10) \n" + "WHERE @CLASS='node' ";
		// System.out.println("node query: " + query);
		final List<ODocument> nodes = _ndexDatabase
				.query(new OSQLSynchQuery<ODocument>(query));
		for (final ODocument node : nodes) {
			ORID rid = node.field("rid", ORID.class);
			result.add(rid);
		}
		return result;
	}

	/*
	 * result.add(new Node( node.field("jdexId"), node.field("name"),
	 * node.field("representsId"), node.field("metadata")));
	 */

	private List<Edge> getEdgesByEdgeRids(Set<ORID> edgeRids) {
		List<Edge> result = new ArrayList<Edge>();
		String edgeIdCsv = ridsToCsv(edgeRids);

		final String query = 
				"SELECT jdexId, in_edgeSubject.jdexId as sId, out_edgePredicate.jdexId as pId, out_edgeObject.jdexId as oId FROM "
				+ "[ " + edgeIdCsv + " ] ";
		// System.out.println("node query: " + query);
		final List<ODocument> edges = _ndexDatabase
				.query(new OSQLSynchQuery<ODocument>(query));
		for (final ODocument doc : edges) {
			String jdexId = doc.field("jdexId");
			String subjectId = doc.field("sId");
			String predicateId = doc.field("pId");
			String objectId = doc.field("oId");
			Edge edge = new Edge(
					jdexId, 
					subjectId,
					predicateId,
					objectId);
			result.add(edge);
		}
		return result;
	}

	private Network getSubnetworkByEdgeRids(ORID networkRid, Set<ORID> edgeRids) {
		// Create Network
		Network network = new Network();
		
		Map<String, ORID> nodeIdRidMap = new HashMap<String, ORID>();
		Map<String, ORID> termIdRidMap = new HashMap<String, ORID>();
		
		// Expand edgeRids to include reified edges
		addReifiedEdgeIds(network, networkRid, edgeRids);
		addEdgesToNetwork(network, networkRid, edgeRids, nodeIdRidMap, termIdRidMap);
		
		// Get Nodes
		addNodesToNetwork(network, networkRid, nodeIdRidMap, termIdRidMap);
		
		// Get Terms
		//addTermsToNetwork(network, networkRid, nodeIdRidMap, termIdRidMap);
		
		// Add Supports and Citations to Network
		//addSupportsAndCitationsToNetwork(network, networkRid, edgeRids);
		
		// Add Namespaces for Terms
		//addNamespacesToNetwork(network, networkRid, termIdRidMap);
		
		// Add Network Properties
	
		return network;
	}

	private void addReifiedEdgeIds(Network network, ORID networkRid,
			Set<ORID> edgeRids) {
		if (null == edgeRids || edgeRids.size() == 0) return;
		System.out.println("Finding reified edges for = " + edgeRids.size() + " edges");
		String edgeIdCsv = ridsToCsv(edgeRids);
		// TODO: check current limit of 1000!
		
		// This query
		final String edgeQuery = "SELECT @rid as rid FROM (TRAVERSE in_edgeSubject, out_edgeObject, out_nodeRepresents, out_reifiedEdgeTermEdge from [ " 
				+ edgeIdCsv
				+ " ] while $depth < 4) where @class ='edge' and $depth > 0 limit 1000";
		
		
		final List<ODocument> edgesFound = _ndexDatabase
				.query(new OSQLSynchQuery<ODocument>(edgeQuery));
		
		System.out.println("Additional reified edges found = " + edgesFound.size());
		for (final ODocument doc : edgesFound) {
			edgeRids.add((ORID) doc.field("rid", ORID.class));
		}
		
	}

	private void addEdgesToNetwork(Network network, ORID networkRid,
			Set<ORID> edgeRids, Map<String, ORID> nodeIdRidMap, Map<String, ORID> termIdRidMap) {
		String edgeIdCsv = ridsToCsv(edgeRids);
		Map<String, Edge> edges = network.getEdges();
		Map<String, Node> nodes = network.getNodes();
		Map<String, Term> terms = network.getTerms();
		
		final String query = 
				"SELECT jdexId, in_edgeSubject.jdexId as sId, in_edgeSubject.@rid as sRid, "
				+ "out_edgePredicate.jdexId as pId, out_edgePredicate.@rid as pRid, "
				+ "out_edgeObject.jdexId as oId, out_edgeObject.@rid as oRid FROM "
				+ "[ " + edgeIdCsv + " ] ";
		
		final List<ODocument> docs = _ndexDatabase.query(new OSQLSynchQuery<ODocument>(query));
		for (final ODocument doc : docs) {
			String jdexId = doc.field("jdexId");
			String subjectId = doc.field("sId");
			ORID subjectRid = doc.field("sRid", ORID.class);
			String predicateId = doc.field("pId");
			ORID predicateRid = doc.field("pRid", ORID.class);
			String objectId = doc.field("oId");
			ORID objectRid = doc.field("oRid", ORID.class);
			
			nodeIdRidMap.put(subjectId, subjectRid);
			nodeIdRidMap.put(objectId, objectRid);
			termIdRidMap.put(predicateId, predicateRid);
			Edge edge = new Edge(
					jdexId, 
					subjectId,
					predicateId,
					objectId);
			edges.put(jdexId, edge);

		}
		
		System.out.println("Added edges to network: " + network.getEdges().values().size());
		
	}

	private void addNodesToNetwork(
			Network network, 
			ORID networkRid,
			Map<String, ORID> nodeIdRidMap,
			Map<String, ORID> termIdRidMap) {
		String nodeIdCsv = ridsToCsv(nodeIdRidMap.values());
		final String query = 
				"SELECT jdexId, name, out_nodeRepresents.jdexId as repId, "
				+ "out_nodeRepresents.@rid as repRid "
				+ "FROM "
				+ "[ " + nodeIdCsv + " ] ";
		final List<ODocument> docs = _ndexDatabase.query(new OSQLSynchQuery<ODocument>(query));
		for (final ODocument doc : docs) {
			String nodeId = doc.field("jdexId");
			String name = doc.field("name");
			String repId = doc.field("repId");
			//ORID nodeRid = doc.field("nodeRid", ORID.class);
			
			Node node = new Node();
			node.setId(nodeId);
			if (null != name) node.setName(name);
			if (null != repId) node.setRepresents(repId);
			network.getNodes().put(nodeId, node);
			
			ORID repRid = doc.field("repRid", ORID.class);
			if (null != repRid) termIdRidMap.put(repId, repRid);
			
		}
		
		System.out.println("Added nodes to network: " + network.getNodes().values().size());
		
		// add node aliases
		final String aliasQuery =
				"select jdexId as aliasId, @rid as aliasRid, in_nodeUnificationAliases.jdexId as nodeId "
				+ "from (traverse out_networkTerms from "
				+ networkRid 
				+ " while $depth < 2) where in_nodeUnificationAliases in [ " 
				+ nodeIdCsv + " ] ";
		
		final List<ODocument> aliasDocs = _ndexDatabase.query(new OSQLSynchQuery<ODocument>(aliasQuery));
		for (final ODocument doc : aliasDocs) {
			String nodeId= doc.field("nodeId");
			String aliasId= doc.field("aliasId");
			ORID aliasRid = doc.field("aliasRid", ORID.class);
			Node node = network.getNodes().get(nodeId);
			// add the alias id to the node in the network
			if (null != node) node.getAliases().add(aliasId);
			// add the alias to our map of terms
			if (null != aliasRid) termIdRidMap.put(aliasId, aliasRid);
		}
	}

	private void addTermsToNetwork(Network network, ORID networkRid,
			Map<String, ORID> nodeIdRidMap, Map<String, ORID> termIdRidMap) {
		// TODO Auto-generated method stub
		
	}

	private void addSupportsAndCitationsToNetwork(Network network,
			ORID networkRid, Set<ORID> edgeRids) {
		// TODO Auto-generated method stub
		
	}

	private void addNamespacesToNetwork(Network network, ORID networkRid,
			Map<String, ORID> termIdRidMap) {
		// TODO Auto-generated method stub
		
	}

	private Set<ORID> edgeIdsBySourceTargetTraversal(ORID networkRid,
			List<ORID> sourceNodeRids, List<ORID> targetNodeRids,
			List<ORID> includedPredicateRids, int maxDepth) {
		Set<ORID> allEdgeRids = new HashSet<ORID>();
		Set<ORID> foundEdgeRids = new HashSet<ORID>();
		Map<ORID, Integer> foundNodeRidDepthMap = new HashMap<ORID, Integer>();
		Map<ORID, Integer> searchedNodeRidDepthMap = new HashMap<ORID, Integer>();
		Stack<ORID> edgeRidStack = new Stack<ORID>();
		Stack<ORID> nodeRidStack = new Stack<ORID>();
		for (ORID sourceNodeRid : sourceNodeRids) {
			// each source node search is completely separate
			// because interactions will make the code hard to 
			// debug and or optimize
			nodeRidStack.clear();
			edgeRidStack.clear();
			foundEdgeRids.clear();
			foundNodeRidDepthMap.clear();
			searchedNodeRidDepthMap.clear();
			
			nodeRidStack.push(sourceNodeRid);
			
			System.out.println("-----------------------------------------\n");
			System.out.println("source node: " + sourceNodeRid);
			System.out.println("-----------------------------------------\n");
			
			searchFromNodeToTargetNodes(sourceNodeRid, nodeRidStack,
					edgeRidStack, includedPredicateRids, targetNodeRids,
					maxDepth, foundEdgeRids, foundNodeRidDepthMap, searchedNodeRidDepthMap);
			allEdgeRids.addAll(foundEdgeRids);
			
			
			
		}
		return allEdgeRids;
	}

	// This is the basis for a neighborhood query
	private Set<ORID> edgeIDsByTraversalFromSourceNode(ORID networkRid,
			List<ORID> sourceNodeRids, List<ORID> includedPredicateRids,
			int maxDepth) {

		Set<ORID> foundEdgeRids = new HashSet<ORID>();
		Set<ORID> foundNodeRids = new HashSet<ORID>();

			
			searchFromNode(
					sourceNodeRids, 
					includedPredicateRids, 
					1,
					maxDepth,
					foundEdgeRids, 
					foundNodeRids);
			

		return foundEdgeRids;
	}
	


	private void searchFromNode(
			List<ORID> sourceNodeRids,
			List<ORID> includedPredicateRids, 
			int depth,
			int maxDepth,
			Set<ORID> foundEdgeRids, 
			Set<ORID> foundNodeRids) {
		boolean belowMaxDepth = depth <= maxDepth;
		System.out.println("search from  : " + sourceNodeRids.size() + " nodes at depth " + depth + " starting with foundEdges " + foundEdgeRids.size());
		
		List<ORID[]> results = sourceSearchQuery(sourceNodeRids, includedPredicateRids, false);

		results.addAll(sourceSearchQuery(sourceNodeRids, includedPredicateRids, true));
		
		List<ORID> nextSourceNodeRids = new ArrayList<ORID>();
		
		// for each edgeId + nodeId pair found:
		for (ORID[] result : results) {
			ORID nodeRid = result[1];
			ORID edgeRid = result[0];
			
			foundEdgeRids.add(edgeRid);
			if (!foundNodeRids.contains(nodeRid)) nextSourceNodeRids.add(nodeRid);
	
		}
		
		foundNodeRids.addAll(nextSourceNodeRids);
		if (belowMaxDepth){
			searchFromNode(nextSourceNodeRids, includedPredicateRids, depth + 1, maxDepth, foundEdgeRids, foundNodeRids);
		}
	}

	private List<ORID[]> sourceSearchQuery(
			List<ORID> sourceNodeRids,
			List<ORID> includedPredicateRids,
			boolean upstream) {

		String query;
		String sourceNodeCsv = ridsToCsv(sourceNodeRids);
		
		if (upstream){
			query= "select $path from "
				+ "(traverse in_edgeObject, in_edgeSubject from [ "
				+ sourceNodeCsv
				+ " ] while $depth < 3) where $depth = 2";
		} else {
			query= "select $path from "
					+ "(traverse out_edgeSubject, out_edgeObject from [ "
					+ sourceNodeCsv 
					+ " ] while $depth < 3) where $depth = 2";
		}
		
		System.out.println("query = " + query);

		List<ODocument> paths = _ndexDatabase
				.query(new OSQLSynchQuery<ODocument>(query));

		List<ORID[]> results = new ArrayList<ORID[]>();

		for (ODocument doc : paths) {
			String path = (String) doc.fieldValues()[0];
			String[] components = path.split("\\.");
			ORID edgeRid = stringToRid(components[2]);
			ORID nodeRid = stringToRid(components[4]);
			ORID[] pair = { edgeRid, nodeRid };
			results.add(pair);
			//System.out.println("edgeId = " + pair[0] + " nodeRid = " + pair[1]);
		}
		if (upstream){
			System.out.println("sourceQuery upstream results : " + results.size());
		} else {
			System.out.println("sourceQuery downstream results : " + results.size());
		}

		return results;
		
	}

	// this is the basis for a source-target or interconnect query
	private void searchFromNodeToTargetNodes(ORID sourceNodeRid,
			Stack<ORID> nodeRidStack, Stack<ORID> edgeRidStack,
			List<ORID> includedPredicateRids, List<ORID> targetNodeRids,
			int maxDepth, Set<ORID> foundEdgeRids, Map<ORID, Integer> foundNodeRidDepthMap, Map<ORID, Integer> searchedNodeRidDepthMap) {
		
		System.out.println("searchFromNodeToTargetNodes  : " + sourceNodeRid + " at " + edgeRidStack.size() + " with foundEdges " + foundEdgeRids.size());

		// Check to see if we are at maxDepth:
		// is the size of the edgeStack < maxDepth?
		int depth = edgeRidStack.size();
		boolean belowMaxDepth = depth <= maxDepth;

		// Query to find edges and linked nodes from source node
		// Excludes nodes in nodeStack and edges in edgeStack - no loops!

		List<ORID[]> results = sourceTargetSearchQuery(sourceNodeRid,
				nodeRidStack, edgeRidStack, includedPredicateRids, false);
		
		results.addAll(sourceTargetSearchQuery(sourceNodeRid,
				nodeRidStack, edgeRidStack, includedPredicateRids, true));

		// for each edgeId + nodeId pair found:
		for (ORID[] result : results) {
			ORID nodeRid = result[1];
			ORID edgeRid = result[0];

			// 1. if nodeId is in targetNodeRids, 
			// 2. or if nodeId is in foundNodeRids
			// then add the edgeStack to foundEdgeRids
			// and add the nodeStack to foundNodeRids
			if (targetNodeRids.contains(nodeRid)) {
				System.out.println("found target " + nodeRid + " at depth " + depth);
				for (ORID rid : edgeRidStack) {
					foundEdgeRids.add(rid);
				}
				updateMinimumDepths(nodeRidStack, foundNodeRidDepthMap);

			} else if (foundNodeRidDepthMap.containsKey(nodeRid) && foundNodeRidDepthMap.get(nodeRid) <= depth) {
				System.out.println("node " + nodeRid 
						+ " was previously in a path at depth " 
						+ foundNodeRidDepthMap.get(nodeRid) 
						+ " which is less than or equal to current depth "+ depth);
				for (ORID rid : edgeRidStack) {
					foundEdgeRids.add(rid);
				}
				updateMinimumDepths(nodeRidStack, foundNodeRidDepthMap);
			}
			

			// if below the max depth, then we can recurse
			if (belowMaxDepth) {
				
				// skip this node if we have already searched from it at this depth or lower
				if (hasPriorSearchAtNextDepthOrLower(nodeRid, depth, searchedNodeRidDepthMap)){
					System.out.println("Already searched " + nodeRid + " at " + searchedNodeRidDepthMap.get(nodeRid) + " when next depth is " + (depth + 1));
				} else {
					nodeRidStack.push(nodeRid);
					edgeRidStack.push(edgeRid);
					
					searchFromNodeToTargetNodes(nodeRid, nodeRidStack,
							edgeRidStack, includedPredicateRids, targetNodeRids,
							maxDepth, foundEdgeRids, foundNodeRidDepthMap, searchedNodeRidDepthMap);
					
					nodeRidStack.pop();
					edgeRidStack.pop();
					
					searchedNodeRidDepthMap.put(nodeRid, depth);
				} 
			}
		}
		System.out.println("searchFromNodeToTargetNodes done with : " + foundEdgeRids.size() + " at " + edgeRidStack.size());

	}

	private boolean hasPriorSearchAtNextDepthOrLower(ORID nodeRid, int depth,
			Map<ORID, Integer> nodeRidDepthMap) {
		int nextDepth = depth + 1;
		Integer priorMinmumDepth = nodeRidDepthMap.get(nodeRid);
		if (null == priorMinmumDepth) return false; // never searched before
		if (nextDepth < priorMinmumDepth) return false; // the next depth is lower than prior searches
		return true;
	}

	private void updateMinimumDepths(Stack<ORID> nodeRidStack,
			Map<ORID, Integer> nodeRidDepthMap) {
		for (int depth = 0; depth < nodeRidStack.size(); depth++){
			ORID stackNodeRid = nodeRidStack.get(depth);
			Integer previousMinimumDepth = nodeRidDepthMap.get(stackNodeRid);
			if (null == previousMinimumDepth || depth < previousMinimumDepth){
				nodeRidDepthMap.put(stackNodeRid, depth);
			}
		}
		
	}

	public void testSTQuery() {
		try {
			setup();
			ORID rid = new ORecordId(28,
					OClusterPositionFactory.INSTANCE.valueOf(1837));
			sourceTargetSearchQuery(rid, null, null, null, false);
		} finally {
			teardown();
		}

	}

	private List<ORID[]> sourceTargetSearchQuery(ORID sourceNodeRid,
			Stack<ORID> nodeRidStack, Stack<ORID> edgeRidStack,
			List<ORID> includedPredicateRids,
			boolean upstream) {
		// select $path from (traverse out_edgeSubject, out_edgeObject from
		// #28:1837 while $depth < 3) where $depth = 2

		// predicateIds

		//
		String conditionalString = " while $depth < 3";
		if (null != nodeRidStack && nodeRidStack.size() > 0) {
			conditionalString = 
					" while $depth < 2 OR ($depth = 2 and @rid not in [ " 
					+ ridsToCsv(nodeRidStack)
					+ " ] )";
		}

		String query;
		
		if (upstream){
			query= "select $path from "
				+ "(traverse in_edgeObject, in_edgeSubject from "
				+ sourceNodeRid 
				+ conditionalString
				+ " ) where $depth = 2";
		} else {
			query= "select $path from "
					+ "(traverse out_edgeSubject, out_edgeObject from "
					+ sourceNodeRid 
					+ conditionalString
					+ " ) where $depth = 2";
		}
		
		//System.out.println("query = " + query);

		List<ODocument> paths = _ndexDatabase
				.query(new OSQLSynchQuery<ODocument>(query));

		List<ORID[]> results = new ArrayList<ORID[]>();

		for (ODocument doc : paths) {
			String path = (String) doc.fieldValues()[0];
			String[] components = path.split("\\.");
			ORID edgeRid = stringToRid(components[2]);
			ORID nodeRid = stringToRid(components[4]);
			ORID[] pair = { edgeRid, nodeRid };
			results.add(pair);
			//System.out.println("edgeId = " + pair[0] + " nodeRid = " + pair[1]);
		}
		if (upstream){
			//System.out.println("stQuery upstream results : " + results.size() + " at " + edgeRidStack.size());
		} else {
			//System.out.println("stQuery downstream results : " + results.size() + " at " + edgeRidStack.size());
		}

		return results;
	}

	/*
	 * 
	 * Common Utilities
	 */

	private void checkNetworkExists(ORID networkRid, String networkId)
			throws ObjectNotFoundException {
		if (null == _ndexDatabase.load(networkRid)) {
			throw new ObjectNotFoundException("Network", networkId);
		}
	}

	private ORID checkAndConvertNetworkId(String networkId) {
		if (networkId == null || networkId.isEmpty())
			throw new IllegalArgumentException("No network ID was specified.");
		return IdConverter.toRid(networkId);
	}

	private void checkBlockSize(int blockSize) {
		if (blockSize < 1)
			throw new IllegalArgumentException(
					"Number of results to return is less than 1.");
	}

	private ORID stringToRid(String ridString) {
		final Matcher m = Pattern.compile("^#(\\d*):(\\d*)$").matcher(
				ridString.trim());

		if (m.matches())
			return new ORecordId(Integer.valueOf(m.group(1)),
					OClusterPositionFactory.INSTANCE.valueOf(m.group(2)));
		else
			throw new IllegalArgumentException(ridString
					+ " is not a valid RID.");

	}

	private static String ridsToCsv(Collection<ORID> rids) {
		String resultString = "";
		if (null == rids || rids.size() < 1)
			return resultString;
		for (final ORID rid : rids) {
			resultString += rid + ", ";
		}
		resultString = resultString.substring(0, resultString.length() - 2);
		return resultString;
	}

	private String stringsToCsv(Collection<String> strings) {
		String resultString = "";
		for (final String string : strings) {
			resultString += "'" + string + "',";
		}
		resultString = resultString.substring(0, resultString.length() - 1);
		return resultString;

	}
	
	private void printItems(String prefix, List<Object> items, boolean printAll){
		if (printAll){
			String printString = "";
			for (Object item : items){
				printString += item + " . ";
			}
			System.out.println(prefix + " (" + items.size() + ")[ " + printString +  " ]");
		} else {
			System.out.println(prefix + " (" + items.size() + ")");
		}
		
		
	}



}
