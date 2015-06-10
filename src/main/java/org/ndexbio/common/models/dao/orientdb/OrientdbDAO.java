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

	public static final int maxRetries = 100; 
	
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
		
		try {
			OIndex<?> Idx;
			OIdentifiable record = null;
			
			Idx = this.db.getMetadata().getIndexManager().getIndex("index-external-id");
			OIdentifiable temp = (OIdentifiable) Idx.get(id);
			if((temp != null) )
				record = temp;
				
			if(record == null || ( orientClass !=null && !((ODocument)record.getRecord()).getClassName().equals(orientClass))) 
				throw new ObjectNotFoundException("[Class "+ orientClass + "] Object with ID: " + id.toString() + " doesn't exist.");
			
			return (ODocument) record.getRecord();
			
		}  catch (ObjectNotFoundException e) {
			logger.severe("Object with UUID " + id + " not found : " + e.getMessage());
			e.printStackTrace();
			throw new ObjectNotFoundException(e.getMessage());
			
		}  catch (Exception e) {
			logger.severe("Unexpected error on external object retrieval by UUID : " + e.getMessage());
			e.printStackTrace();
			throw new NdexException(e.getMessage());
		}
		
	}
	
	
	public ODatabaseDocumentTx getDBConnection() {
		return db;
	}
/*	
	protected ODocument getRecordByExternalId(UUID id) 
			throws ObjectNotFoundException, NdexException {
		
		try {
			OIndex<?> Idx;
			OIdentifiable record = null;
			
			Idx = this.db.getMetadata().getIndexManager().getIndex("index-external-id");
			OIdentifiable temp = (OIdentifiable) Idx.get(id.toString());
			if((temp != null) )
					record = temp;
				
			if(record == null) 
				throw new ObjectNotFoundException("Object", id.toString());
			
			return (ODocument) record.getRecord();
			
		}  catch (Exception e) {
			logger.severe("Unexpected error on external object retrieval by UUID : " + e.getMessage());
			e.printStackTrace();
			throw new NdexException(e.getMessage());
		}
		
	}
*/	
	
	public ODocument getRecordByAccountName(String accountName, String orientClass) 
			throws ObjectNotFoundException, NdexException {
		
		try {
			OIndex<?> Idx = this.db.getMetadata().getIndexManager().getIndex( NdexClasses.Index_accountName );
			OIdentifiable user = (OIdentifiable) Idx.get(accountName); // account to traverse by
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
}
