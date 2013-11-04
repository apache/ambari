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

package org.apache.ambari.server.controller.spi;


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
   * Obtain the properties contained by this group in a map structure.
   * The category/property hierarchy is flattened into a map where
   * each key is the absolute category name and the corresponding
   * value is a map of properties(name/value pairs) for that category.
   *
   * @return resource properties map
   */
  public Map<String, Map<String, Object>> getPropertiesMap();

  /**
   * Set a property value for the given property id on this resource.
   *
   * @param id    the property id
   * @param value the value
   */
  public void setProperty(String id, Object value);

  /**
   * Add an empty category to this resource.
   *
   * @param id the category id
   */
  public void addCategory(String id);

  /**
   * Get a property value for the given property id from this resource.
   *
   * @param id the property id
   * @return the property value
   */
  public Object getPropertyValue(String id);

  /**
   * Resource types.
   */
  public enum Type {
    Cluster,
    Service,
    Host,
    Component,
    HostComponent,
    Configuration,
    ConfigGroup,
    Action,
    Request,
    Task,
    User,
    Stack,
    StackVersion,
    OperatingSystem,
    Repository,
    StackService,
    StackConfiguration,
    StackServiceComponent,
    DRFeed,
    DRTargetCluster,
    DRInstance,
    Workflow,
    Job,
    TaskAttempt,
    RootService,
    RootServiceComponent,
    RootServiceHostComponent
  }
}
