/**
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

package org.apache.ambari.server.controller;


import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.state.PropertyInfo.PropertyType;

public class StackConfigurationResponse {

  /**
   * Stack configuration response.
   * @param propertyName Property Key
   * @param propertyValue Property Value
   * @param propertyDescription Property Description
   * @param type Configuration type
   * @param propertyAttributes Attributes map
   */
  public StackConfigurationResponse(String propertyName, String propertyValue, String propertyDescription,
                                    String type, Map<String, String> propertyAttributes) {
    setPropertyName(propertyName);
    setPropertyValue(propertyValue);
    setPropertyDescription(propertyDescription);
    setType(type);
    setPropertyAttributes(propertyAttributes);
  }

  /**
   * Stack configuration response with all properties.
   * @param propertyName Property Key
   * @param propertyValue Property Value
   * @param propertyDescription Property Description
   * @param type Configuration type
   * @param isRequired Is required to be set
   * @param propertyType Property Type
   * @param propertyAttributes Attributes map
   */
  public StackConfigurationResponse(String propertyName, String propertyValue,
                                    String propertyDescription, String type,
                                    Boolean isRequired, Set<PropertyType> propertyTypes, Map<String, String> propertyAttributes) {
    setPropertyName(propertyName);
    setPropertyValue(propertyValue);
    setPropertyDescription(propertyDescription);
    setType(type);
    setRequired(isRequired);
    setPropertyType(propertyTypes);
    setPropertyAttributes(propertyAttributes);
  }

  private String stackName;
  private String stackVersion;
  private String serviceName;
  private String propertyName;
  private String propertyValue;
  private String propertyDescription;
  private String type;
  private Map<String, String> propertyAttributes;
  private Boolean isRequired;
  private Set<PropertyType> propertyTypes;

  public String getStackName() {
    return stackName;
  }

  public void setStackName(String stackName) {
    this.stackName = stackName;
  }

  public String getStackVersion() {
    return stackVersion;
  }

  public void setStackVersion(String stackVersion) {
    this.stackVersion = stackVersion;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getPropertyName() {
    return propertyName;
  }

  public void setPropertyName(String propertyName) {
    this.propertyName = propertyName;
  }

  public String getPropertyValue() {
    return propertyValue;
  }

  public void setPropertyValue(String propertyValue) {
    this.propertyValue = propertyValue;
  }

  public String getPropertyDescription() {
    return propertyDescription;
  }

  public void setPropertyDescription(String propertyDescription) {
    this.propertyDescription = propertyDescription;
  }

  /**
   * Configuration type
   * @return Configuration type (*-site.xml)
   */
  public String getType() {
    return type;
  }
  
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Provides attributes of this configuration.
   *
   * @return Map of attribute name to attribute value
   */
  public Map<String, String> getPropertyAttributes() {
    return propertyAttributes;
  }

  /**
   * Sets attributes for this configuration.
   *
   * @param propertyAttributes Map of attribute name to attribute value
   */
  public void setPropertyAttributes(Map<String, String> propertyAttributes) {
    this.propertyAttributes = propertyAttributes;
  }

  /**
   * Is property a isRequired property
   * @return True/False
   */
  public Boolean isRequired() {
    return isRequired;
  }

  /**
   * Set required attribute on this property.
   * @param required True/False.
   */
  public void setRequired(Boolean required) {
    this.isRequired = required;
  }

  /**
   * Get type of property as set in the stack definition.
   * @return Property type.
   */
  public Set<PropertyType> getPropertyType() {
    return propertyTypes;
  }

  public void setPropertyType(Set<PropertyType> propertyTypes) {
    this.propertyTypes = propertyTypes;
  }
}
