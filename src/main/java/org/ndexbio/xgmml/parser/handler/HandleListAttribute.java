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

import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.xgmml.parser.ObjectType;
import org.ndexbio.xgmml.parser.ParseState;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class HandleListAttribute extends AbstractHandler {

    @Override
    public ParseState handle(String namespace, String tag, String qName,  Attributes atts, ParseState currentParseState) throws SAXException, NdexException {
        final String type = atts.getValue("type");
        final String name = manager.currentAttributeID;
        final ObjectType objType = typeMap.getType(type);
        final Object value = attributeValueUtil.getTypedAttributeValue(objType, atts, name);
        //Class<?> clazz = null;
        
        //throw new NdexException("list attributes not yet handled");

        //System.out.println("Adding '" + value.toString() + "' to current list");
        manager.getCurrentList().add(value.toString());/*
        for (String item : manager.getCurrentList()){
        	System.out.println("Current List has: " + item);
        }
        */
        return currentParseState;
        
        /*
        switch (objType) {
            case BOOLEAN:
                clazz = Boolean.class;
                break;
            case REAL:
                clazz = SUIDUpdater.isUpdatable(name) ? Long.class : Double.class;
                break;
            case INTEGER:
                clazz = Integer.class;
                break;
            case STRING:
            default:
                clazz = String.class;
                break;
        }
        
        

        final CyRow row = manager.getCurrentRow();
        CyColumn column = row.getTable().getColumn(name);

        if (column == null) {
            row.getTable().createListColumn(name, clazz, false, new ArrayList());
            column = row.getTable().getColumn(name);
        }

        if (List.class.isAssignableFrom(column.getType())) {
            if (manager.listAttrHolder == null) {
                manager.listAttrHolder = new ArrayList<Object>();
                row.set(name, manager.listAttrHolder);
            }

            if (manager.listAttrHolder != null && value != null)
                manager.listAttrHolder.add(value);
        }

        return current;
        
        */

    }
}
