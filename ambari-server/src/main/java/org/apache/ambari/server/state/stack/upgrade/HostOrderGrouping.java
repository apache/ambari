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
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlType;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.UpgradeContext;
import org.apache.ambari.server.state.stack.UpgradePack.ProcessingComponent;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * Marker group for Host-Ordered upgrades.
 */
@XmlType(name="host-order")
public class HostOrderGrouping extends Grouping {
  private static final String TYPE = "type";
  private static final String HOST = "host";
  private static Logger LOG = LoggerFactory.getLogger(HostOrderGrouping.class);

  /**
   * Contains the ordered actions to schedule for this grouping.
   */
  private List<HostOrderItem> m_hostOrderItems;

  /**
   * Constructor
   */
  public HostOrderGrouping() {
  }

  /**
   * Sets the {@link HostOrderItem}s on this grouping.
   *
   * @param hostOrderItems
   */
  public void setHostOrderItems(List<HostOrderItem> hostOrderItems) {
    m_hostOrderItems = hostOrderItems;
  }

  @Override
  public StageWrapperBuilder getBuilder() {
    return new HostBuilder(this);
  }

  /**
   * Builder for host upgrades.
   */
  private static class HostBuilder extends StageWrapperBuilder {
    private final List<HostOrderItem> hostOrderItems;

    /**
     * @param grouping the grouping
     */
    protected HostBuilder(HostOrderGrouping grouping) {
      super(grouping);
      hostOrderItems = grouping.m_hostOrderItems;
    }

    @Override
    public void add(UpgradeContext upgradeContext, HostsType hostsType, String service,
        boolean clientOnly, ProcessingComponent pc, Map<String, String> params) {
      // !!! NOOP, this called when there are services in the group, and there
      // are none for host-ordered.
    }

    @Override
    public List<StageWrapper> build(UpgradeContext upgradeContext,
        List<StageWrapper> stageWrappers) {

      List<StageWrapper> wrappers = new ArrayList<>(stageWrappers);

      for (HostOrderItem orderItem : hostOrderItems) {
        switch (orderItem.getType()) {
          case HOST_UPGRADE:
            wrappers.addAll(buildHosts(upgradeContext, orderItem.getActionItems()));
            break;
          case SERVICE_CHECK:
            wrappers.addAll(buildServiceChecks(upgradeContext, orderItem.getActionItems()));
            break;
        }
      }

      return wrappers;
    }

    /**
     * @param upgradeContext  the context
     * @param hosts           the list of hostnames
     * @return  the wrappers for a host
     */
    private List<StageWrapper> buildHosts(UpgradeContext upgradeContext, List<String> hosts) {
      if (CollectionUtils.isEmpty(hosts)) {
        return Collections.emptyList();
      }

      Cluster cluster = upgradeContext.getCluster();
      List<StageWrapper> wrappers = new ArrayList<>();

      for (String hostName : hosts) {

        List<TaskWrapper> stopTasks = new ArrayList<>();
        List<TaskWrapper> upgradeTasks = new ArrayList<>();

        for (ServiceComponentHost sch : cluster.getServiceComponentHosts(hostName)) {
          if (!isVersionAdvertised(upgradeContext, sch)) {
            continue;
          }

          HostsType hostsType = upgradeContext.getResolver().getMasterAndHosts(
              sch.getServiceName(), sch.getServiceComponentName());

          // !!! if the hosts do not contain the current one, that means the component
          // either doesn't exist or the downgrade is to the current target version.
          // hostsType better not be null either, but check anyway
          if (null != hostsType && !hostsType.hosts.contains(hostName)) {
            LOG.warn("Host {} could not be orchestrated. Either there are no components for {}/{} " +
                "or the target version {} is already current.",
                hostName, sch.getServiceName(), sch.getServiceComponentName(), upgradeContext.getVersion());
            continue;
          }

          if (!sch.isClientComponent()) {
            stopTasks.add(new TaskWrapper(sch.getServiceName(), sch.getServiceComponentName(),
                Collections.singleton(hostName), new StopTask()));
          }

          // !!! simple restart will do
          upgradeTasks.add(new TaskWrapper(sch.getServiceName(), sch.getServiceComponentName(),
              Collections.singleton(hostName), new RestartTask()));
        }

        if (stopTasks.isEmpty() && upgradeTasks.isEmpty()) {
          LOG.info("No tasks for {}", hostName);
          continue;
        }

        StageWrapper stopWrapper = new StageWrapper(StageWrapper.Type.STOP, String.format("Stop on %s", hostName),
            stopTasks.toArray(new TaskWrapper[stopTasks.size()]));

        StageWrapper startWrapper = new StageWrapper(StageWrapper.Type.RESTART, String.format("Start on %s", hostName),
            upgradeTasks.toArray(new TaskWrapper[upgradeTasks.size()]));

        String message = String.format("Please acknowledge that host %s has been prepared.", hostName);

        ManualTask mt = new ManualTask();
        mt.messages.add(message);
        JsonObject structuredOut = new JsonObject();
        structuredOut.addProperty(TYPE, HostOrderItem.HostOrderActionType.HOST_UPGRADE.toString());
        structuredOut.addProperty(HOST, hostName);
        mt.structuredOut = structuredOut.toString();

        StageWrapper manualWrapper = new StageWrapper(StageWrapper.Type.SERVER_SIDE_ACTION, "Manual Confirmation",
            new TaskWrapper(null, null, Collections.<String>emptySet(), mt));

        wrappers.add(stopWrapper);
        wrappers.add(manualWrapper);
        // !!! TODO install_packages for hdp and conf-select changes.  Hopefully these will no-op.
        wrappers.add(startWrapper);

      }

      return wrappers;
    }

    /**
     * @param upgradeContext  the context
     * @param hosts           the list of hostnames
     * @return  the wrappers for a host
     */
    private List<StageWrapper> buildServiceChecks(UpgradeContext upgradeContext, List<String> serviceChecks) {
      if (CollectionUtils.isEmpty(serviceChecks)) {
        return Collections.emptyList();
      }

      List<StageWrapper> wrappers = new ArrayList<>();

      Cluster cluster = upgradeContext.getCluster();

      for (String serviceName : serviceChecks) {
        boolean hasService = false;
        try {
          cluster.getService(serviceName);
          hasService = true;
        } catch (Exception e) {
          LOG.warn("Service {} not found to orchestrate", serviceName);
        }

        if (!hasService) {
          continue;
        }

        StageWrapper wrapper = new StageWrapper(StageWrapper.Type.SERVICE_CHECK,
            String.format("Service Check %s", upgradeContext.getServiceDisplay(serviceName)),
            new TaskWrapper(serviceName, "", Collections.<String>emptySet(), new ServiceCheckTask()));

        wrappers.add(wrapper);
      }

      return wrappers;
    }


    /**
     * @param upgradeContext  the context
     * @param sch             the host component
     * @return                {@code true} if the host component advertises its version
     */
    private boolean isVersionAdvertised(UpgradeContext upgradeContext, ServiceComponentHost sch) {
      StackId targetStack = upgradeContext.getTargetStackId();

      try {
        ComponentInfo component = upgradeContext.getAmbariMetaInfo().getComponent(
            targetStack.getStackName(), targetStack.getStackVersion(),
            sch.getServiceName(), sch.getServiceComponentName());

        return component.isVersionAdvertised();
      } catch (AmbariException e) {
       LOG.warn("Could not determine if {}/{}/{} could be upgraded; returning false",
           targetStack, sch.getServiceName(), sch.getServiceComponentName(), e);
       return false;
      }
    }
  }

}
