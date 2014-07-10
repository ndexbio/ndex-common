package org.ndexbio.common.models.dao;

import org.ndexbio.common.exceptions.NdexException;

@Deprecated
public interface FeedbackDAO {

	/**************************************************************************
	 * Emails feedback to Cytoscape Consortium.
	 * 
	 * @param feedbackType
	 *            The type of feedback being given.
	 * @param feedbackText
	 *            The feedback.
	 * @throws IllegalArgumentException
	 *            Bad input.
	 * @throws NdexException
	 *            Failed to send the email. 
	 **************************************************************************/
	public abstract void emailFeedback(String feedbackType, String feedbackText, String userId)
			throws IllegalArgumentException, NdexException;

}