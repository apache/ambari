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

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CategoryType complex type.
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CategoryType", propOrder = {
    "definition",
    "roles"
})
@XmlRootElement
public class Component {

    @XmlAttribute(required = true)
    protected String name;
    @XmlAttribute
    protected String architecture;
    @XmlAttribute
    protected String version;
    @XmlAttribute
    protected String provider;
    @XmlElement
    protected ComponentDefinition definition;
    @XmlElement
    protected List<Role> roles;

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Get the roles property
     * @return the custom configuration for the component
     */
    public List<Role> getRoles() {
      return roles;
    }
    
    /**
     * Set the roles property
     * @param roles
     */
    public void setRoles(List<Role> roles) {
      this.roles = roles;
    }

    /**
     * Get the architecture of the package to install.
     * @return the name of the architecture.
     */
    public String getArchitecture() {
      return architecture;
    }
    
    /**
     * Set the architecture of the package to install.
     * @param value the new architecture
     */
    public void setArchitecture(String value) {
      architecture = value;
    }
    
    /**
     * Get the name of the component definition
     * @return the component definition name
     */
    public ComponentDefinition getDefinition() {
      return definition;
    }
    
    /**
     * Set the name of the component definition
     * @param value the new name
     */
    public void setDefinition(ComponentDefinition value) {
      definition = value;
    }

    /**
     * Get the version of the package to install
     * @return the version string
     */
    public String getVersion() {
      return version;
    }
    
    /**
     * Set the version of the package to install
     * @param version the new version
     */
    public void setVersion(String version) {
      this.version = version;
    }
    
    /**
     * Get the provider of the package to install
     * @return the provider name
     */
    public String getProvider() {
      return provider;
    }
    
    /**
     * Set the provider of the package to install
     * @param value the new provider
     */
    public void setProvider(String value) {
      provider = value;
    }
}
