package org.ndexbio.common.access;

import java.util.List;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.object.BaseTerm;
import org.ndexbio.common.models.object.Edge;
import org.ndexbio.common.models.object.Network;
import org.ndexbio.common.models.object.NetworkQueryParameters;
import org.ndexbio.common.models.object.User;

public interface NetworkADAO {

	/*
	 * Returns a block of BaseTerm objects in the network specified by
	 * networkId. 'blockSize' specified the number of terms to retrieve in the
	 * block, 'skipBlocks' specifies the number of blocks to skip.")
	 */
	public abstract List<BaseTerm> getTerms(User user, String networkId, int skipBlocks,
			int blockSize) throws IllegalArgumentException, NdexException;

	public abstract List<Edge> queryForEdges(User user,String networkId,
			NetworkQueryParameters parameters, int skipBlocks, int blockSize)
			throws IllegalArgumentException, NdexException;

	public abstract Network queryForSubnetwork(User user,String networkId,
			NetworkQueryParameters parameters, int skipBlocks, int blockSize)
			throws IllegalArgumentException, NdexException;

}