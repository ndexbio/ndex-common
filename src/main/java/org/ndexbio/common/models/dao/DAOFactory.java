package org.ndexbio.common.models.dao;

@Deprecated
public abstract class DAOFactory {
	

	public abstract AdminDAO getAdminDAO();
	public abstract FeedbackDAO getFeedbackDAO();
	public abstract GroupDAO getGroupDAO();
	public abstract NetworkDAO getNetworkDAO();
	public abstract RequestDAO getRequestDAO() ;
	public abstract TaskDAO getTaskDAO();
	public abstract UserDAO getUserDAO();
	
	

}
