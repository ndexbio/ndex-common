/**
 *   Copyright (c) 2013, 2015
 *  	The Regents of the University of California
 *  	The Cytoscape Consortium
 *
 *   Permission to use, copy, modify, and distribute this software for any
 *   purpose with or without fee is hereby granted, provided that the above
 *   copyright notice and this permission notice appear in all copies.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *   WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *   MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *   ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *   WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *   ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *   OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package org.ndexbio.common.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.TreeSet;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.models.dao.orientdb.Helper;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.common.models.dao.orientdb.NetworkDocDAO;
import org.ndexbio.common.models.dao.orientdb.NetworkSearchDAO;
import org.ndexbio.common.models.dao.orientdb.NetworkSearchDAO.NetworkResultComparator;
import org.ndexbio.common.models.dao.orientdb.UserDocDAO;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.network.query.NetworkPropertyFilter;
import org.ndexbio.model.network.query.PropertyFilter;
import org.ndexbio.model.network.query.PropertySpecification;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.Permissions;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.VisibilityType;

import com.google.common.base.Strings;
import com.orientechnologies.orient.core.command.traverse.OTraverse;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.filter.OSQLPredicate;

public class SearchNetworkByPropertyExecutor {

	Collection<PropertySpecification> filters;
	ORID userRID;
	ORID adminUserRID;
	
	public SearchNetworkByPropertyExecutor( NetworkPropertyFilter propertyFilter, String userAccount) throws NdexException {
		filters = propertyFilter.getProperties();
		
		try ( UserDocDAO userdao = new UserDocDAO() ) {
			if ( propertyFilter.getAdmin() != null ) {
				ODocument accDoc = userdao.getRecordByAccountName(propertyFilter.getAdmin(), NdexClasses.User);
				adminUserRID = accDoc.getIdentity();
			} else 
				adminUserRID = null;
		if ( userAccount == null )
			throw new NdexException ("userAccount can't be null in SearchNetworkByPropertyExecutor.");
		ODocument accDoc = userdao.getRecordByAccountName(userAccount, NdexClasses.User);
		userRID = accDoc.getIdentity();
		}
		
		
	}
	
	public Collection<NetworkSummary> evaluate() throws NdexException {
		
		Collection<NetworkSummary> result = new ArrayList<>();

		if ( filters == null || filters.size() == 0)
			return result;
		
		try (NetworkDAO dao = new NetworkDAO() ) {

			for (final ODocument networkDoc : dao.getDBConnection().browseClass(NdexClasses.Network)) {
				Boolean isComplete = networkDoc.field(NdexClasses.Network_P_isComplete);
				Boolean isDeleted = networkDoc.field(NdexClasses.ExternalObj_isDeleted);
				if ( isComplete !=null && isComplete.booleanValue() && !isDeleted.booleanValue()) {
					if ( NetworkSearchDAO.isSearchable(networkDoc, userRID, adminUserRID, false, true, Permissions.READ) )	{	
						   if ( networkSatisfyFilter(networkDoc))
							result.add(NetworkDocDAO.getNetworkSummary(networkDoc));
					}
				}
			} 

		
		}
		
		return result;
	}

		
	private boolean networkSatisfyFilter(ODocument networkDoc) {
		
		for ( ODocument propDoc : Helper.getDocumentLinks(networkDoc, "out_", NdexClasses.E_ndexProperties)) {
			NdexPropertyValuePair prop = Helper.getNdexPropertyFromDoc(propDoc);
			for ( PropertySpecification spec : filters) {
				if ( spec.getName().equalsIgnoreCase(prop.getPredicateString()) &&
						spec.getValue().equalsIgnoreCase((prop.getValue())) ) 
						return true;
			}
			
		}
		
		return false;
	}

	
}
