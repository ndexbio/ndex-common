/**
 * Copyright (c) 2013, 2016, The Regents of the University of California, The Cytoscape Consortium
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
package org.ndexbio.common.models.dao.orientdb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.exceptions.ObjectNotFoundException;
import org.ndexbio.model.object.Permissions;

import com.orientechnologies.orient.core.command.traverse.OTraverse;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.filter.OSQLPredicate;
import com.tinkerpop.blueprints.Direction;

public class OrientdbDAO implements AutoCloseable {

	
	protected ODatabaseDocumentTx db;
	private static final Logger logger = Logger.getLogger(OrientdbDAO.class.getName());

	public OrientdbDAO(ODatabaseDocumentTx connection) {
		this.db = connection;
	}

	/*
	 * resolving a user is a common requirement across all DAO classes
	 * 
	 */

	protected ODocument getRecordByUUID(UUID id, String orientClass) 
			throws ObjectNotFoundException, NdexException {
		return getRecordByUUIDStr(id.toString(), orientClass);
		
	}
	
	public ODocument getRecordByUUIDStr(String id, String orientClass) 
			throws ObjectNotFoundException, NdexException {
		
		//try {
			OIndex<?> Idx;
			OIdentifiable record = null;
			
			Idx = this.db.getMetadata().getIndexManager().getIndex(NdexClasses.Index_UUID);
			OIdentifiable temp = (OIdentifiable) Idx.get(id);
			if((temp != null) )
				record = temp;
				
			if(record == null || ( orientClass !=null && !((ODocument)record.getRecord()).getClassName().equals(orientClass))) 
				throw new ObjectNotFoundException("[Class "+ orientClass + "] Object with ID: " + id.toString() + " doesn't exist.");
			
			return (ODocument) record.getRecord();
			
//		}  
	/*catch (Exception e) {
			logger.severe("Unexpected error on external object retrieval by UUID : " + e.getMessage());
			e.printStackTrace();
			throw new NdexException(e.getMessage());
		} */
		
	}
	
	
	public ODatabaseDocumentTx getDBConnection() {
		return db;
	}

	
	public ODocument getRecordByAccountName(String accountName, String orientClass) 
			throws ObjectNotFoundException, NdexException {
		
		try {
			OIndex<?> Idx = this.db.getMetadata().getIndexManager().getIndex( NdexClasses.Index_accountName );
			OIdentifiable user = (OIdentifiable) Idx.get(accountName); 
			if(user == null)
				throw new ObjectNotFoundException("Account with ID: " + accountName + " doesn't exist.");
			
			if( orientClass != null && 
					!( (ODocument) user.getRecord() ).getSchemaClass().getName().equals( orientClass ) )
				throw new NdexException("accountName "+ accountName +" is not for class " + orientClass);
			
			return (ODocument) user.getRecord();
			
		} catch (ObjectNotFoundException e) {
			logger.info("Account " + accountName + " does not exist for class: " + orientClass + ". detail: " + e.getMessage());
			throw e;
		} catch (Exception e) {
			logger.severe("Unexpected error on user retrieval by accountName: " + e.getMessage());
			e.printStackTrace();
			throw new NdexException(e.getMessage());
		}
		
	} 


	public static boolean checkPermission(ORID source, ORID destination, Direction dir, Integer depth, Permissions... permissions) {
		
		Collection<Object> fields = new ArrayList<>();
		
		for(Permissions permission : permissions) {
			fields.add( dir.name().toLowerCase() + "_" + permission.name().toLowerCase() );
		}
		
		for(OIdentifiable id : new OTraverse().target(source)
											.fields(fields)
											.predicate( new OSQLPredicate("$depth <= "+depth.toString()) )
											.execute()) {
			if(id.getIdentity().equals(destination))
				return true;
		}
		return false;
	}
	
	@Override
	public void close () {
		db.close();
	}

    public void commit () {
    	db.commit();
    }
    
    // removing this function because OriendDB guys said we should not use is. Should just throw an exception when error occurs in a transaction.
 /*   public void rollback() {
    	db.rollback();
    } */
}
