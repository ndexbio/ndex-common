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
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.7 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.12.03 at 11:33:45 AM PST 
//


package org.ndexbio.xbel.model;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://belframework.org/schema/1.0/xbel}header"/>
 *         &lt;element ref="{http://belframework.org/schema/1.0/xbel}namespaceGroup" minOccurs="0"/>
 *         &lt;element ref="{http://belframework.org/schema/1.0/xbel}annotationDefinitionGroup" minOccurs="0"/>
 *         &lt;element ref="{http://belframework.org/schema/1.0/xbel}statementGroup" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "header",
    "namespaceGroup",
    "annotationDefinitionGroup",
    "statementGroup"
})
@XmlRootElement(name = "document", namespace = "http://belframework.org/schema/1.0/xbel")
public class Document {

    @XmlElement(required = true)
    protected Header header;
    protected NamespaceGroup namespaceGroup;
    protected AnnotationDefinitionGroup annotationDefinitionGroup;
    @XmlElement(required = true)
    protected List<StatementGroup> statementGroup;

    /**
     * Gets the value of the header property.
     * 
     * @return
     *     possible object is
     *     {@link Header }
     *     
     */
    public Header getHeader() {
        return header;
    }

    /**
     * Sets the value of the header property.
     * 
     * @param value
     *     allowed object is
     *     {@link Header }
     *     
     */
    public void setHeader(Header value) {
        this.header = value;
    }

    /**
     * Gets the value of the namespaceGroup property.
     * 
     * @return
     *     possible object is
     *     {@link NamespaceGroup }
     *     
     */
    public NamespaceGroup getNamespaceGroup() {
        return namespaceGroup;
    }

    /**
     * Sets the value of the namespaceGroup property.
     * 
     * @param value
     *     allowed object is
     *     {@link NamespaceGroup }
     *     
     */
    public void setNamespaceGroup(NamespaceGroup value) {
        this.namespaceGroup = value;
    }

    /**
     * Gets the value of the annotationDefinitionGroup property.
     * 
     * @return
     *     possible object is
     *     {@link AnnotationDefinitionGroup }
     *     
     */
    public AnnotationDefinitionGroup getAnnotationDefinitionGroup() {
        return annotationDefinitionGroup;
    }

    /**
     * Sets the value of the annotationDefinitionGroup property.
     * 
     * @param value
     *     allowed object is
     *     {@link AnnotationDefinitionGroup }
     *     
     */
    public void setAnnotationDefinitionGroup(AnnotationDefinitionGroup value) {
        this.annotationDefinitionGroup = value;
    }

    /**
     * Gets the value of the statementGroup property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the statementGroup property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getStatementGroup().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link StatementGroup }
     * 
     * 
     */
    public List<StatementGroup> getStatementGroup() {
        if (statementGroup == null) {
            statementGroup = new ArrayList<StatementGroup>();
        }
        return this.statementGroup;
    }

}
