package org.ndexbio.xgmml.parser.handler;

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.PropertiedObject;
import org.ndexbio.xgmml.parser.ObjectType;
import org.ndexbio.xgmml.parser.ObjectTypeMap;
import org.ndexbio.xgmml.parser.ParseState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class AttributeValueUtil {

    static final String ATTR_NAME = "name";
    static final String ATTR_LABEL = "label";
    static final String ATTR_VALUE = "value";
    static final String LOCKED_VISUAL_PROPS = "lockedVisualProperties";
    
    static final Pattern XLINK_PATTERN = Pattern.compile(".*#(-?\\d+)");
    
    private Locator locator;

    private final ReadDataManager manager;
    private final ObjectTypeMap typeMap;
    
    protected static final Logger logger = LoggerFactory.getLogger(AttributeValueUtil.class);

    public AttributeValueUtil(final ReadDataManager manager) {
        this.manager = manager;
        this.typeMap = new ObjectTypeMap();
    }

    public void setLocator(Locator locator) {
        this.locator = locator;
    }
 
    /********************************************************************
     * Routines to handle attributes
     *******************************************************************/

    /**
     * Return the string attribute value for the attribute indicated by "key".
     * If no such attribute exists, return null. In particular, this routine
     * looks for an attribute with a <b>name</b> or <b>label</b> of <i>key</i>
     * and returns the <b>value</b> of that attribute.
     * 
     * @param atts
     *            the attributes
     * @param key
     *            the specific attribute to get
     * @return the value for "key" or null if no such attribute exists
     */
    protected String getAttributeValue(Attributes atts, String key) {
        String name = atts.getValue(ATTR_NAME);

        if (name == null) name = atts.getValue(ATTR_LABEL);

        if (name != null && name.equals(key))
            return atts.getValue(ATTR_VALUE);
		return null;
    }

    /**
	 * Return the typed attribute value for the passed attribute. In this case, the caller has already determined that
	 * this is the correct attribute and we just lookup the value. This routine is responsible for type conversion
	 * consistent with the passed argument.
	 * 
	 * @param type the ObjectType of the value
	 * @param atts the attributes
	 * @param name the attribute name
	 * @return the value of the attribute in the appropriate type
	 */
    protected Object getTypedAttributeValue(ObjectType type, Attributes atts, String name) throws SAXParseException {
        String value = atts.getValue("value");

        try {
            return typeMap.getTypedValue(type, value, name);
        } catch (Exception e) {
            throw new SAXParseException("Unable to convert '" + value + "' to type " + type.toString(), locator);
        }
    }

    /**
     * Return the attribute value for the attribute indicated by "key". If no
     * such attribute exists, return null.
     * 
     * @param atts
     *            the attributes
     * @param key
     *            the specific attribute to get
     * @return the value for "key" or null if no such attribute exists
     */
    protected String getAttribute(Attributes atts, String key) {
        return atts.getValue(key);
    }

    /**
     * Return the attribute value for the attribute indicated by "key". If no
     * such attribute exists, return null.
     * 
     * @param atts
     *            the attributes
     * @param key
     *            the specific attribute to get
     * @param ns
     *            the namespace for the attribute we're interested in
     * @return the value for "key" or null if no such attribute exists
     */
    protected String getAttributeNS(Attributes atts, String key, String ns) {
        if (atts.getValue(ns, key) != null)
            return atts.getValue(ns, key);
		return atts.getValue(key);
    }

    protected ParseState handleAttribute(Attributes atts, boolean isNetworkAttribute) throws SAXParseException, ExecutionException {
    	ParseState parseState = ParseState.NONE;
    	
    	final String name = atts.getValue("name");
    	String type = atts.getValue("type");
    	String value = atts.getValue("value");
    	
    	if ("has_nested_network".equals(name))
        	type = ObjectType.BOOLEAN.getName();
    	
    	// Not handling equations or hidden attributes in NDEx at this time
    	
    	//final boolean isEquation = ObjectTypeMap.fromXGMMLBoolean(atts.getValue("cy:equation"));
    	//final boolean isHidden = ObjectTypeMap.fromXGMMLBoolean(atts.getValue("cy:hidden"));
        
		
		//INetwork network = manager.getCurrentNetwork();
		ObjectType objType = typeMap.getType(type);
		if (objType.equals(ObjectType.LIST)){
			manager.currentAttributeID = name;
			manager.setCurrentList(new ArrayList<String>());			
			return ParseState.LIST_ATT;
		}
		
//		System.out.println("setting attribute name = " + name + " value = " + value );
		
		if (null != type){
			try {
				if ( isNetworkAttribute) {
					manager.addNetworkAttribute(name, value, type);
				} else {
					final Long curElementId = manager.getCurrentElementId();
					manager.setElementProperty(curElementId, name, value, type);
				}
			} catch ( NdexException e) {
				throw new SAXParseException ("Ndex error: " + e.getMessage(),null);
			} catch (SAXException e) {
				throw new SAXParseException ("SAXException: " + e.getMessage(),null);
			}
		}
		
		return parseState;
		
    }
    
    public static void setAttribute(final PropertiedObject element, final String key, final String value, String type){
    	if (null == element.getProperties()){
    		element.setProperties(new ArrayList<NdexPropertyValuePair>());
    	}
    	setProperty(element.getProperties(), key, value, type);   	
    }
    
    // TODO - not used at the moment, determine role - are we going to support XLINKs?
    public static Long getIdFromXLink(String href) {
		Matcher matcher = XLINK_PATTERN.matcher(href);
		return matcher.matches() ? Long.valueOf(matcher.group(1)) : null;
	}

	public static void setProperty(List<NdexPropertyValuePair> props, String key,
			String value, String type) {
		for (NdexPropertyValuePair prop : props){
    		if (key.equalsIgnoreCase(prop.getPredicateString())){
    			prop.setValue(value);
    			return;
    		}
    	}
    	NdexPropertyValuePair prop = new NdexPropertyValuePair();
    	prop.setPredicateString(key);
    	prop.setValue(value);
    	if ( type != null ) {
    		prop.setDataType(type);
    	} else 
    		prop.setDataType(NdexPropertyValuePair.STRING);
    	props.add(prop);
		
	}
}
