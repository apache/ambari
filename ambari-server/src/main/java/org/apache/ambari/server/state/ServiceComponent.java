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

package org.apache.ambari.server.state;

import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.ServiceComponentResponse;

public interface ServiceComponent {

  String getName();

  String getServiceName();

  long getClusterId();

  String getClusterName();

  State getDesiredState();

  void setDesiredState(State state);

  StackId getDesiredStackVersion();

  void setDesiredStackVersion(StackId stackVersion);

  Map<String, ServiceComponentHost> getServiceComponentHosts();

  ServiceComponentHost getServiceComponentHost(String hostname)
      throws AmbariException;

  void addServiceComponentHosts(Map<String, ServiceComponentHost>
      hostComponents) throws AmbariException ;

  void addServiceComponentHost(ServiceComponentHost hostComponent)
      throws AmbariException ;

  ServiceComponentResponse convertToResponse();

  void refresh();

  boolean isPersisted();

  void persist();

  void debugDump(StringBuilder sb);

  boolean isClientComponent();

  boolean isMasterComponent();

  boolean isVersionAdvertised();

  boolean canBeRemoved();

  void deleteAllServiceComponentHosts() throws AmbariException;

  void deleteServiceComponentHosts(String hostname)
      throws AmbariException;

  ServiceComponentHost addServiceComponentHost(
      String hostName) throws AmbariException;

  void delete() throws AmbariException;

  /**
   * Get lock to control access to cluster structure
   * @return cluster-global lock
   */
  ReadWriteLock getClusterGlobalLock();
}
