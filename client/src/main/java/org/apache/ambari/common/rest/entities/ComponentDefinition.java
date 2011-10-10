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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * Define the name, group, and version of a component definition.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class ComponentDefinition {
  @XmlAttribute
  protected String group;
  @XmlAttribute
  protected String definition;
  @XmlAttribute
  protected String version;
  
  /**
   * Get the group that published the component definition
   * @return the group name
   */
  public String getGroup() {
    return group;
  }
  
  /**
   * Get the name of the component definition
   * @return the component definition name
   */
  public String getDefinition() {
    return definition;
  }
  
  /**
   * Get the version of the component definition
   * @return the version string
   */
  public String getVersion() {
    return version;
  }
  
  /**
   * Set the group that published the component definition
   * @param group
   */
  public void setGroup(String group) {
    this.group = group;
  }
  
  /**
   * Set the component definition name.
   * @param definition the new name
   */
  public void setDefinition(String definition) {
    this.definition = definition;
  }
  
  /**
   * Set the version of the component definition.
   * @param version the new version
   */
  public void setVersion(String version) {
    this.version = version;
  }
}
