package org.ndexbio.common.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.NetworkSourceFormat;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.models.dao.orientdb.Helper;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.PropertyGraphNetwork;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.SimplePathQuery;
import org.ndexbio.model.object.User;

import com.orientechnologies.orient.core.command.traverse.OTraverse;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.OClusterPositionFactory;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexException;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

public class NetworkAOrientDBDAO extends NdexAOrientDBDAO  {

	private static NetworkAOrientDBDAO INSTANCE = null;
	
	private static final Logger _logger = Logger.getLogger(NetworkAOrientDBDAO.class.getName());
	
//	private Class stringlist = new ArrayList<String>().getClass();

	protected NetworkAOrientDBDAO() {
		super();
	}
	
	public static synchronized NetworkAOrientDBDAO getInstance() {
	      if(INSTANCE == null) {
	         INSTANCE = new NetworkAOrientDBDAO();
	      }
	      return INSTANCE;
	   }

	/*
	 * Returns a block of BaseTerm objects in the network specified by
	 * networkId. 'blockSize' specified the number of terms to retrieve in the
	 * block, 'skipBlocks' specifies the number of blocks to skip.")
	 */
	/* (non-Javadoc)
	 * @see org.ndexbio.common.access.NetworkADAO#getTerms(java.lang.String, int, int)
	 */
	public List<BaseTerm> getBaseTerms(User user, final String networkId,
			final int skipBlocks, final int blockSize)
			throws IllegalArgumentException, NdexException {
		
		NetworkDAO dao = new NetworkDAO(this._ndexDatabase);
		ODocument networkDoc = dao.getNetworkDocByUUIDString(networkId);
		
		final ORID networkRid = networkDoc.getIdentity();
		checkBlockSize(blockSize);

		final List<BaseTerm> foundTerms = new ArrayList<>();
		final int startIndex = skipBlocks * blockSize;

		final String query = "SELECT name, id, out_baseTermNamespace.jdexId as namespaceId FROM (TRAVERSE out_networkTerms FROM "
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
				BaseTerm bterm = new BaseTerm();
				bterm.setId( (long)doc.field("jdexId"));
				bterm.setName( (String)doc.field("name"));
				bterm.setNamespaceId((long)doc.field("namespaceId"));

				foundTerms.add(bterm);
			}
			return foundTerms;
		} catch (ObjectNotFoundException onfe) {
			throw onfe;
		} catch (Exception e) {
			_logger.severe("Failed to query network: " + networkId + ". " + e.getMessage());
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
				NetworkDAO dao = new NetworkDAO(this._ndexDatabase);
				ODocument networkDoc = dao.getNetworkDocByUUIDString(networkId);
				
				//final ORID networkRid = networkDoc.getIdentity();

				Network network =  getSubnetworkByEdgeRids( edgeRids, networkDoc);
				final long getSubnetworkTime = System.currentTimeMillis();
				System.out.println("  Network Nodes : " + network.getNodes().size());
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
	
	public List<Edge> queryForEdges(final String networkId,
			final SimplePathQuery parameters, final int skipBlocks,
			final int blockSize) throws IllegalArgumentException, NdexException {
		try {
			setup();
			NetworkDAO dao = new NetworkDAO(this._ndexDatabase);
			ODocument networkDoc = dao.getNetworkDocByUUIDString(networkId);
			
			final ORID networkRid = networkDoc.getIdentity();
			checkBlockSize(blockSize);
			
			Set<ORID> edgeRids = queryForEdgeRids(networkRid, parameters
			//		,skipBlocks, blockSize
					);
			
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
	
	public Network queryForSubnetwork(final String networkId,
			final SimplePathQuery parameters
		//	,final int skipBlocks, final int blockSize
			) throws IllegalArgumentException, NdexException {

		try {
			setup();
			NetworkDAO dao = new NetworkDAO(this._ndexDatabase);
			ODocument networkDoc = dao.getNetworkDocByUUIDString(networkId);
			
			final ORID networkRid = networkDoc.getIdentity();
			
			final long startTime = System.currentTimeMillis();
			System.out.println("Starting subnetworkQuery ");
			Set<ORID> edgeRids = queryForEdgeRids(
					networkRid, 
					parameters 
					//,skipBlocks,blockSize
					);
			final long getEdgesTime = System.currentTimeMillis();
			Network network =  getSubnetworkByEdgeRids( edgeRids, networkDoc);
			final long getSubnetworkTime = System.currentTimeMillis();
			System.out.println("  Network Nodes : " + network.getNodes().size());
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

	public PropertyGraphNetwork queryForSubPropertyGraphNetwork(final String networkId,
			final SimplePathQuery parameters
			//,final int skipBlocks,	final int blockSize
			) throws IllegalArgumentException, NdexException {

		try {
			setup();
			NetworkDAO dao = new NetworkDAO(this._ndexDatabase);
			ODocument networkDoc = dao.getNetworkDocByUUIDString(networkId);
			
			final ORID networkRid = networkDoc.getIdentity();
			
			final long startTime = System.currentTimeMillis();
			System.out.println("Starting subnetworkQuery ");
			Set<ORID> edgeRids = queryForEdgeRids(
					networkRid, 
					parameters 
					//,skipBlocks,blockSize
					);
			final long getEdgesTime = System.currentTimeMillis();
			PropertyGraphNetwork network =  getSubPropertyGraphNetworkByEdgeRids( edgeRids);
			final long getSubnetworkTime = System.currentTimeMillis();
			System.out.println("     Network Nodes : " + network.getNodes().size());
			System.out.println("     Network Edges : " + network.getEdges().size());
			System.out.println("  Getting Edges MS : " + (getEdgesTime - startTime));
			System.out.println("Getting Network MS : " + (getSubnetworkTime - getEdgesTime));
			return network;
		} catch (Exception e) {
			e.printStackTrace();
			throw new NdexException("Error in queryForSubnetwork: " + e.getLocalizedMessage());
		} finally {
			teardown();
		}
	}

	
	private Set<ORID> getEdgeRids(ORID networkRid, int skipBlocks, int blockSize) {
		Set<ORID> result = new HashSet<>();

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


	private Set<ORID> queryForEdgeRids(
			ORID networkRid,
			SimplePathQuery parameters
		//	,int skipBlocks,int blockSize
			) throws NdexException {
		
		// Select query handler depending on parameters
		// TODO validate parameters
			String searchString = parameters.getSearchString();
//			List<String> searchTerms = Arrays.asList(searchString.split("[ ,]+"));
			
//			for (String term : searchTerms){
				System.out.println("termString:" + searchString);
				
//			}

			Collection<ORID> sourceNodeRids = getNodeRidsFromTermNames(networkRid,
					searchString,
					//parameters.getStartingTermStrings(),
					true); // change to
																// nodes
			List<ORID> includedPredicateRids = new ArrayList<>();
			// getBaseTermRids(networkRid,parameters.getIncludedPredicateIds());
			int maxDepth = parameters.getSearchDepth();
			
			System.out.println("-----------------------------------------\n");
			System.out.println(" Neighborhood Query ");
			System.out.println("search string: " + searchString);
			System.out.println("-----------------------------------------\n");

			return edgeIDsByTraversalFromSourceNode(networkRid, sourceNodeRids,
					includedPredicateRids, maxDepth);
//		}
//		return new HashSet<ORID>();
	}

	private Collection<ORID> getBaseTermRidsFromNames(ORID networkRid,
			String searchString) {

		Set<ORID> result = new TreeSet<>();

		// select from (traverse out_networkTerms from #24:733
		// while $depth < 2) where @CLASS='baseTerm' and name like "AKT%" limit
		// 10
		
		OIndex<?> basetermIdx = _ndexDatabase.getMetadata().getIndexManager().getIndex(NdexClasses.Index_BTerm_name);
		
		
		for (OIdentifiable termOID : (Collection<OIdentifiable>) basetermIdx.get( searchString)) {
			ODocument termDoc = (ODocument) termOID.getRecord();
			OIdentifiable tnId = termDoc.field("in_" + NdexClasses.Network_E_BaseTerms);
			if ( tnId != null && tnId.getIdentity().equals(networkRid)) {
				result.add(termOID.getIdentity());
			}
		}
		
		return result;

	}


	private Collection<ORID> getNodeRidsFromTermNames(ORID networkRid,String searchString, boolean includeAliases ) throws NdexException {
		
		
		Set<ORID> result = new TreeSet<>();
		
	  try {	
		OTraverse traverser = new OTraverse()
  			.fields("in_" + NdexClasses.FunctionTerm_E_paramter, "in_" + NdexClasses.Node_E_represents)
  			.target(getBaseTermRidsFromNames(networkRid, searchString));
		if ( includeAliases)
			traverser.field("in_" + NdexClasses.Node_E_alias);
		
		for (OIdentifiable nodeRec : traverser) {
 
			ODocument doc = (ODocument) nodeRec;
  
			if ( doc.getClassName().equals(NdexClasses.Node)) 
				result.add(nodeRec.getIdentity());
		}
		
	    OIndex<?> nodeNameIdx = _ndexDatabase.getMetadata().getIndexManager().getIndex(NdexClasses.Index_node_name);
				
	 	for (OIdentifiable termOID : (Collection<OIdentifiable>) nodeNameIdx.get( searchString)) {
			ODocument termDoc = (ODocument) termOID.getRecord();
			OIdentifiable tnId = termDoc.field("in_" + NdexClasses.Network_E_Nodes);
			if ( tnId != null && tnId.getIdentity().equals(networkRid)) {
				result.add(termOID.getIdentity());
			}
		}

		return result;
	  } catch (OIndexException e1) {
		  throw new NdexException ("Invalid search string. " + e1.getCause().getMessage());
	  }
	}

	/*
	 * result.add(new Node( node.field("jdexId"), node.field("name"),
	 * node.field("representsId"), node.field("metadata")));
	 */

	private List<Edge> getEdgesByEdgeRids(Set<ORID> edgeRids) {
		List<Edge> result = new ArrayList<>();
		String edgeIdCsv = ridsToCsv(edgeRids);

		final String query = 
				"SELECT id, in_edgeSubject.id as sId, out_edgePredicate.id as pId, out_edgeObject.id as oId FROM "
				+ "[ " + edgeIdCsv + " ] ";
		// System.out.println("node query: " + query);
		final List<ODocument> edges = _ndexDatabase
				.query(new OSQLSynchQuery<ODocument>(query));
		for (final ODocument doc : edges) {
			Edge edge = new Edge();
			edge.setId((Long) doc.field("id"));
			edge.setSubjectId((Long) doc.field("sId"));
			edge.setPredicateId((Long) doc.field("pId"));
			edge.setObjectId((Long) doc.field("oId"));
			result.add(edge);
		}
		return result;
	}

	private Network getSubnetworkByEdgeRids( Set<ORID> edgeRids, ODocument networkDoc) throws Exception {
		
	    Network network = new Network(edgeRids.size());  //result holder
	    
	    NetworkDAO dao = new NetworkDAO (this._ndexDatabase);

	    // get namespaces from network
        for ( Namespace ns : NetworkDAO.getNamespacesFromNetworkDoc(networkDoc, network)) {
        	network.getNamespaces().put(ns.getId(),ns);
        }


        for (ORID edgeId : edgeRids) {
            ODocument doc = new ODocument(edgeId);
            Edge e = dao.getEdgeFromDocument(doc,network);
            network.getEdges().put(e.getId(), e);
        }
        
        // copy the source format
        NetworkSourceFormat fmt = Helper.getSourceFormatFromNetworkDoc(networkDoc);
        if ( fmt!=null)
        	network.getProperties().add(new NdexPropertyValuePair(NdexClasses.Network_P_source_format, fmt.toString()));
        
        network.setNodeCount(network.getNodes().size());
        network.setEdgeCount(network.getEdges().size());
		 return network; 
	}

	private PropertyGraphNetwork getSubPropertyGraphNetworkByEdgeRids(Set<ORID> edgeRids) throws Exception {
		
	    PropertyGraphNetwork network = new PropertyGraphNetwork();  //result holder
    
	    TreeMap<ORID, String> termStringMap = new TreeMap<> ();
	    
	    NetworkDAO dao = new NetworkDAO (this._ndexDatabase);
        for (ORID edgeId : edgeRids) {
            ODocument doc = new ODocument(edgeId);
            dao.fetchPropertyGraphEdgeToNetwork(doc, network, termStringMap);
        }
        
		 return network; 
	}

	// This is the basis for a neighborhood query
	// post 1.0 
	private Set<ORID> edgeIDsByTraversalFromSourceNode(ORID networkRid,
			Collection<ORID> sourceNodeRids, List<ORID> includedPredicateRids,
			int maxDepth) {

		Set<ORID> foundEdgeRids = new HashSet<>();
		Set<ORID> foundNodeRids = new HashSet<>();

			
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
			Collection<ORID> sourceNodeRids,
			List<ORID> includedPredicateRids, 
			int depth,
			int maxDepth,
			Set<ORID> foundEdgeRids, 
			Set<ORID> foundNodeRids) {
		boolean belowMaxDepth = depth < maxDepth;
		System.out.println("search from  : " + sourceNodeRids.size() + " nodes at depth " + depth + " starting with foundEdges " + foundEdgeRids.size());
		
		List<ORID[]> results = sourceSearchQuery(sourceNodeRids, includedPredicateRids, false);

		results.addAll(sourceSearchQuery(sourceNodeRids, includedPredicateRids, true));
		
		Set<ORID> nextSourceNodeRids = new HashSet<>();
		
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
			Collection<ORID> sourceNodeRids,
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

		List<ORID[]> results = new ArrayList<>();

		for (ODocument doc : paths) {
			String path = (String) doc.fieldValues()[0];
			String[] components = path.split("\\.");
			ORID edgeRid = stringToRid(components[1]);
			ORID nodeRid = stringToRid(components[2]);
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

	private static ORID checkAndConvertNetworkId(String networkId) {
		if (networkId == null || networkId.isEmpty())
			throw new IllegalArgumentException("No network ID was specified.");
		return new ORecordId(networkId);
	}

	private static void checkBlockSize(int blockSize) {
		if (blockSize < 1)
			throw new IllegalArgumentException(
					"Number of results to return is less than 1.");
	}

	private static ORID stringToRid(String ridString) {
		final Matcher m = Pattern.compile("^.*#(\\d+):(\\d+).*$").matcher(
				ridString.trim());

		if (m.matches())
			return new ORecordId(Integer.valueOf(m.group(1)),
					OClusterPositionFactory.INSTANCE.valueOf(m.group(2)));
		
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

}
