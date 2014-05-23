/**
 * 
 */
package org.ndexbio.common.models.dao.orientdb;

import java.util.List;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.dao.AdminDAO;
import org.ndexbio.common.models.object.NdexStatus;

import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

/**
 * @author fcriscuo
 *
 */
public class AdminOrientdbDAO extends OrientdbDAO implements AdminDAO {

	private AdminOrientdbDAO() {
		super();
	}
	
	static AdminOrientdbDAO createInstance() {
		return new AdminOrientdbDAO();
	}
	
	@Override
	public NdexStatus getStatus() throws NdexException {

		try {

			setupDatabase();
			NdexStatus status = new NdexStatus();
			status.setNetworkCount(this.getClassCount("network"));
			status.setUserCount(this.getClassCount("user"));
			status.setGroupCount(this.getClassCount("group"));
			return status;
		} finally {

			teardownDatabase();

		}

	}
	
	private Integer getClassCount(String className) {

		final List<ODocument> classCountResult = _ndexDatabase
				.query(new OSQLSynchQuery<ODocument>(
						"SELECT COUNT(*) as count FROM " + className));

		final Long count = classCountResult.get(0).field("count");

		Integer classCount = count != null ? count.intValue() : null;

		return classCount;

	}

}
