package org.ndexbio.xgmml.parser.handler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.xgmml.parser.ParseState;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class HandleEdge extends AbstractHandler {

	//private static final String SPLIT_PATTERN = "[()]";
	private static final Pattern predictatePattern = Pattern.compile("^\\s*\\S+\\s+\\((\\S+)\\)\\s+\\S+\\s*$");

	@Override
	public ParseState handle(final String namespace, final String tag, final String qName, final Attributes atts, final ParseState current) throws SAXException, NdexException {
		// Get the label, id, source and target
//		Object id = null;
		String href = atts.getValue(ReadDataManager.XLINK, "href");

		if (href == null) {
			// Create the edge:
	//		id = getId(atts);
			String label = getLabel(atts);
			String sourceId = atts.getValue("source");
			String targetId = atts.getValue("target");
			String interaction = null;
			
			// check if we can parse the predicate from from label
			
			Matcher m = predictatePattern.matcher(label);
			
			if ( m.find()) 
				interaction = m.group(1);

	//Long edgeId = 
			manager.addEdge(sourceId, interaction, targetId);

			manager.getCurrentXGMMLEdge().getProps().add(new NdexPropertyValuePair(LABEL,label));
			//TODO: add the rest of atts as properties to the edge.
			

		} else {
			// The edge might not have been created yet!
			// Save the reference so it can be added to the network after the
			// whole graph is parsed.
			throw new NdexException("not yet handling XLINK in XGMML");
		}

		return current;
	}

/*	
	private final static Object asLongOrString(final String value) {
		if (value != null) {
			try {
				return Long.valueOf(value.trim());
			} catch (NumberFormatException nfe) {
				// TODO: warning?
			}
		}
		return value;
	} */
}
