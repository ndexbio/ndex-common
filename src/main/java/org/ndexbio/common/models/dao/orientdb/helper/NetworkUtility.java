package org.ndexbio.common.models.dao.orientdb.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.data.IBaseTerm;
import org.ndexbio.common.models.data.ICitation;
import org.ndexbio.common.models.data.IEdge;
import org.ndexbio.common.models.data.IFunctionTerm;
import org.ndexbio.common.models.data.INamespace;
import org.ndexbio.common.models.data.INode;
import org.ndexbio.common.models.data.IReifiedEdgeTerm;
import org.ndexbio.common.models.data.ISupport;
import org.ndexbio.common.models.data.ITerm;
import org.ndexbio.common.models.object.MetaParameter;
import org.ndexbio.model.object.SearchParameters;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class NetworkUtility {

	private static Joiner csvJoiner = Joiner.on(',').skipNulls();
	
	public static String joinEdgeIdsToCsv(Collection<IEdge> iEdges) {
		String resultString = "";
		if (iEdges.size() < 1) return resultString;
		for (final IEdge iEdge: iEdges) {
			resultString += iEdge.asVertex().getId().toString() + ", ";
		}
		resultString = resultString.substring(0, resultString.length() - 2);
		return resultString;
	}

	public static String joinNodeIdsToCsv(List<INode> iNodes) {
		String resultString = "";
		if (iNodes.size() < 1) return resultString;
		for (final INode iNode: iNodes) {
			resultString += iNode.asVertex().getId().toString() + ", ";
		}
		resultString = resultString.substring(0, resultString.length() - 2);
		return resultString;
	}

	public static String joinCitationIdsToCsv(List<ICitation> iCitations) {
		String resultString = "";
		if (iCitations.size() < 1) return resultString;
		for (final ICitation iCitation: iCitations) {
			resultString += iCitation.asVertex().getId().toString() + ", ";
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
	public static  String joinBaseTermIdsToCsv(List<IBaseTerm> iBaseTerms) {
		String resultString = "";
		if (iBaseTerms.size() < 1) return resultString;
		for (final IBaseTerm iBaseTerm : iBaseTerms) {
			resultString += iBaseTerm.asVertex().getId().toString() + ", ";
		}
		resultString = resultString.substring(0, resultString.length() - 2);
		return resultString;
	}

	/**************************************************************************
	 * Parses metadata and metaterm parameters using the given regex and removes
	 * them from the search parameters.
	 * 
	 * @param searchParameters
	 *            The search parameters.
	 * @param metaRegex
	 *            The regex pattern to use for parsing parameters.
	 * @return An ArrayList containing the search parameters.
	 **************************************************************************/
	public static ArrayList<MetaParameter> parseMetaParameters(
			final SearchParameters searchParameters, final Pattern metaRegex) {
		final ArrayList<MetaParameter> metadataParameters = new ArrayList<MetaParameter>();
		final Matcher metadataMatches = metaRegex.matcher(searchParameters
				.getSearchString());
	
		if (!metadataMatches.find())
			return metadataParameters;
	
		for (int groupIndex = 0; groupIndex < metadataMatches.groupCount(); groupIndex += 3) {
			metadataParameters.add(new MetaParameter(metadataMatches
					.group(groupIndex + 1), metadataMatches.group(
					groupIndex + 2).charAt(0), metadataMatches.group(
					groupIndex + 3).substring(1,
					metadataMatches.group(groupIndex + 3).length() - 1)));
	
			searchParameters.setSearchString(searchParameters.getSearchString()
					.replace(metadataMatches.group(groupIndex), ""));
		}
	
		return metadataParameters;
	}

	public  static Set<ISupport> getEdgeSupports(final Collection<IEdge> edges) {
		final Set<ISupport> edgeSupports = new HashSet<ISupport>();
	
		for (final IEdge edge : edges) {
			for (final ISupport support : edge.getSupports())
				edgeSupports.add(support);
		}
	
		return edgeSupports;
	}

	public static Set<ICitation> getEdgeCitations(final Collection<IEdge> edges,
			final Collection<ISupport> supports) {
		final Set<ICitation> edgeCitations = new HashSet<ICitation>();
		for (final IEdge edge : edges) {
			for (final ICitation citation : edge.getCitations())
				edgeCitations.add(citation);
		}
	
		for (final ISupport support : supports) {
			if (support.getSupportCitation() != null)
				edgeCitations.add(support.getSupportCitation());
		}
	
		return edgeCitations;
	}

	public static Set<INode> getEdgeNodes(final Collection<IEdge> edges) {
		final Set<INode> edgeNodes = new HashSet<INode>();
	
		for (final IEdge edge : edges) {
			edgeNodes.add(edge.getSubject());
			edgeNodes.add(edge.getObject());
		}
	
		return edgeNodes;
	}

	public static Set<ITerm> getEdgeTerms(final Collection<IEdge> edges,
			final Collection<INode> nodes) throws NdexException {
		final Set<ITerm> edgeTerms = new HashSet<ITerm>();
	
		if (null != edges){
			for (final IEdge edge : edges)
				edgeTerms.add(edge.getPredicate());
		}
		
		for (final INode node : nodes) {
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
	
		return edgeTerms;
	}

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
	        throw new IllegalArgumentException
	            (l + " cannot be cast to int without changing its value.");
	    }
	    return (int) l;
	}

}
