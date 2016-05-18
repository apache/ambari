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

package org.apache.ambari.server.state.svccomphost;


import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.UpgradeState;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


/**
 * Represents a summary of the versions of the components installed on a host.
 */
public class ServiceComponentHostSummary {

  private Collection<HostComponentStateEntity> allHostComponents;
  private Collection<HostComponentStateEntity> haveAdvertisedVersion;
  private Collection<HostComponentStateEntity> waitingToAdvertiseVersion;
  private Collection<HostComponentStateEntity> noVersionToAdvertise;
  private Set<String> versions;

  public ServiceComponentHostSummary(AmbariMetaInfo ambariMetaInfo, HostEntity host, String stackName, String stackVersion) throws AmbariException {
    allHostComponents = host.getHostComponentStateEntities();
    haveAdvertisedVersion = new HashSet<>();
    waitingToAdvertiseVersion = new HashSet<>();
    noVersionToAdvertise = new HashSet<>();
    versions = new HashSet<>();

    for (HostComponentStateEntity hostComponentStateEntity : allHostComponents) {
      ComponentInfo compInfo = ambariMetaInfo.getComponent(
          stackName, stackVersion, hostComponentStateEntity.getServiceName(),
          hostComponentStateEntity.getComponentName());

      if (!compInfo.isVersionAdvertised()) {
        // Some Components cannot advertise a version. E.g., ZKF, AMBARI_METRICS, Kerberos
        noVersionToAdvertise.add(hostComponentStateEntity);
      } else {
        if (hostComponentStateEntity.getUpgradeState().equals(UpgradeState.IN_PROGRESS) ||
            hostComponentStateEntity.getVersion().equalsIgnoreCase(State.UNKNOWN.toString())) {
          waitingToAdvertiseVersion.add(hostComponentStateEntity);
        } else {
          haveAdvertisedVersion.add(hostComponentStateEntity);
          versions.add(hostComponentStateEntity.getVersion());
        } // TODO: what if component reported wrong version?
      }
    }
  }

  public ServiceComponentHostSummary(AmbariMetaInfo ambariMetaInfo, HostEntity host, StackId stackId) throws AmbariException {
    this(ambariMetaInfo, host, stackId.getStackName(), stackId.getStackVersion());
  }

  public Collection<HostComponentStateEntity> getHaveAdvertisedVersion() {
    return haveAdvertisedVersion;
  }

  public boolean isUpgradeFinished() {
    return haveAllComponentsFinishedAdvertisingVersion() && noComponentVersionMismatches(getHaveAdvertisedVersion());
  }

  /**
   * @param upgradeEntity Upgrade info about update on given host
   * @return Return true if multiple component versions are found for this host, or if it does not coincide with the
   * CURRENT repo version.
   */
  public boolean isUpgradeInProgress(UpgradeEntity upgradeEntity) {
    // Exactly one CURRENT version must exist
    // We can only detect an upgrade if the Host has at least one component that advertises a version and has done so already
    // If distinct versions have been advertises, then an upgrade is in progress.
    // If exactly one version has been advertises, but it doesn't coincide with the CURRENT HostVersion, then an upgrade is in progress.
    return upgradeEntity != null;
  }

  /**
   * Determine if all of the components on that need to advertise a version have finished doing so.
   * @return Return a bool indicating if all components that can report a version have done so.
   */
  public boolean haveAllComponentsFinishedAdvertisingVersion() {
    return waitingToAdvertiseVersion.isEmpty();
  }

  /**
   * Checks that every component has really advertised version (in other words, we are not waiting
   * for version advertising), and that no version mismatch occurred
   *
   * @param hostComponents host components
   * @return true if components have advertised the same version, or collection is empty, false otherwise.
   */
  public static boolean noComponentVersionMismatches(Collection<HostComponentStateEntity> hostComponents) {
    for (HostComponentStateEntity hostComponent : hostComponents) {
      if (UpgradeState.VERSION_NON_ADVERTISED_STATES.contains(hostComponent.getUpgradeState())) {
        return false;
      }
    }
    return true;
  }
}
