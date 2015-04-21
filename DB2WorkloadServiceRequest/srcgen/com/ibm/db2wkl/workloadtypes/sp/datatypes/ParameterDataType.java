/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation,IBM  v2.0.5-07/07/2007 05:49 PM(Raja)-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.09.27 at 01:54:03 PM CEST 
//


package com.ibm.db2wkl.workloadtypes.sp.datatypes;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ParameterDataType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="ParameterDataType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="integer"/>
 *     &lt;enumeration value="varchar"/>
 *     &lt;enumeration value="char"/>
 *     &lt;enumeration value="smallint"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlEnum
@XmlType(name = "ParameterDataType", namespace = "http://www.example.org/StoredProceduresSchema")
public enum ParameterDataType {

    @XmlEnumValue("integer")
    INTEGER("integer"),
    @XmlEnumValue("varchar")
    VARCHAR("varchar"),
    @XmlEnumValue("char")
    CHAR("char"),
    @XmlEnumValue("smallint")
    SMALLINT("smallint");
    private final String value;

    ParameterDataType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ParameterDataType fromValue(String v) {
        for (ParameterDataType c: ParameterDataType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v.toString());
    }

}
