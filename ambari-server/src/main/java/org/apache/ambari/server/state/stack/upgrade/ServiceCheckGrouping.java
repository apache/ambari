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
package org.apache.ambari.server.state.stack.upgrade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.UpgradeContext;
import org.apache.ambari.server.state.stack.UpgradePack.ProcessingComponent;

/**
 * Grouping that is used to create stages that are service checks for a cluster.
 */
@XmlType(name="service-check")
public class ServiceCheckGrouping extends Grouping {

  @XmlElementWrapper(name="priority")
  @XmlElement(name="service")
  private Set<String> priorityServices = new HashSet<String>();

  private ServiceCheckBuilder m_builder = new ServiceCheckBuilder();

  @Override
  public ServiceCheckBuilder getBuilder() {
    return m_builder;
  }

  /**
   * Used to build stages for service check groupings.
   */
  public class ServiceCheckBuilder extends StageWrapperBuilder {
    private Cluster m_cluster;
    private AmbariMetaInfo m_metaInfo;


    @Override
    public void add(HostsType hostsType, String service, boolean forUpgrade, boolean clientOnly,
        ProcessingComponent pc) {
      // !!! nothing to do here
    }

    @Override
    public List<StageWrapper> build(UpgradeContext ctx) {
      m_cluster = ctx.getCluster();
      m_metaInfo = ctx.getAmbariMetaInfo();

      List<StageWrapper> result = new ArrayList<StageWrapper>();

      Map<String, Service> serviceMap = m_cluster.getServices();

      Set<String> clusterServices = new LinkedHashSet<String>(serviceMap.keySet());

      // create stages for the priorities
      for (String service : ServiceCheckGrouping.this.priorityServices) {
        if (checkServiceValidity(service, serviceMap)) {
          StageWrapper wrapper = new StageWrapper(
              StageWrapper.Type.SERVICE_CHECK,
              "Service Check " + service,
              new TaskWrapper(service, "", Collections.<String>emptySet(),
                  new ServiceCheckTask()));
          result.add(wrapper);

          clusterServices.remove(service);
        }
      }

      // create stages for everything else
      for (String service : clusterServices) {
        if (checkServiceValidity(service, serviceMap)) {
          StageWrapper wrapper = new StageWrapper(
              StageWrapper.Type.SERVICE_CHECK,
              "Service Check " + service,
              new TaskWrapper(service, "", Collections.<String>emptySet(),
                  new ServiceCheckTask()));
          result.add(wrapper);
        }
      }
      return result;
    }

    /**
     * Checks if the service is valid for a service check
     *
     * @param service         the name of the service to check
     * @param clusterServices the map of available services for a cluster
     * @return {@code true} if the service is valid and can execute a service check
     */
    private boolean checkServiceValidity(String service, Map<String, Service> clusterServices) {
      if (!clusterServices.containsKey(service)) {
        return false;
      } else {
        Service svc = clusterServices.get(service);
        if (null == svc) {
          return false;
        } else {
          if (svc.isClientOnlyService()) {
            return false;
          } else {
            StackId stackId = m_cluster.getDesiredStackVersion();
            try {
              ServiceInfo si = m_metaInfo.getService(stackId.getStackName(),
                  stackId.getStackVersion(), service);
              if (null == si.getCommandScript()) {
                return false;
              }
            } catch (AmbariException e) {
              return false;
            }
          }
        }
      }

      return true;
    }
  }

}
