package org.ndexbio.common.models.dao;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(Suite.class)
@SuiteClasses({ TestTaskDAO.class, TestFeedbackDAO.class, 
	 TestRequestDAO.class})
public class DAOTestSuite
{
}
