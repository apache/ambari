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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.UpgradePack.ProcessingComponent;
import org.apache.ambari.server.state.stack.upgrade.Grouping;
import org.apache.ambari.server.state.stack.upgrade.StageWrapper;
import org.apache.ambari.server.state.stack.upgrade.StageWrapperBuilder;
import org.apache.ambari.server.state.stack.upgrade.TaskWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to assist with upgrading a cluster.
 */
public class UpgradeHelper {

  private static Logger LOG = LoggerFactory.getLogger(UpgradeHelper.class);

  /**
   * Generates a list of UpgradeGroupHolder items that are used to execute an upgrade
   * @param cluster the cluster
   * @param upgradePack the upgrade pack
   * @return the list of holders
   */
  public List<UpgradeGroupHolder> createUpgrade(Cluster cluster, UpgradePack upgradePack) {

    Map<String, Map<String, ProcessingComponent>> allTasks = upgradePack.getTasks();

    List<UpgradeGroupHolder> groups = new ArrayList<UpgradeGroupHolder>();

    for (Grouping group : upgradePack.getGroups()) {
      UpgradeGroupHolder groupHolder = new UpgradeGroupHolder();
      groupHolder.name = group.name;
      groupHolder.title = group.title;
      groups.add(groupHolder);

      StageWrapperBuilder builder = group.getBuilder();

      for (UpgradePack.OrderService service : group.services) {

        if (!allTasks.containsKey(service.serviceName)) {
          continue;
        }

        for (String component : service.components) {
          if (!allTasks.get(service.serviceName).containsKey(component)) {
            continue;
          }

          Set<String> componentHosts = getClusterHosts(cluster, service.serviceName, component);

          if (0 == componentHosts.size()) {
            continue;
          }

          ProcessingComponent pc = allTasks.get(service.serviceName).get(component);

          builder.add(componentHosts, service.serviceName, pc);
        }
      }

      List<StageWrapper> proxies = builder.build();

      if (LOG.isDebugEnabled()) {
        LOG.debug(group.name);

        int i = 0;
        for (StageWrapper proxy : proxies) {
          LOG.debug("  Stage {}", Integer.valueOf(i++));
          int j = 0;

          for (TaskWrapper task : proxy.getTasks()) {
            LOG.debug("    Task {} {}", Integer.valueOf(j++), task);
          }
        }
      }

      groupHolder.items = proxies;
    }

    return groups;

  }

  /**
   * @param cluster the cluster
   * @param serviceName name of the service
   * @param componentName name of the component
   * @return the set of hosts for the provided service and component
   */
  private Set<String> getClusterHosts(Cluster cluster, String serviceName, String componentName) {
    Map<String, Service> services = cluster.getServices();

    if (!services.containsKey(serviceName)) {
      return Collections.emptySet();
    }

    Service service = services.get(serviceName);
    Map<String, ServiceComponent> components = service.getServiceComponents();

    if (!components.containsKey(componentName) ||
        components.get(componentName).getServiceComponentHosts().size() == 0) {
      return Collections.emptySet();
    }

    return components.get(componentName).getServiceComponentHosts().keySet();
  }

  /**
   * Short-lived objects that hold information about upgrade groups
   */
  public static class UpgradeGroupHolder {
    /**
     * The name
     */
    public String name;
    /**
     * The title
     */
    public String title;

    /**
     * List of stages for the group
     */
    public List<StageWrapper> items = new ArrayList<StageWrapper>();
  }



}
