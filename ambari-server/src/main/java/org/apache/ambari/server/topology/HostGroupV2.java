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
import java.util.regex.Pattern;

import org.apache.ambari.server.controller.internal.ProvisionAction;

/**
 * Host Group representation.
 */
public interface HostGroupV2 {

  /**
   * Compiled regex for hostgroup token.
   */
  Pattern HOSTGROUP_REGEX = Pattern.compile("%HOSTGROUP::(\\S+?)%");
  /**
   * Get the name of the host group.
   *
   * @return the host group name
   */
  String getName();

  /**
   * Get the name of the associated blueprint
   *
   * @return associated blueprint name
   */
  String getBlueprintName();

  /**
   * Get the fully qualified host group name in the form of
   * blueprintName:hostGroupName
   *
   * @return fully qualified host group name
   */
  String getFullyQualifiedName();

  /**
   * Get all of the host group components.
   *
   * @return collection of component instances
   */
  Collection<ComponentV2> getComponents();

  /**
   * Get all of the host group component names
   *
   * @return collection of component names as String
   */
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
  Collection<String> getComponentNames(ProvisionAction provisionAction);

  /**
   * Get the host group components which belong to the specified service.
   *
   * @param serviceId  service id
   *
   * @return collection of component names for the specified service; will not return null
   */
  Collection<ComponentV2> getComponentsByServiceId(ServiceId serviceId);

  Collection<ComponentV2> getComponents(Service serviceId);

  /**
   * Determine if the host group contains a master component.
   *
   * @return true if the host group contains a master component; false otherwise
   */
  boolean containsMasterComponent();

  /**
   * @return collection of service ids associated with the host group components.
   */
  Collection<ServiceId> getServiceIds();

  /**
   * @return collection of services associated with the host group components.
   */
  Collection<Service> getServices();

  Service getService(ServiceId serviceId);

  /**
   * @return collection of service names associated with the host group components.
   */
  Collection<String> getServiceNames();

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

