package org.ndexbio.xgmml.parser;

/*
 * #%L
 * Cytoscape IO Impl (io-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */



import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.xgmml.parser.handler.AttributeValueUtil;

import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Manipulates network metadata for loading and saving.<br>
 *
 * @author kono
 *
 */
public class MetadataParser {
	/*
	 * Actual CyAttribute name for the network metadata
	 */

	/**
	 *
	 */
	public static final String DEFAULT_NETWORK_METADATA_LABEL = "Network Metadata";

	/*
	 * Default values for new meta data. Maybe changed later...
	 */
	private static final String DEF_URI = "http://www.cytoscape.org/";
	private static final String DEF_TYPE = "Protein-Protein Interaction";
	private static final String DEF_FORMAT = "Cytoscape-XGMML";
//	private String metadataLabel;
	private Network network;
	//private CyRow networkAttributes;
//	private Properties props;
	private List<NdexPropertyValuePair> rdfAsNdexProperties;

	/**
	 * Constructor.
	 *
	 * @param network
	 *            Target network for editing metadata.
	 */
	public MetadataParser(Network network) {
		this(network, DEFAULT_NETWORK_METADATA_LABEL);
	}

	/**
	 * Constructor.
	 *
	 * @param network
	 *            Target network
	 * @param metadataLabel
	 *            Label used as a tag for this attribute.
	 */
	public MetadataParser(Network network, String metadataLabel) {
//		this.metadataLabel = metadataLabel;
		this.network = network;

		rdfAsNdexProperties = network.getProperties();
	}

	// TODO to be injected
	public void setProperties(Properties p) {
//		props = p;
	}

	/**
	 * Build new metadata RDF structure based on given network information.
	 *
	 * Data items in "defaultLabels" will be created and inserted into RDF
	 * structure.
	 */
	public List<NdexPropertyValuePair> makeNewMetadataProperties() {
		List<NdexPropertyValuePair> props = new ArrayList<>();

	    // TODO: determine where these defaults should be coming from
		// Extract default values from property
		//String defSource = props.getProperty("defaultMetadata.source");
		//String defType = props.getProperty("defaultMetadata.type");
		//String defFormat = props.getProperty("defaultMetadata.format");
		String defSource = null;
	    String defType = null;
	    String defFormat = null;

		MetadataEntries[] entries = MetadataEntries.values();

		for (int i = 0; i < entries.length; i++) {
			switch (entries[i]) {
				case DATE:

					java.util.Date now = new java.util.Date();
					DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					AttributeValueUtil.setProperty(props, entries[i].toString(), df.format(now),null);
					//dataMap.put(entries[i].toString(), df.format(now));

					break;

				case TITLE:
					//dataMap.put(entries[i].toString(), network.getRow(network).get("title",String.class));

					break;

				case SOURCE:

					if (defSource == null) {
						AttributeValueUtil.setProperty(props, entries[i].toString(), DEF_URI,null);
						//dataMap.put(entries[i].toString(), DEF_URI);
					} else {
						AttributeValueUtil.setProperty(props, entries[i].toString(), defSource, null);
						//dataMap.put(entries[i].toString(), defSource);
					}

					break;

				case TYPE:

					if (defType == null) {
						AttributeValueUtil.setProperty(props, entries[i].toString(), DEF_TYPE,null);
						//dataMap.put(entries[i].toString(), DEF_TYPE);
					} else {
						AttributeValueUtil.setProperty(props, entries[i].toString(), defType, null);
						//dataMap.put(entries[i].toString(), defType);
					}

					break;

				case FORMAT:

					if (defFormat == null) {
						AttributeValueUtil.setProperty(props, entries[i].toString(), DEF_FORMAT, null);
						//dataMap.put(entries[i].toString(), DEF_FORMAT);
					} else {
						AttributeValueUtil.setProperty(props, entries[i].toString(), defFormat, null);
						// dataMap.put(entries[i].toString(), defFormat);
					}

					break;

				default:
					AttributeValueUtil.setProperty(props, entries[i].toString(), "N/A", null);
					//dataMap.put(entries[i].toString(), "N/A");

					break;
			}
		}

		return props;
	}

	/**
	 * Get Network Metadata as Map object
	 *
	 * @return
	 * @throws URISyntaxException
	 */
	public List<NdexPropertyValuePair> getMetadataProperties() {
		if ((rdfAsNdexProperties == null) || (rdfAsNdexProperties.size() == 0)) {
			rdfAsNdexProperties = makeNewMetadataProperties();
		}

		return rdfAsNdexProperties;
	}

	public void setMetadata(MetadataEntries entryName, String value) {

		AttributeValueUtil.setAttribute(network, entryName.toString(), value, null);

		rdfAsNdexProperties = network.getProperties();
	}
}
