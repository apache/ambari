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

package org.apache.ambari.server.state;

import java.util.Map;

import org.apache.ambari.annotations.Experimental;
import org.apache.ambari.annotations.ExperimentalFeature;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.ServiceComponentResponse;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;

public interface ServiceComponent {

  String getName();

  String getType();

  Long getId();

  /**
   * Get a true or false value specifying
   * if auto start was enabled for this component.
   * @return true or false
   */
  boolean isRecoveryEnabled();

  /**
   * Set a true or false value specifying if this
   * component is to be enabled for auto start or not.
   * @param recoveryEnabled - true or false
   */
  void setRecoveryEnabled(boolean recoveryEnabled);

  String getServiceName();

  String getServiceType();

  Long getServiceGroupId();

  Long getClusterId();

  String getClusterName();

  State getDesiredState();

  Long getServiceId();

  void setDesiredState(State state);

  /**
   * Gets the desired repository for this service component.
   *
   * @return
   */
  @Deprecated
  @Experimental(feature = ExperimentalFeature.REPO_VERSION_REMOVAL)
  RepositoryVersionEntity getDesiredRepositoryVersion();

  @Deprecated
  @Experimental(feature = ExperimentalFeature.REPO_VERSION_REMOVAL)
  StackId getDesiredStackId();

  @Deprecated
  @Experimental(feature = ExperimentalFeature.REPO_VERSION_REMOVAL)
  String getDesiredVersion();

  @Deprecated
  @Experimental(feature = ExperimentalFeature.REPO_VERSION_REMOVAL)
  void setDesiredRepositoryVersion(RepositoryVersionEntity repositoryVersionEntity);

  /**
   * Refresh Component info due to current stack
   * @throws AmbariException
   */
  void updateComponentInfo() throws AmbariException;

  Map<String, ServiceComponentHost> getServiceComponentHosts();

  ServiceComponentHost getServiceComponentHost(String hostname)
      throws AmbariException;

  void addServiceComponentHosts(Map<String, ServiceComponentHost>
      hostComponents) throws AmbariException ;

  void addServiceComponentHost(ServiceComponentHost hostComponent)
      throws AmbariException ;

  ServiceComponentResponse convertToResponse();

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
   * @return the repository state for the desired version
   */
  @Deprecated
  @Experimental(feature = ExperimentalFeature.REPO_VERSION_REMOVAL)
  RepositoryVersionState getRepositoryState();
}
