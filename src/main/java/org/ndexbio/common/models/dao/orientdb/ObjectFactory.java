package org.ndexbio.common.models.dao.orientdb;

import org.ndexbio.common.models.dao.AdminDAO;
import org.ndexbio.common.models.dao.DAOFactory;
import org.ndexbio.common.models.dao.FeedbackDAO;
import org.ndexbio.common.models.dao.GroupDAO;
import org.ndexbio.common.models.dao.NetworkDAO;
import org.ndexbio.common.models.dao.RequestDAO;
import org.ndexbio.common.models.dao.TaskDAO;
import org.ndexbio.common.models.dao.UserDAO;

public class ObjectFactory extends DAOFactory {

	@Override
	public AdminDAO getAdminDAO() {
		return AdminOrientdbDAO.createInstance();
	}

	@Override
	public FeedbackDAO getFeedbackDAO() {
		return  FeedbackOrientdbDAO.createInstance();
	}

	@Override
	public GroupDAO getGroupDAO() {
		return  GroupOrientdbDAO.createInstance();
	}

	@Override
	public NetworkDAO getNetworkDAO() {
		return NetworkOrientdbDAO.createInstance();
	}

	@Override
	public RequestDAO getRequestDAO() {
		return RequestOrientdbDAO.createInstance();
	}

	@Override
	public TaskDAO getTaskDAO() {
		return TaskOrientdbDAO.createInstance();
	}

	@Override
	public UserDAO getUserDAO() {
		return UserOrientdbDAO.createInstance();
	}

}
