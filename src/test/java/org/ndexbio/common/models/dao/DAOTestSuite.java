package org.ndexbio.common.models.dao;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(Suite.class)
@SuiteClasses({ TestTaskDAO.class, TestFeedbackDAO.class, TestGroupDAO.class, 
	 TestRequestDAO.class, TestUserDAO.class})
public class DAOTestSuite
{
}
