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
