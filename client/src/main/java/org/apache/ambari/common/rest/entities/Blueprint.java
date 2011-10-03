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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for BlueprintType complex type.
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Blueprint", propOrder = {
    "name",
    "revision",
    "parentName",
    "parentRevision",
    "packageRepositories",
    "configuration",
    "components",
    "roles"
})
@XmlRootElement(name = "Blueprints")
public class Blueprint {

    @XmlElement(name = "Name", required = true)
    protected String name;
    @XmlElement(name = "Revision", required = true)
    protected String revision;
    @XmlElement(name = "ParentName", required = true)
    protected String parentName;
    @XmlElement(name = "ParentRevision", required = true)
    protected String parentRevision;
    @XmlElement(name = "PackageRepositories")
    protected List<PackageRepository> packageRepositories;
    @XmlElement(name = "Configuration")
    protected Configuration configuration;
    
    // TODO: Should component include fixed or variable set of properties?
    @XmlElement(name = "Components")
    protected List<Component> components;
    @XmlElement(name = "Roles")
    protected List<Role> roles;
    
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
     * @return the revision
     */
    public String getRevision() {
            return revision;
    }
    /**
     * @param revision the revision to set
     */
    public void setRevision(String revision) {
            this.revision = revision;
    }
    
    /**
     * @return the parentName
     */
    public String getParentName() {
            return parentName;
    }
    /**
     * @param parentName the parentName to set
     */
    public void setParentName(String parentName) {
            this.parentName = parentName;
    }
    /**
     * @return the parentRevision
     */
    public String getParentRevision() {
            return parentRevision;
    }
    /**
     * @param parentRevision the parentRevision to set
     */
    public void setParentRevision(String parentRevision) {
            this.parentRevision = parentRevision;
    }
    /**
     * @return the packageRepositories
     */
    public List<PackageRepository> getPackageRepositories() {
            return packageRepositories;
    }
    /**
     * @param packageRepositories the packageRepositories to set
     */
    public void setPackageRepositories(
                    List<PackageRepository> packageRepositories) {
            this.packageRepositories = packageRepositories;
    }
    /**
     * @return the configuration
     */
    public Configuration getConfiguration() {
            return configuration;
    }
    /**
     * @param configuration the configuration to set
     */
    public void setConfiguration(Configuration configuration) {
            this.configuration = configuration;
    }
    /**
     * @return the components
     */
    public List<Component> getComponents() {
            return components;
    }
    /**
     * @param components the components to set
     */
    public void setComponents(List<Component> components) {
            this.components = components;
    }
    /**
     * @return the roles
     */
    public List<Role> getRoles() {
            return roles;
    }
    /**
     * @param roles the roles to set
     */
    public void setRoles(List<Role> roles) {
            this.roles = roles;
    }

}
