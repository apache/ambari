/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.common.rest.entities;

import java.util.Date;
import java.util.GregorianCalendar;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * The information about a server running on a node, which is included in 
 * Node.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
public class NodeServer {

    public static final String NODE_SERVER_STATE_UP = "UP";
    public static final String NODE_SERVER_STATE_DOWN = "DOWN";
    
    /*
     * name should be component name : role name
     * TODO : May be we can have component and role as two separate attributes instead of name
     */
    @XmlAttribute(required = true)
    protected String name;
    
    @XmlAttribute(required = true)
    protected String state;  // UP/DOWN
    
    @XmlAttribute(required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar lastStateUpdateTime;
        
    /**
     * 
     */
    
    /**
     * @return the name
     */
    public String getName() {
            return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
            this.name = name;
    }

    /**
     * @return the state
     */
    public String getState() {
            return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(String state) {
            this.state = state;
    }


    /**
     * @return the lastStateUpdateTime
     */
    public XMLGregorianCalendar getLastStateUpdateTime() {
            return lastStateUpdateTime;
    }

    /**
     * @param lastStateUpdateTime the lastStateUpdateTime to set
     */
    public void setLastStateUpdateTime(XMLGregorianCalendar lastStateUpdateTime) {
            this.lastStateUpdateTime = lastStateUpdateTime;
    }

    /**
     * @param creationTime the creationTime to set
     */
    protected void setLastUpdateTime(Date lastStateUpdateTime) throws Exception {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(lastStateUpdateTime);
            this.lastStateUpdateTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
    }
    
}
