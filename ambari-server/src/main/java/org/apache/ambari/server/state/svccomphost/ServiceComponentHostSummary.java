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


import com.google.inject.Inject;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.HashSet;


/**
 * Represents a summary of the versions of the components installed on a host.
 */
public class ServiceComponentHostSummary  {

  private Collection<HostComponentStateEntity> allHostComponents;
  private Collection<HostComponentStateEntity> versionedHostComponents;
  private Collection<HostComponentStateEntity> noVersionNeededComponents;


  public ServiceComponentHostSummary(AmbariMetaInfo ambariMetaInfo, HostEntity host, String stackName, String stackVersion) throws AmbariException {
    allHostComponents = host.getHostComponentStateEntities();
    versionedHostComponents = new HashSet<HostComponentStateEntity>();
    noVersionNeededComponents = new HashSet<HostComponentStateEntity>();

    for (HostComponentStateEntity hostComponentStateEntity: allHostComponents) {
      if (!hostComponentStateEntity.getVersion().equalsIgnoreCase(State.UNKNOWN.toString())) {
        versionedHostComponents.add(hostComponentStateEntity);
      } else {
        // Some Components cannot advertise a version. E.g., ZKF, AMS, Kerberos
        ComponentInfo compInfo = ambariMetaInfo.getComponent(
            stackName, stackVersion, hostComponentStateEntity.getServiceName(),
            hostComponentStateEntity.getComponentName());

        if (!compInfo.isVersionAdvertised()) {
          noVersionNeededComponents.add(hostComponentStateEntity);
        }
      }
    }
  }

  public ServiceComponentHostSummary(AmbariMetaInfo ambariMetaInfo, HostEntity host, StackId stackId) throws AmbariException {
    this(ambariMetaInfo, host, stackId.getStackName(), stackId.getStackVersion());
  }

  public Collection<HostComponentStateEntity> getAllHostComponents() {
    return allHostComponents;
  }

  public Collection<HostComponentStateEntity> getVersionedHostComponents() {
    return versionedHostComponents;
  }

  public Collection<HostComponentStateEntity> getNoVersionNeededComponents() {
    return noVersionNeededComponents;
  }

  /**
   * Determine if all of the components on this host have finished advertising a version, which occurs when all of the
   * components that advertise a version, plus the components that do not advertise a version, equal the total number
   * of components.
   * @return Return a bool indicating if all components that can report a version have done so.
   */
  public boolean haveAllComponentsFinishedAdvertisingVersion() {
    return allHostComponents.size() == versionedHostComponents.size() + noVersionNeededComponents.size();
  }

  /**
   * Checks that every component has the same version
   *
   * @param hostComponents host components
   * @return true if components have the same version, or collection is empty, false otherwise.
   */
  public static boolean haveSameVersion(Collection<HostComponentStateEntity> hostComponents) {
    // It is important to return true even if the collection is empty because technically, there are no conflicts.
    if (hostComponents.isEmpty()) {
      return true;
    }
    String firstVersion = null;
    for (HostComponentStateEntity hostComponent : hostComponents) {
      if (!hostComponent.getVersion().isEmpty()) {
        if (firstVersion == null) {
          firstVersion = hostComponent.getVersion();
        } else {
          if (!StringUtils.equals(firstVersion, hostComponent.getVersion())) {
            return false;
          }
        }
      }
    }
    return true;
  }
}
