/**
 * Copyright (c) 2013, 2015, The Regents of the University of California, The Cytoscape Consortium
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package org.ndexbio.common.query;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.models.dao.orientdb.Helper;
import org.ndexbio.common.models.dao.orientdb.NetworkDocDAO;
import org.ndexbio.common.query.filter.orientdb.EdgeByEdgePropertyFilterODB;
import org.ndexbio.common.query.filter.orientdb.EdgeByNodePropertyFilterODB;
import org.ndexbio.common.query.filter.orientdb.EdgeCollectionQueryODB;
import org.ndexbio.common.util.TermStringType;
import org.ndexbio.common.util.TermUtilities;
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
		
		// check if the query is valid
		if ( query.getEdgeFilter() == null || query.getEdgeFilter().getPropertySpecifications().size() == 0) {
			if ( query.getNodeFilter() == null || query.getNodeFilter().getPropertySpecifications().size() == 0 )  {  //error
				throw new NdexException ("Invalid query object received. Both filters are empty.");
			}
		} 
		
		//TODO: optimize the case that when filter compiled to an empty list. Should just return empty collection without iteration.
		
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
			   EdgeByEdgePropertyFilter filter, ODocument networkDoc) throws NdexException {
		if ( filter == null) return null;
		
		EdgeByEdgePropertyFilterODB odbFilter = new EdgeByEdgePropertyFilterODB();

		for ( PropertySpecification spec : filter.getPropertySpecifications()) {
			String value = spec.getValue();
			String propName = spec.getName();
			if ( propName.equalsIgnoreCase(edgePredicatePropertyName) ) {
				Iterable<ODocument> bTerms = Helper.getNetworkElements(networkDoc, NdexClasses.Network_E_BaseTerms);
				if ( bTerms !=null) {
					for ( ODocument d : bTerms) {
						String name = d.field(NdexClasses.BTerm_P_name);
						if ( name !=null && name.equalsIgnoreCase(value)) {
							odbFilter.addPredicateId((Long)d.field(NdexClasses.Element_ID));
						}
					}
				}
			} else {  // normal properties
				//TODO: need to reimplement this
/*				for ( ODocument baseTermDoc : Helper.getNetworkElements(networkDoc, NdexClasses.Network_E_BaseTerms)) {
					if (propertyNameMatchesBaseterm(propName, baseTermDoc) ) {
					   for ( ODocument prop : Helper.getDocumentLinks(baseTermDoc, "in_", NdexClasses.ndexProp_E_predicate)) {
							   String v = prop.field(NdexClasses.ndexProp_P_value);
							   if ( v.equalsIgnoreCase(value)) {
								   odbFilter.addPropertyId(prop.getIdentity().toString());
							   }
					   }
					}
				} */
			}
		}
		
		return odbFilter;
	}

	private static EdgeByNodePropertyFilterODB preprocessEdgeByNodePropertyFilter(
			   EdgeByNodePropertyFilter filter, ODocument networkDoc) throws NdexException {
		if ( filter == null) return null;
		
		EdgeByNodePropertyFilterODB odbFilter = new EdgeByNodePropertyFilterODB();
		odbFilter.setMode(filter.getMode());
		
		for (PropertySpecification spec: filter.getPropertySpecifications()) {
			String value = spec.getValue();
			String propName = spec.getName();
			if ( propName.equalsIgnoreCase(nodePropertyNodeName) ) {
		       odbFilter.addNodeName(value);
			} else if (propName.equalsIgnoreCase(nodePropertyNameORTerm)) {
			       odbFilter.addNodeName(value);
					for ( ODocument d : Helper.getNetworkElements(networkDoc, NdexClasses.Network_E_BaseTerms)) {
							String name = d.field(NdexClasses.BTerm_P_name);
							if ( name !=null && name.equalsIgnoreCase(value)) {
								odbFilter.addRepresentTermID((Long)d.field(NdexClasses.Element_ID));
							}
					}
			} else if (propName.equalsIgnoreCase(nodePropertyFunctionTermType)) {
				//TODO: need to reimplement this
				/*	for ( ODocument funcTermDoc : Helper.getNetworkElements(networkDoc, NdexClasses.Network_E_FunctionTerms)) {
						Long fBTermId = funcTermDoc.field(NdexClasses.BaseTerm);
						String name = fBTerm.field(NdexClasses.BTerm_P_name);
						if ( name !=null && name.equalsIgnoreCase(value)) {
							odbFilter.addRepresentTermID(funcTermDoc.getIdentity().toString());
						}
					} */
			} else {  // normal property
				for ( ODocument baseTermDoc : Helper.getNetworkElements(networkDoc, NdexClasses.Network_E_BaseTerms)) {
					//TODO: need to reimplement this.
					/*	
					if (propertyNameMatchesBaseterm(propName, baseTermDoc)) {
					   for ( ODocument prop : Helper.getDocumentLinks(baseTermDoc, "in_", NdexClasses.ndexProp_E_predicate)) {
						   String v = prop.field(NdexClasses.ndexProp_P_value);
						   if ( v.equalsIgnoreCase(value)) {
								   odbFilter.addPropertyId(prop.getIdentity().toString());
						   }
					   }
					}  */
				}
			}
		}
		return odbFilter;
	}

	private static boolean propertyNameMatchesBaseterm(String propertyName, ODocument baseTermDoc) throws NdexException {

		String name = baseTermDoc.field(NdexClasses.BTerm_P_name);
		
		if ( name == null) return false;
		
		TermStringType termType = TermUtilities.getTermType(propertyName);
		
		switch (termType) {
		case URI:
			throw new NdexException("URI type baseterm search not implemented yet.");
	//		break;
		case CURIE:
			String[] qname =TermUtilities.getNdexQName(propertyName);
			if ( ! name.equalsIgnoreCase(qname[1]) ) return false;
			
			Long nsId = baseTermDoc.field(NdexClasses.BTerm_NS_ID);
			try (NetworkDocDAO dao = new NetworkDocDAO()) {
				ODocument nsDoc = dao.getDocumentByElementId(nsId);
				String prefix = nsDoc.field(NdexClasses.ns_P_prefix);
				if (prefix !=null && prefix.equalsIgnoreCase(qname[0])) 
					  return true;	 
			}
			break;
		case NAME:
			if ( name.equalsIgnoreCase(propertyName)) 
				return true;
			break;
		default:
			break;
		}

		
		return false;
		
	}
	
}
