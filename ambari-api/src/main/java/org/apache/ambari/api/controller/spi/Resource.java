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
package org.apache.ambari.api.controller.spi;

import java.util.Map;

/**
 * The resource object represents a requested resource.  The resource
 * contains a collection of values for the requested properties.
 */
public interface Resource {
  /**
   * Get the resource type.
   *
   * @return the resource type
   */
  public Type getType();

  /**
   * Get the map of categories contained by this resource.  The map
   * is keyed by the category name and contains maps of properties
   * for each category.
   *
   * @return the map of categories
   */
  public Map<String, Map<String, String>> getCategories();

  /**
   * Set a string property value for the given property id on this resource.
   *
   * @param id    the property id
   * @param value the value
   */
  public void setProperty(PropertyId id, String value);

  /**
   * Set a integer property value for the given property id on this resource.
   *
   * @param id    the property id
   * @param value the value
   */
  public void setProperty(PropertyId id, Integer value);

  /**
   * Set a float property value for the given property id on this resource.
   *
   * @param id    the property id
   * @param value the value
   */
  public void setProperty(PropertyId id, Float value);

  /**
   * Set a double property value for the given property id on this resource.
   *
   * @param id    the property id
   * @param value the value
   */
  public void setProperty(PropertyId id, Double value);

  /**
   * Set a long property value for the given property id on this resource.
   *
   * @param id    the property id
   * @param value the value
   */
  public void setProperty(PropertyId id, Long value);

  /**
   * Get a property value for the given property id from this resource.
   *
   * @param id the property id
   * @return the property value
   */
  public String getPropertyValue(PropertyId id);

  /**
   * Resource types.
   */
  public enum Type {
    Cluster,
    Service,
    Host,
    Component,
    HostComponent
  }
}
