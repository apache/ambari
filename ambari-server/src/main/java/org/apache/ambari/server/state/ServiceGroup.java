/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.state;

import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.ServiceGroupKey;
import org.apache.ambari.server.controller.ServiceGroupDependencyResponse;
import org.apache.ambari.server.controller.ServiceGroupResponse;
import org.apache.ambari.server.orm.entities.ServiceGroupEntity;

public interface ServiceGroup {

  Long getServiceGroupId();

  String getServiceGroupName();

  void setServiceGroupName(String serviceGroupName);

  long getClusterId();

  Cluster getCluster();

  Set<ServiceGroupKey> getServiceGroupDependencies();

  void setServiceGroupDependencies(Set<ServiceGroupKey> serviceGroupDependencies);

  ServiceGroupResponse convertToResponse();

  Set<ServiceGroupDependencyResponse> getServiceGroupDependencyResponses();

  StackId getStackId();

  void debugDump(StringBuilder sb);

  void refresh();

  /**
   * Find out whether the service and its components
   * are in a state that it can be removed from a cluster
   * @return
   */
  boolean canBeRemoved();

  void delete() throws AmbariException;

  /**
   * @param dependencyServiceGroupId dependency service group id which should be added to current
   * @return updated service group entity
   */
  ServiceGroupEntity addServiceGroupDependency(Long dependencyServiceGroupId) throws AmbariException;

  /**
   * @param dependencyServiceGroupId dependency service group id which should be removed from current
   * @return updated service group entity
   */
  ServiceGroupEntity deleteServiceGroupDependency(Long dependencyServiceGroupId) throws AmbariException;
}
