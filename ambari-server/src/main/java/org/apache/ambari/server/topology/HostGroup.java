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

package org.apache.ambari.server.topology;

import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.orm.entities.HostGroupEntity;
import org.apache.ambari.server.state.DependencyInfo;

import java.util.Collection;
import java.util.Map;

/**
 * Host Group representation.
 */
public interface HostGroup {

  /**
   * Get the name of the host group.
   *
   * @return the host group name
   */
  public String getName();

  /**
   * Get the name of the associated blueprint
   *
   * @return associated blueprint name
   */
  public String getBlueprintName();

  /**
   * Get the fully qualified host group name in the form of
   * blueprintName:hostgroupName
   *
   * @return fully qualified host group name
   */
  public String getFullyQualifiedName();

  /**
   * Get all of the host group components.
   *
   * @return collection of component instances
   */
  public Collection<Component> getComponents();

  /**
   * Get all of the host group component names
   *
   * @return collection of component names as String
   */
  public Collection<String> getComponentNames();

  /**
   * Get all host group component names for instances
   *   that have the specified provision action association.
   *
   * @param provisionAction the provision action that must be associated
   *                          with the component names returned
   *
   * @return collection of component names as String that are associated with
   *           the specified provision action
   */
  public Collection<String> getComponentNames(ProvisionAction provisionAction);

  /**
   * Get the host group components which belong to the specified service.
   *
   * @param service  service name
   *
   * @return collection of component names for the specified service; will not return null
   */
  public Collection<String> getComponents(String service);

  /**
   * Add a component to the host group.
   *
   * @param component  name of the component to add
   *
   * @return true if the component didn't already exist
   */
  public boolean addComponent(String component);

  /**
   * Add a component to the host group, with the specified name
   *   and provision action.
   *
   * @param component  component name
   * @param provisionAction provision action for this component
   * @return
   */
  public boolean addComponent(String component, ProvisionAction provisionAction);

  /**
   * Determine if the host group contains a master component.
   *
   * @return true if the host group contains a master component; false otherwise
   */
  public boolean containsMasterComponent();

  /**
   * Get all of the services associated with the host group components.
   *
   * @return collection of service names
   */
  public Collection<String> getServices();

  /**
   * Get the configuration associated with the host group.
   * The host group configuration has the blueprint cluster scoped
   * configuration set as it's parent.
   *
   * @return host group configuration
   */
  public Configuration getConfiguration();

  /**
   * Get the stack associated with the host group.
   *
   * @return associated stack
   */
  public Stack getStack();

  /**
   * Get the cardinality value that was specified for the host group.
   * This is simply meta-data for the stack that a deployer can use
   * and this information is not used by ambari.
   *
   * @return the cardinality specified for the hostgroup
   */
  public String getCardinality();
}

