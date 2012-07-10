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
  private String provider;
  @XmlAttribute
  private String name; 
  @XmlAttribute
  private String version;
  
  public ComponentDefinition() {
    // PASS
  }

  public ComponentDefinition(String name, String provider, String version) {
    this.name = name;
    this.provider = provider;
    this.version = version;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || other.getClass() != getClass()) {
      return false;
    } else if (other == this) {
      return true;
    } else {
      ComponentDefinition otherDefn = (ComponentDefinition) other;
      return isStringEqual(name, otherDefn.name) && 
             isStringEqual(provider, otherDefn.provider) &&
             isStringEqual(version, otherDefn.version);
    }
  }

  @Override
  public int hashCode() {
    return stringHash(name) + stringHash(version);
  }

  static int stringHash(String str) {
    return str != null ? str.hashCode() : 0;
  }

  static boolean isStringEqual(String left, String right) {
    if (left == right) {
      return true;
    } if (left == null || right == null) {
      return false;
    } else {
      return left.equals(right);
    }
  }

  /**
   * Override this configuration's properties with any corresponding ones
   * that are set in the other component.
   * @param other the overriding component
   */
  public void mergeInto(ComponentDefinition other) {
    if (other.provider != null) {
      this.provider = other.provider;
    }
    if (other.name != null) {
      this.name = other.name;
    }
    if (other.version != null) {
      this.version = other.version;
    }
  }
  
  /**
   * Get the provider that published the component definition
   * @return the provider name
   */
  public String getProvider() {
    return provider;
  }
  
  /**
   * Get the name of the component definition
   * @return the component definition name
   */
  public String getName() {
    return name;
  }
  
  /**
   * Get the version of the component definition
   * @return the version string
   */
  public String getVersion() {
    return version;
  }
  
  /**
   * Set the provider that published the component definition
   * @param provider
   */
  public void setProvider(String provider) {
    this.provider = provider;
  }
  
  /**
   * Set the component definition name.
   * @param definition the new name
   */
  public void setName(String name) {
    this.name = name;
  }
  
  /**
   * Set the version of the component definition.
   * @param version the new version
   */
  public void setVersion(String version) {
    this.version = version;
  }
  
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    buffer.append(provider);
    buffer.append('.');
    buffer.append(name);
    buffer.append('@');
    buffer.append(version);
    return buffer.toString();
  }
}
