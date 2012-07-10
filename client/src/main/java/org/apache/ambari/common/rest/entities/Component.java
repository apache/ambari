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
 * Metadata information about a given component.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Component", propOrder = {
    "definition",
    "user_group",
    "configuration",
    "roles"
})
@XmlRootElement
public class Component {

    /**
     * The name of the component.
     */
    @XmlAttribute(required = true)
    private String name;
    
    /**
     * The architecture of the tarball/rpm to install.
     */
    @XmlAttribute
    private String architecture;
    
    /**
     * The version of the tarball/rpm to install.
     */
    @XmlAttribute
    private String version;
    
    /**
     * The provider of the tarball/rpm to install.
     */
    @XmlAttribute
    private String provider;
    
    /**
     * The definition of the component including how to configure and run
     * the component.
     */
    @XmlElement
    private ComponentDefinition definition;
    
    /**
     * Component user/group information
     */
    @XmlElement
    private UserGroup user_group;
    
    /**
     * @return the user_group
     */
    public UserGroup getUser_group() {
        return user_group;
    }

    /**
     * @param user_group the user_group to set
     */
    public void setUser_group(UserGroup user_group) {
        this.user_group = user_group;
    }

    /**
     * The configuration shared between the active roles of the component.
     */
    @XmlElement
    private Configuration configuration;
    
    /**
     * Specific configuration for each of the roles.
     */
    @XmlElement
    private List<Role> roles;

    public Component() {
      // PASS
    }

    public Component(String name, String version, String architecture,
                     String provider, ComponentDefinition definition,
                     Configuration configuration, List<Role> roles, UserGroup user_group) {
      this.name = name;
      this.version = version;
      this.architecture = architecture;
      this.provider = provider;
      this.definition = definition;
      this.configuration = configuration;
      this.roles = roles;
      this.user_group = user_group;
    }

    /**
     * Shallow copy overriding attributes of a component.
     * @param other the component to copy
     */
    public void mergeInto(Component other) {
      if (other.architecture != null) {
        this.architecture = other.architecture;
      }
      if (other.configuration != null) {
        this.configuration = other.configuration;
      }
      if (other.definition != null) {
        this.definition.mergeInto(other.definition);
      }
      if (other.name != null) {
        this.name = other.name;
      }
      if (other.provider != null) {
        this.provider = other.provider;
      }
      if (other.roles != null) {
        this.roles = other.roles;
      }
      if (other.version != null) {
        this.version = other.version;
      }
      if (other.user_group != null) {
          this.user_group = other.user_group;
        }
    }
    
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
    
    /**
     * Get the configuration for all of the active roles.
     * @return the configuration
     */
    public Configuration getConfiguration() {
      return configuration;
    }
    
    /**
     * Set the configuration for all of the active roles
     * @param conf the configuration
     */
    public void setConfiguration(Configuration conf) {
      configuration = conf;
    }
}
