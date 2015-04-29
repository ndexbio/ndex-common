package org.ndexbio.common.query;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.models.dao.orientdb.Helper;
import org.ndexbio.common.models.dao.orientdb.NetworkDocDAO;
import org.ndexbio.common.query.filter.orientdb.EdgeByEdgePropertyFilterODB;
import org.ndexbio.common.query.filter.orientdb.EdgeByNodePropertyFilterODB;
import org.ndexbio.common.query.filter.orientdb.EdgeCollectionQueryODB;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.network.query.EdgeByEdgePropertyFilter;
import org.ndexbio.model.network.query.EdgeByNodePropertyFilter;
import org.ndexbio.model.network.query.EdgeCollectionQuery;
import org.ndexbio.model.network.query.PropertySpecification;

import com.orientechnologies.orient.core.record.impl.ODocument;

public class NetworkFilterQueryExecutorFactory {
	
	private static final String edgePredicatePropertyName = "ndex:predicate";
	
	private static final String nodePropertyFunctionTermType = "ndex:functionTermType";
	private static final String nodePropertyNameORTerm = "ndex:nameOrTermName";
	private static final String nodePropertyNodeName = "ndex:nodeName";
	
	public static NetworkFilterQueryExecutor createODBExecutor(String networkIdStr, EdgeCollectionQuery query) throws NdexException {
		
		EdgeCollectionQueryODB edgeQuery = new EdgeCollectionQueryODB();
		edgeQuery.setQueryName(query.getQueryName());
		edgeQuery.setEdgeLimit(query.getEdgeLimit());
		
		try ( NetworkDocDAO networkDao = new NetworkDocDAO()) {
		
			ODocument networkDoc = networkDao.getNetworkDocByUUIDString(networkIdStr);
			
			EdgeByEdgePropertyFilterODB edgeFilter = preprocessEdgeByEdgePropertyFilter(
					   query.getEdgeFilter(), networkDoc)	;
			
			EdgeByNodePropertyFilterODB nodeFilter = preprocessEdgeByNodePropertyFilter(
					query.getNodeFilter(), networkDoc);
			
			edgeQuery.setEdgeFilter(edgeFilter);
			edgeQuery.setNodeFilter(nodeFilter);
		  NetworkFilterQueryExecutor executor = new NetworkFilterQueryExecutor(networkIdStr, edgeQuery);
		
		  return executor;
		}
	}

	
	private static EdgeByEdgePropertyFilterODB preprocessEdgeByEdgePropertyFilter(
			   EdgeByEdgePropertyFilter filter, ODocument networkDoc) {
		if ( filter == null) return null;
		
		EdgeByEdgePropertyFilterODB odbFilter = new EdgeByEdgePropertyFilterODB();

		for ( PropertySpecification spec : filter.getPropertySpecList()) {
			String value = spec.getValue();
			String propName = spec.getProperty();
			if ( propName.equalsIgnoreCase(edgePredicatePropertyName) ) {
				Iterable<ODocument> bTerms = Helper.getNetworkElements(networkDoc, NdexClasses.Network_E_BaseTerms);
				if ( bTerms !=null) {
					for ( ODocument d : bTerms) {
						String name = d.field(NdexClasses.BTerm_P_name);
						if ( name !=null && name.equalsIgnoreCase(value)) {
							odbFilter.addPredicateId(d.getIdentity().toString());
						}
					}
				}
			} else {
				for ( ODocument baseTermDoc : Helper.getNetworkElements(networkDoc, NdexClasses.Network_E_BaseTerms)) {
					String name = baseTermDoc.field(NdexClasses.BTerm_P_name);
					if ( name !=null && name.equalsIgnoreCase(propName)) {
						   for ( ODocument prop : Helper.getDocumentLinks(baseTermDoc, "in_", NdexClasses.ndexProp_E_predicate)) {
							   String v = prop.field(NdexClasses.ndexProp_P_value);
							   if ( v.equalsIgnoreCase(value)) {
								   odbFilter.addPropertyId(prop.getIdentity().toString());
							   }
						   }
					}
				}
			}
		}
		
		return odbFilter;
	}

	private static EdgeByNodePropertyFilterODB preprocessEdgeByNodePropertyFilter(
			   EdgeByNodePropertyFilter filter, ODocument networkDoc) {
		if ( filter == null) return null;
		
		EdgeByNodePropertyFilterODB odbFilter = new EdgeByNodePropertyFilterODB();
		odbFilter.setMode(filter.getMode());
		
		for (PropertySpecification spec: filter.getPropertySpecList()) {
			String value = spec.getValue();
			String propName = spec.getProperty();
			if ( propName.equalsIgnoreCase(nodePropertyNodeName) ) {
		       odbFilter.addNodeName(value);
			} else if (propName.equalsIgnoreCase(nodePropertyNameORTerm)) {
			       odbFilter.addNodeName(value);
					for ( ODocument d : Helper.getNetworkElements(networkDoc, NdexClasses.Network_E_BaseTerms)) {
							String name = d.field(NdexClasses.BTerm_P_name);
							if ( name !=null && name.equalsIgnoreCase(value)) {
								odbFilter.addRepresentTermID(d.getIdentity().toString());
							}
					}
			} else if (propName.equalsIgnoreCase(nodePropertyFunctionTermType)) {
					for ( ODocument funcTermDoc : Helper.getNetworkElements(networkDoc, NdexClasses.Network_E_FunctionTerms)) {
						ODocument fBTerm = funcTermDoc.field("out_"+NdexClasses.FunctionTerm_E_baseTerm);
						String name = fBTerm.field(NdexClasses.BTerm_P_name);
						if ( name !=null && name.equalsIgnoreCase(value)) {
							odbFilter.addRepresentTermID(funcTermDoc.getIdentity().toString());
						}
					}
			} else {  // normal property
				for ( ODocument baseTermDoc : Helper.getNetworkElements(networkDoc, NdexClasses.Network_E_BaseTerms)) {
					String name = baseTermDoc.field(NdexClasses.BTerm_P_name);
					if ( name !=null && name.equalsIgnoreCase(propName)) {
						   for ( ODocument prop : Helper.getDocumentLinks(baseTermDoc, "in_", NdexClasses.ndexProp_E_predicate)) {
							   String v = prop.field(NdexClasses.ndexProp_P_value);
							   if ( v.equalsIgnoreCase(value)) {
								   odbFilter.addPropertyId(prop.getIdentity().toString());
							   }
						   }
					}
				}
			}
		}
		return odbFilter;
	}

}
