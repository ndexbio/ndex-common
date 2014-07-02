package org.ndexbio.common.models.dao;

import org.ndexbio.common.models.dao.orientdb.ObjectFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

public enum DAOFactorySupplier {
	INSTANCE;


	public Supplier<ObjectFactory> resolveDAOFactoryByType(String type){
		Preconditions.checkArgument(!Strings.isNullOrEmpty(type), "A DAO type is required");
		switch(type) {
		case CommonDAOValues.ORIENTDB_DAO_TYPE:
			return Suppliers.memoize(new OrientdbDAOFactorySupplier() );
		default:
			System.out.println("DAO type " + type +" is not supported.");
		}
		return null;
		
	}
	
  public class OrientdbDAOFactorySupplier implements Supplier<ObjectFactory> {
	@Override
	public ObjectFactory get() {
		return new org.ndexbio.common.models.dao.orientdb.ObjectFactory();
	}
	  
  }
}
