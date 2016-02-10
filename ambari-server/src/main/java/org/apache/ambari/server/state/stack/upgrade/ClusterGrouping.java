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
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.UpgradeContext;
import org.apache.ambari.server.state.stack.UpgradePack.ProcessingComponent;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Used to represent cluster-based operations.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="cluster")
public class ClusterGrouping extends Grouping {

  /**
   * Stages against a Service and Component, or the Server, that doesn't need a Processing Component.
   */
  @XmlElement(name="execute-stage")
  public List<ExecuteStage> executionStages;

  @Override
  public ClusterBuilder getBuilder() {
    return new ClusterBuilder(this);
  }


  /**
   * Represents a single-stage execution that happens as part of a cluster-wide
   * upgrade or downgrade.
   */
  public static class ExecuteStage {
    @XmlAttribute(name="title")
    public String title;

    /**
     * An optional ID which can be used to uniquely identified any execution
     * stage.
     */
    @XmlAttribute(name="id")
    public String id;

    @XmlElement(name="direction")
    public Direction intendedDirection = null;

    /**
     * Optional service name, can be ""
     */
    @XmlAttribute(name="service")
    public String service;

    /**
     * Optional component name, can be ""
     */
    @XmlAttribute(name="component")
    public String component;

    @XmlElement(name="task")
    public Task task;
  }

  public class ClusterBuilder extends StageWrapperBuilder {

    /**
     * Constructor.
     *
     * @param grouping
     *          the upgrade/downgrade grouping (not {@code null}).
     */
    private ClusterBuilder(Grouping grouping) {
      super(grouping);
    }

    @Override
    public void add(UpgradeContext ctx, HostsType hostsType, String service,
        boolean clientOnly, ProcessingComponent pc, Map<String, String> params) {
      // !!! no-op in this case
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StageWrapper> build(UpgradeContext upgradeContext,
        List<StageWrapper> stageWrappers) {

      if (null == executionStages) {
        return stageWrappers;
      }

      List<StageWrapper> results = new ArrayList<>(stageWrappers);

      if (executionStages != null) {
        for (ExecuteStage execution : executionStages) {
          if (null != execution.intendedDirection
              && execution.intendedDirection != upgradeContext.getDirection()) {
            continue;
          }

          Task task = execution.task;

          StageWrapper wrapper = null;

          switch (task.getType()) {
            case MANUAL:
            case SERVER_ACTION:
            case CONFIGURE:
              wrapper = getServerActionStageWrapper(upgradeContext, execution);
              break;

            case EXECUTE:
              wrapper = getExecuteStageWrapper(upgradeContext, execution);
              break;

            default:
              break;
          }

          if (null != wrapper) {
            results.add(wrapper);
          }
        }
      }

      return results;
    }
  }

  /**
   * Return a Stage Wrapper for a server side action that runs on the server.
   * @param ctx Upgrade Context
   * @param execution Execution Stage
   * @return Returns a Stage Wrapper
   */
  private StageWrapper getServerActionStageWrapper(UpgradeContext ctx, ExecuteStage execution) {

    String service   = execution.service;
    String component = execution.component;
    Task task        = execution.task;

    Set<String> realHosts = Collections.emptySet();

    if (null != service && !service.isEmpty() &&
        null != component && !component.isEmpty()) {

      HostsType hosts = ctx.getResolver().getMasterAndHosts(service, component);

      if (null == hosts || hosts.hosts.isEmpty()) {
        return null;
      } else {
        realHosts = new LinkedHashSet<String>(hosts.hosts);
      }
    }

    if (Task.Type.MANUAL == task.getType()) {
      return new StageWrapper(
          StageWrapper.Type.SERVER_SIDE_ACTION,
          execution.title,
          new TaskWrapper(service, component, realHosts, task));
    } else {
      return new StageWrapper(
          StageWrapper.Type.SERVER_SIDE_ACTION,
          execution.title,
          new TaskWrapper(null, null, Collections.<String>emptySet(), task));
    }
  }

  /**
   * Return a Stage Wrapper for a task meant to execute code, typically on Ambari Server.
   * @param ctx Upgrade Context
   * @param execution Execution Stage
   * @return Returns a Stage Wrapper, or null if a valid one could not be created.
   */
  private StageWrapper getExecuteStageWrapper(UpgradeContext ctx, ExecuteStage execution) {
    String service   = execution.service;
    String component = execution.component;
    ExecuteTask et = (ExecuteTask) execution.task;

    if (null != service && !service.isEmpty() &&
        null != component && !component.isEmpty()) {

      HostsType hosts = ctx.getResolver().getMasterAndHosts(service, component);

      if (hosts != null) {

        Set<String> realHosts = new LinkedHashSet<String>(hosts.hosts);
        if (ExecuteHostType.MASTER == et.hosts && null != hosts.master) {
          realHosts = Collections.singleton(hosts.master);
        }

        // Pick a random host.
        if (ExecuteHostType.ANY == et.hosts && !hosts.hosts.isEmpty()) {
          realHosts = Collections.singleton(hosts.hosts.iterator().next());
        }

        // Pick the first host sorted alphabetically (case insensitive)
        if (ExecuteHostType.FIRST == et.hosts && !hosts.hosts.isEmpty()) {
          List<String> sortedHosts = new ArrayList<>(hosts.hosts);
          Collections.sort(sortedHosts, String.CASE_INSENSITIVE_ORDER);
          realHosts = Collections.singleton(sortedHosts.get(0));
        }

        // !!! cannot execute against empty hosts (safety net)
        if (realHosts.isEmpty()) {
          return null;
        }

        return new StageWrapper(
            StageWrapper.Type.RU_TASKS,
            execution.title,
            new TaskWrapper(service, component, realHosts, et));
      }
    } else if (null == service && null == component) {
      // no service and no component will distributed the task to all healthy
      // hosts not in maintenance mode
      Cluster cluster = ctx.getCluster();
      Set<String> hostNames = new HashSet<String>();
      for (Host host : ctx.getCluster().getHosts()) {
        MaintenanceState maintenanceState = host.getMaintenanceState(cluster.getClusterId());
        if (maintenanceState == MaintenanceState.OFF) {
          hostNames.add(host.getHostName());
        }
      }

      return new StageWrapper(
          StageWrapper.Type.RU_TASKS,
          execution.title,
          new TaskWrapper(service, component, hostNames, et));
    }
    return null;
  }

  /**
   * Populates the manual task, mt, with information about the list of hosts.
   * @param mt Manual Task
   * @param hostToComponents Map from host name to list of components
   */
  private void fillHostDetails(ManualTask mt, Map<String, List<String>> hostToComponents) {
    JsonArray arr = new JsonArray();
    for (Entry<String, List<String>> entry : hostToComponents.entrySet()) {
      JsonObject hostObj = new JsonObject();
      hostObj.addProperty("host", entry.getKey());

      JsonArray componentArr = new JsonArray();
      for (String comp : entry.getValue()) {
        componentArr.add(new JsonPrimitive(comp));
      }
      hostObj.add("components", componentArr);

      arr.add(hostObj);
    }

    JsonObject obj = new JsonObject();
    obj.add("unhealthy", arr);

    mt.structuredOut = obj.toString();
  }
}
