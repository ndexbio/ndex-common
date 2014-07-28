/**
 * 
 */
package org.ndexbio.common.models.dao.orientdb;

import javax.mail.MessagingException;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.helpers.Configuration;
import org.ndexbio.common.models.dao.FeedbackDAO;
import org.ndexbio.common.util.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;


/**
 * @author fcriscuo
 *
 */
public class FeedbackOrientdbDAO extends OrientdbDAO implements FeedbackDAO {

	
	private  final Logger logger = LoggerFactory.getLogger(FeedbackOrientdbDAO.class);
	
	private FeedbackOrientdbDAO() {super();}
	
	static FeedbackOrientdbDAO createInstance() { return new FeedbackOrientdbDAO();}
	
	@Override
	public void emailFeedback(String feedbackType, String feedbackText, String userId)
			throws IllegalArgumentException, NdexException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(feedbackType), "A feedback type is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(feedbackText), 
				"Feedback text is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId), 
				"A user id is required");
        
        try
        {
        	this.setupDatabase();
   /*     	final User user = this.findIuserById(userId);
            if (user != null)
            {
                Email.sendEmail(user.getEmailAddress(),
                    Configuration.getInstance().getProperty("Feedback-Email"),
                    feedbackType,
                    feedbackText);
            }
            else
            {
                Email.sendEmail(Configuration.getInstance().getProperty("Feedback-Email"),
                    Configuration.getInstance().getProperty("Feedback-Email"),
                    feedbackType,
                    feedbackText);
            }*/
        } finally {
        	this.teardownDatabase();
        }
    }

	

}
