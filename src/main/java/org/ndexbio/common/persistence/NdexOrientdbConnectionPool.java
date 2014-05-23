package org.ndexbio.common.persistence;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

public enum NdexOrientdbConnectionPool {
	INSTANCE;
	private ConnectionPoolFactory  factory = new ConnectionPoolFactory();
	
	private ObjectPool<NdexOrientdbConnection> pool= new GenericObjectPool<NdexOrientdbConnection>(factory);
	
	public ObjectPool<NdexOrientdbConnection> getConnectionPool() {
		return this.pool;
	}
	
	public static class ConnectionPoolFactory extends BasePooledObjectFactory<NdexOrientdbConnection> {

		@Override
		public NdexOrientdbConnection create() throws Exception {
			return new NdexOrientdbConnection();
		}

		@Override
		public PooledObject<NdexOrientdbConnection> wrap(
				NdexOrientdbConnection conn) {
			return new DefaultPooledObject<NdexOrientdbConnection>(conn);
		}
		
	}

}
