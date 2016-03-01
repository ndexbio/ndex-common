/**
 * Copyright (c) 2013, 2016, The Regents of the University of California, The Cytoscape Consortium
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package org.ndexbio.common.models.dao.orientdb.helper;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.Citation;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.Node;
import org.ndexbio.model.object.network.Support;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class NetworkUtility {

	private static Joiner csvJoiner = Joiner.on(',').skipNulls();
	
	public static String joinEdgeIdsToCsv(Collection<Edge> iEdges) {
		String resultString = "";
		if (iEdges.size() < 1) return resultString;
		for (final Edge iEdge: iEdges) {
			resultString += iEdge.getId() + ", ";
		}
		resultString = resultString.substring(0, resultString.length() - 2);
		return resultString;
	}

	public static String joinNodeIdsToCsv(List<Node> iNodes) {
		String resultString = "";
		if (iNodes.size() < 1) return resultString;
		for (final Node iNode: iNodes) {
			resultString += iNode.getId() + ", ";
		}
		resultString = resultString.substring(0, resultString.length() - 2);
		return resultString;
	}

	public static String joinCitationIdsToCsv(List<Citation> iCitations) {
		String resultString = "";
		if (iCitations.size() < 1) return resultString;
		for (final Citation iCitation: iCitations) {
			resultString += iCitation.getId() + ", ";
		}
		resultString = resultString.substring(0, resultString.length() - 2);
		return resultString;
	}

	/**************************************************************************
	 * join together an array of strings to create a quoted, comma-separated
	 * string
	 * 
	 * @param strings
	 * @return resultString
	 **************************************************************************/
	public static String joinStringsToCsv(String[] strings) {
		return joinStringsToCsv(Arrays.asList(strings));
	
	}
	

	public static String joinStringsToCsv(Collection<String> values) {
		
		Preconditions.checkArgument(null != values && values.size()>0,
				"Missing input values");
		List<String> quotedValues = Lists.newArrayList(Iterables.transform(values,
				new Function<String,String>(){
					@Override
					public String apply(String value) {
						return new StringBuilder().append("'").append(value).append("'").toString();
					}}));
		return csvJoiner.join(quotedValues);
	}
	

	/**************************************************************************
	 * process a list of VertexFrames to create a quoted, comma-separated string
	 * of their ids - suitable for incorporation in a query
	 * 
	 * @param vertexFrames
	 * @return resultString
	 **************************************************************************/
	public static  String joinBaseTermIdsToCsv(List<BaseTerm> iBaseTerms) {
		String resultString = "";
		if (iBaseTerms.size() < 1) return resultString;
		for (final BaseTerm iBaseTerm : iBaseTerms) {
			resultString += iBaseTerm.getId() + ", ";
		}
		resultString = resultString.substring(0, resultString.length() - 2);
		return resultString;
	}

	public  static Set<Long> getEdgeSupports(final Collection<Edge> edges) {
		final Set<Long> edgeSupports = new HashSet<>();
	
		for (final Edge edge : edges) {
			for (final Long support : edge.getSupportIds())
				edgeSupports.add(support);
		}
	
		return edgeSupports;
	}

	public static Set<Long> getEdgeCitations(final Collection<Edge> edges,
			final Collection<Support> supports) {
		final Set<Long> edgeCitations = new HashSet<>();
		for (final Edge edge : edges) {
			for (final Long citation : edge.getCitationIds())
				edgeCitations.add(citation);
		}
	
		for (final Support support : supports) {
			if (support.getCitationId() >0 ) 
				edgeCitations.add(support.getCitationId());
		}
		return edgeCitations;
	}

	public static Set<Long> getEdgeNodes(final Collection<Edge> edges) {
		final Set<Long> edgeNodes = new HashSet<>();
	
		for (final Edge edge : edges) {
			edgeNodes.add(edge.getSubjectId());
			edgeNodes.add(edge.getObjectId());
		}
	
		return edgeNodes;
	}

	public static Set<Long> getEdgeTerms(final Collection<Edge> edges,
			final Collection<Node> nodes) {
		final Set<Long> edgeTerms = new HashSet<>();
	
		if (null != edges){
			for (final Edge edge : edges)
				edgeTerms.add(edge.getPredicateId());
		}
		
/*		for (final Node node : nodes) {
			if (node.getRepresents() != null){
				NetworkUtility.addTermAndFunctionalDependencies(node.getRepresents(),
						edgeTerms);
			} else {
				System.out.println("missing represents for node " + node.getJdexId());
			}
			if (node.getAliases() != null) {
				for (ITerm iTerm : node.getAliases()) {
					NetworkUtility.addTermAndFunctionalDependencies(iTerm, edgeTerms);
				}
			}
			if (node.getRelatedTerms() != null) {
				for (ITerm iTerm : node.getRelatedTerms()) {
					NetworkUtility.addTermAndFunctionalDependencies(iTerm, edgeTerms);
				}
			}
		}
*/	
		return edgeTerms;
	}
/*
	public static void addTermAndFunctionalDependencies(final ITerm term,
			final Set<ITerm> terms) throws NdexException {
		if (terms.add(term)) {
			
			if (term instanceof IFunctionTerm) {
				terms.add(((IFunctionTerm) term).getTermFunc());
				//System.out.println("Object Model: Added Function Term " + term.getJdexId());
				for (ITerm iterm : ((IFunctionTerm) term).getTermParameters()) {
					addTermAndFunctionalDependencies(iterm, terms);
				}
			} else if (term instanceof IReifiedEdgeTerm) {
				//System.out.println("Object Model: Added Reified Edge Term " + term.getJdexId());
			} else if (term instanceof IBaseTerm) {
				//System.out.println("Object Model: Added Base Term " + term.getJdexId() + " " + ((IBaseTerm)term).getName());
			} else {
				throw new NdexException("Unknown type for term " + term.getJdexId());
			}
		}
	}

	public static Set<INamespace> getTermNamespaces(
			final Set<ITerm> requiredITerms) {
		final Set<INamespace> namespaces = new HashSet<INamespace>();
	
		for (final ITerm term : requiredITerms) {
			if (term instanceof IBaseTerm
					&& ((IBaseTerm) term).getTermNamespace() != null)
				namespaces.add(((IBaseTerm) term).getTermNamespace());
		}
	
		return namespaces;
	}

	public static int safeLongToInt(long l) {
	    if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
	        throw new NdexException
	            (l + " cannot be cast to int without changing its value.");
	    }
	    return (int) l;
	}
*/
}
