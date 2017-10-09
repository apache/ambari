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

package org.apache.ambari.server.state.svccomphost;


import java.util.Collection;
import java.util.HashSet;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.UpgradeState;
import org.apache.commons.lang.StringUtils;


/**
 * Represents a summary of the versions of the components installed on a host.
 */
public class ServiceComponentHostSummary {

  private Collection<HostComponentStateEntity> allHostComponents;
  private Collection<HostComponentStateEntity> haveAdvertisedVersion;
  private Collection<HostComponentStateEntity> waitingToAdvertiseVersion;
  private Collection<HostComponentStateEntity> noVersionToAdvertise;

  /**
   * Constructor.
   *
   * @param ambariMetaInfo
   *          used to lookup whether a component advertises a version (not
   *          {@code null}).
   * @param host
   *          the host to generate a component summary for (not {@code null}).
   * @param repositoryVersion
   *          the repository to generate a summary for (not {@code null}).
   * @throws AmbariException
   */
  public ServiceComponentHostSummary(AmbariMetaInfo ambariMetaInfo, HostEntity host,
      RepositoryVersionEntity repositoryVersion) throws AmbariException {
    allHostComponents = host.getHostComponentStateEntities();
    haveAdvertisedVersion = new HashSet<>();
    waitingToAdvertiseVersion = new HashSet<>();
    noVersionToAdvertise = new HashSet<>();

    String stackName = repositoryVersion.getStackName();
    String stackVersion = repositoryVersion.getStackVersion();

    for (HostComponentStateEntity hostComponentStateEntity : allHostComponents) {
      ClusterServiceEntity serviceEntity = hostComponentStateEntity.getServiceComponentDesiredStateEntity().getClusterServiceEntity();
      String serviceName = serviceEntity.getServiceName();
      String serviceType  = serviceEntity.getServiceType();
      ComponentInfo compInfo = ambariMetaInfo.getComponent(stackName, stackVersion, serviceType,
                                                           hostComponentStateEntity.getComponentName());

      if (!compInfo.isVersionAdvertised()) {
        // Some Components cannot advertise a version. E.g., ZKF, AMBARI_METRICS, Kerberos
        noVersionToAdvertise.add(hostComponentStateEntity);
        continue;
      }

      String versionAdvertised = hostComponentStateEntity.getVersion();
      if (hostComponentStateEntity.getUpgradeState() == UpgradeState.IN_PROGRESS
          || StringUtils.equals(versionAdvertised, State.UNKNOWN.name())) {
        waitingToAdvertiseVersion.add(hostComponentStateEntity);
        continue;
      }

      haveAdvertisedVersion.add(hostComponentStateEntity);
    }
  }

  /**
   * Gets whether all hosts for a service component have reported the correct
   * version.
   *
   * @param repositoryVersion
   *          the version to report (not {@code null}).
   * @return {@code true} if all hosts for this service component have reported
   *         the correct version, {@code false} othwerise.
   */
  public boolean isVersionCorrectForAllHosts(RepositoryVersionEntity repositoryVersion) {
    if (!waitingToAdvertiseVersion.isEmpty()) {
      return false;
    }

    for (HostComponentStateEntity hostComponent : haveAdvertisedVersion) {
      if (UpgradeState.VERSION_NON_ADVERTISED_STATES.contains(hostComponent.getUpgradeState())) {
        return false;
      }

      ServiceComponentDesiredStateEntity desiredState = hostComponent.getServiceComponentDesiredStateEntity();
      RepositoryVersionEntity desiredRepositoryVersion = desiredState.getDesiredRepositoryVersion();
      if (!desiredRepositoryVersion.equals(repositoryVersion)) {
        continue;
      }

      if (!StringUtils.equals(hostComponent.getVersion(), desiredRepositoryVersion.getVersion())) {
        return false;
      }
    }

    return true;
  }
}
