package org.ndexbio.common.models.dao;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.model.object.NdexStatus;

@Deprecated
public interface AdminDAO {

	/**************************************************************************
	 * 
	 * Gets status for the service.
	 **************************************************************************/

	public abstract NdexStatus getStatus() throws NdexException;

}