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

import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * Stack definition.
 * Stacks include the components to deploy and the configuration for the
 * cluster.
 * 
 * <p>
 * The schema is:
 * <pre>
 * grammar {
 *   start = element stack {
 *     attribute name { text }?
 *     attribute revision { text }?
 *     attribute parentName { text }?
 *     attribute parentRevision { text }?
 *     attribute creationTime { text }?
 *     element repositories {
 *       attribute name { text }
 *       element urls { text }*
 *     }*
 *     element configuration { Configuration }?
 *     element components {
 *       attribute name { text }
 *       attribute version { text }?
 *       attribute architecture { text }?
 *       attribute provider { text }?
 *       element definition {
 *         attribute provider { text }?
 *         attribute name { text }?
 *         attribute version { text }?
 *       }
 *       element configuration { Configuration }?
 *       element roles {
 *         attribute name { text }
 *         element configuration { Configuration }?
 *       }*
 *     }*
 *   }
 *   Configuration = element configuration {
 *     element category {
 *       attribute name { text }
 *       element property {
 *         attribute name { text }
 *         attribute value { text }
 *         attribute force { boolean }?
 *       }*
 *     }
 *   }
 * }
 * </pre>
 * </p>
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "stack", propOrder = {
    "name",
    "revision",
    "parentName",
    "parentRevision",
    "creationTime",
    "repositories",
    "configuration",
    "components"
})
@XmlRootElement(name="stack")
public class Stack {

    /**
     * The name of the stack.
     * This is a read-only attribute.
     */
    @XmlAttribute
    protected String name;
    /**
     * This revision of this stack. 
     * This is a read-only attribute.
     */
    @XmlAttribute
    protected String revision;
    
    /**
     * The name of the parent stack. Attributes that aren't defined by this
     * stack are inherited from the parent.
     */
    @XmlAttribute
    protected String parentName;
    
    /**
     * The revision number of the parent stack.
     */
    @XmlAttribute
    protected String parentRevision;
    
    /**
     * When this revision of the stack was created.
     * This is a read-only attribute.
     */
    @XmlAttribute
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar creationTime;

    /**
     * Information about where to pick up the tarballs/rpms for the components.
     */
    @XmlElement
    protected List<RepositoryKind> repositories;
    
    /**
     * The client configuration.
     */
    @XmlElement
    protected Configuration configuration;
    
    /**
     * The list of components that are included in this stack. This includes
     * the version of each component and the associated configuration.
     */
    @XmlElement
    protected List<Component> components;
    
    /**
     * Get the name of the stack.
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
     * Get the name of the parent stack. Attributes that aren't defined by this
     * stack are inherited from the parent.
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
     * Get the list of package repositories that store the rpms and tarballs.
     * @return the packageRepositories
     */
    public List<RepositoryKind> getPackageRepositories() {
            return repositories;
    }

    /**
     * @param packageRepositories the packageRepositories to set
     */
    public void setPackageRepositories(
                    List<RepositoryKind> value) {
            this.repositories = value;
    }

    /**
     * Get the client configuration.
     * @return the configuration
     */
    public Configuration getConfiguration() {
            return configuration;
    }

    /**
     * Set the client configuration.
     * @param configuration the configuration to set
     */
    public void setConfiguration(Configuration configuration) {
            this.configuration = configuration;
    }

    /**
     * Get the list of components for this stack.
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
     * Get the creation time of this stack revision.
     * @return the creation time
     */
    public XMLGregorianCalendar getCreationTime() {
        return creationTime;
    }

    /**
     * @param creationTime the creationTime to set
     */
    public void setCreationTime(XMLGregorianCalendar creationTime) {
        this.creationTime = creationTime;
    }
}
