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
package org.ndexbio.task.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.Citation;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.xbel.splitter.AnnotationDefinitionGroupSplitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;



public class NdexJVMDataModelService implements NdexTaskModelService {
	
	 private NetworkDAO dao;
	
	 private static final Logger logger = LoggerFactory
				.getLogger(NdexJVMDataModelService.class);
	 
	 public NdexJVMDataModelService (ODatabaseDocumentTx db) {
		// ODatabaseDocumentTx db = NdexAOrientDBConnectionPool.getInstance().acquire();
		 dao= new NetworkDAO (db);

	 }

	@Override
	public Network getNetworkById(String networkUUID) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkUUID), 
				"A network id is required");
	
		try {
			return dao.getNetworkById(UUID.fromString(networkUUID));
		} catch (IllegalArgumentException | SecurityException | NdexException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public Collection<Citation> getCitationsByNetworkId(String networkId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), 
				"A network id is required");
		try {
			return dao.getNetworkCitations(networkId);
		} catch (IllegalArgumentException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		} 
		return new ArrayList<Citation>();
	}

	@Override
	public List<Edge> getEdgesBySupportId(String supportId) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * Predicate for filtering invalid namespaces from Namespace query
	 */
	Predicate<Namespace> namespacePredicate = new Predicate<Namespace>(){
		@Override
		public boolean apply(Namespace ns) {
			return ns.getProperties().isEmpty();
		}
		
	};
	
	/*
	 * Predicate to find xbel internal annotations from namespace query
	 */
	Predicate<Namespace> internalAnnotationPredicate = new Predicate<Namespace>() {
		@Override
		public boolean apply(Namespace ns) {
			for ( NdexPropertyValuePair p : ns.getProperties() ) {
				if ( p.getPredicateString().equals(AnnotationDefinitionGroupSplitter.property_Type)
						&& p.getValue().equals(AnnotationDefinitionGroupSplitter.internal_annotation_def))
              return true;
			}
			return false;
		}
		
	};
	
	/*
	 * Predicate to find xbel external annotations from namespace query
	 */
	Predicate<Namespace> externalAnnotationPredicate = new Predicate<Namespace>() {

		@Override
		public boolean apply(Namespace ns) {
			for ( NdexPropertyValuePair p : ns.getProperties()) {
				if ( p.getPredicateString().equals(AnnotationDefinitionGroupSplitter.property_Type)
						&& p.getValue().equals(AnnotationDefinitionGroupSplitter.external_annotation_def))
					return true;
			}
	
			return false;
		}
		
	};
	
	
	/*
	 * public method to get Namespaces for a network.
	 * Since both XBEL annotation definitions and namespaces are mapped to the
	 * NDEx Namespace vertex, this method filters out annotation definitions
	 * 
	 */
	@Override
	public Iterable<Namespace> getNamespacesByNetworkId(String networkId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId),
				"A network id is required");
		
		try {
			return  Iterables.filter(dao.getNetworkNamespaces(networkId), namespacePredicate);
			
		} catch (IllegalArgumentException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		return new ArrayList<Namespace>();
	}


	@Override
	public Network getSubnetworkByCitationId(String networkId,Long citationId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), 
				"A network id is required");
		try {
			return dao.getSubnetworkByCitation( networkId, citationId);
		} catch (IllegalArgumentException | NdexException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		
		return null;
	}


	@Override
	/*
	 * XBEL Internal Annotations are persisted as Namespace types in the database
	 * They are distinguished from External Annotations by not having a URI
	 * 
	 */
	public Iterable<Namespace> getInternalAnnotationsByNetworkId(
			String networkId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), 
				"A network id is required");
		List<Namespace> internalAnnotationList = Lists.newArrayList();
		try {
			return  Iterables.filter(dao.getNetworkNamespaces(networkId), internalAnnotationPredicate);
			
			
		} catch (IllegalArgumentException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		return internalAnnotationList;
	}


	@Override
	/*XBEL External Annotations are persisted as Namespace types in the database
	 * They are distinguished from internal annotations by having a URI property
	 * 
	 */
	public Iterable<Namespace> getExternalAnnotationsByNetworkId(
			String networkId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), 
				"A network id is required");
		List<Namespace> externalAnnotationList = Lists.newArrayList();
		try {
			return  Iterables.filter(dao.getNetworkNamespaces(networkId), externalAnnotationPredicate);
			
			
		} catch (IllegalArgumentException  e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		return externalAnnotationList;
	}
    /*
     * return a list of BaseTerms associated with a specified namespace
     * needed to resolve internal annotations
     */
	@Override
	public Collection<BaseTerm> getBaseTermsByNamespace(String userId, String namespacePrefix,String networkId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId), "A userId is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(namespacePrefix), "A namespace is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), "A network id is required");
		
		try {
			return dao.getBaseTermsByPrefix( networkId, namespacePrefix);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// return an empty list for error conditions
		List<BaseTerm> btList = Lists.newArrayList();
		return btList;
	}

	@Override
	public Network getNoCitationSubnetwork(String networkId) throws NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), 
				"A network id is required");
		try {
			return dao.getOrphanStatementsSubnetwork( networkId);
		} catch (IllegalArgumentException | NdexException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			throw new NdexException ("Error occurred when getting orphan statement subnetwork: " + e.getMessage());
		}
		
	}

	@Override
	public Network getOrphanSupportNetwork(String networkID) throws NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkID), 
				"A network id is required");
		try {
			return dao.getOrphanSupportsNetwork( networkID);
		} catch (IllegalArgumentException | NdexException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			throw new NdexException ("Error occurred when getting orphanSupport subnetwork: " + e.getMessage());
		}
	}

	
	
}
