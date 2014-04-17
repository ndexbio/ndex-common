package org.ndexbio.common.models.dao;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.ndexbio.common.exceptions.NdexException;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestFeedbackDAO extends TestDAO
{
    private static final FeedbackDAO dao =
			DAOFactorySupplier.INSTANCE.resolveDAOFactoryByType(CommonDAOValues.ORIENTDB_DAO_TYPE)
			.get().getFeedbackDAO();

    
    
    //@Test(timeout=5000)
    public void emailFeedback()
    {
        try
        {
            dao.emailFeedback("Question", "Does this test work?", testUserId );
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void emailFeedbackInvalidType() throws IllegalArgumentException, NdexException
    {
        dao.emailFeedback(null, "This feedback has no type.", testUserId );
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void emailFeedbackInvalidFeedback() throws IllegalArgumentException, NdexException
    {
        dao.emailFeedback("Bug", "", testUserId);

    }
    
    @Test(expected = IllegalArgumentException.class)
    public void emailFeedbackInvalidUserId () throws IllegalArgumentException, NdexException
    {
        dao.emailFeedback("Bug", "XXXXXXXXXX","");

    }
    
}
