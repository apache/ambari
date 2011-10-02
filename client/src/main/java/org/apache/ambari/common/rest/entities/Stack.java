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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StackType", propOrder = {
    "name",
    "description",
    "blueprintLocationURL",
    "stackRevision"
})
@XmlRootElement(name = "Stacks")
public class Stack {
    
    @XmlElement(name = "Name", required = true)
    protected String name;
    @XmlElement(name = "Description", required = true)
    protected String description; 
    @XmlElement(name = "StackRevision", required = true)
    protected int stackRevision;
    @XmlElement(name = "BlueprintLocationURL", required = true)
    protected String blueprintLocationURL;
    
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
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }
    /**
     * @return the blueprintLocationURL
     */
    public String getBlueprintLocationURL() {
        return blueprintLocationURL;
    }
    /**
     * @param blueprintLocationURL the blueprintLocationURL to set
     */
    public void setBlueprintLocationURL(String blueprintLocationURL) {
        this.blueprintLocationURL = blueprintLocationURL;
    }
    /**
     * @return the stackRevision
     */
    public int getStackRevision() {
        return stackRevision;
    }
    /**
     * @param stackRevision the stackRevision to set
     */
    public void setStackRevision(int stackRevision) {
        this.stackRevision = stackRevision;
    }
}
