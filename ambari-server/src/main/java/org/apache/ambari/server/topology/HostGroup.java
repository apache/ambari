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

package org.apache.ambari.server.topology;

import java.util.Collection;

import org.apache.ambari.server.controller.internal.ProvisionAction;

/**
 * Host Group representation.
 */
public interface HostGroup {

  /**
   * Get the name of the host group.
   *
   * @return the host group name
   */
  String getName();

  /**
   * Get all of the host group components.
   *
   * @return collection of component instances
   */
  Collection<Component> getComponents();

  /**
   * Get all of the host group component names
   *
   * @return collection of component names as String
   */
  @Deprecated
  Collection<String> getComponentNames();

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
  @Deprecated
  Collection<String> getComponentNames(ProvisionAction provisionAction);

  /**
   * Add a component to the host group
   */
  boolean addComponent(Component component);

  /**
   * Get the configuration associated with the host group.
   * The host group configuration has the blueprint cluster scoped
   * configuration set as it's parent.
   *
   * @return host group configuration
   */
  Configuration getConfiguration();

  /**
   * Get the cardinality value that was specified for the host group.
   * This is simply meta-data for the stack that a deployer can use
   * and this information is not used by ambari.
   *
   * @return the cardinality specified for the hostgroup
   */
  String getCardinality();
}

