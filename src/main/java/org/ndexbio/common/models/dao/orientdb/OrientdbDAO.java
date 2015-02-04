package org.ndexbio.common.models.dao.orientdb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Logger;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Permissions;

import com.orientechnologies.orient.core.command.traverse.OTraverse;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.filter.OSQLPredicate;
import com.tinkerpop.blueprints.Direction;

public abstract class OrientdbDAO implements AutoCloseable {

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

	//TODO: review the needs for parameter orientClass
	protected ODocument getRecordById(UUID id, String orientClass) 
			throws ObjectNotFoundException, NdexException {
		
		try {
			OIndex<?> Idx;
			OIdentifiable record = null;
			
//			for(String oclass : orientClass) {
					Idx = this.db.getMetadata().getIndexManager().getIndex("index-external-id");
					OIdentifiable temp = (OIdentifiable) Idx.get(id.toString());
					if((temp != null) )
						record = temp;
				
//			}
			
/*			if(orientClass.length > 0 && record == null) 
				throw new ObjectNotFoundException("Object", id.toString());
			
			if(orientClass.length > 0)
				return (ODocument) record.getRecord();
			
			if(orientClass.length == 0) {
				Idx = this.db.getMetadata().getIndexManager().getIndex("index-external-id");
				record = (OIdentifiable) Idx.get(id.toString());
			}
*/			
			if(record == null) 
				throw new ObjectNotFoundException("Object", id.toString());
			
			return (ODocument) record.getRecord();
			
		}  catch (Exception e) {
			logger.severe("Unexpected error on external object retrieval by UUID : " + e.getMessage());
			e.printStackTrace();
			throw new NdexException(e.getMessage());
		}
		
	}

	
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
	
	
	public ODocument getRecordByAccountName(String accountName, String orientClass) 
			throws ObjectNotFoundException, NdexException {
		
		try {
			OIndex<?> Idx = this.db.getMetadata().getIndexManager().getIndex( NdexClasses.Index_accountName );
			OIdentifiable user = (OIdentifiable) Idx.get(accountName); // account to traverse by
			if(user == null) 
				throw new ObjectNotFoundException("Account ", accountName);
			
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


}
