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
package org.apache.ambari.server.stack.upgrade.orchestrate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.stack.upgrade.ExecuteHostType;
import org.apache.ambari.server.stack.upgrade.ExecuteTask;
import org.apache.ambari.server.stack.upgrade.Grouping;
import org.apache.ambari.server.stack.upgrade.ParallelClientGrouping;
import org.apache.ambari.server.stack.upgrade.ServiceCheckGrouping.ServiceCheckStageWrapper;
import org.apache.ambari.server.stack.upgrade.Task;
import org.apache.ambari.server.stack.upgrade.Task.Type;
import org.apache.ambari.server.stack.upgrade.UpgradePack.ProcessingComponent;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.collections.CollectionUtils;

/**
 * Responsible for building the stages for {@link ParallelClientGrouping}
 */
public class ParallelClientGroupingBuilder extends StageWrapperBuilder {

  private Map<String, HostHolder> serviceToHostMap = new HashMap<>();

  /**
   * @param grouping
   */
  public ParallelClientGroupingBuilder(Grouping grouping) {
    super(grouping);
  }

  @Override
  public void add(UpgradeContext upgradeContext, HostsType hostsType, String service,
      boolean clientOnly, ProcessingComponent pc, Map<String, String> params) {

    if (null == hostsType || CollectionUtils.isEmpty(hostsType.getHosts())) {
      return;
    }

    Iterator<String> hostIterator = hostsType.getHosts().iterator();
    HostHolder holder = new HostHolder();
    holder.m_firstHost = hostIterator.next();

    while (hostIterator.hasNext()) {
      holder.m_remainingHosts.add(hostIterator.next());
    }

    holder.m_component = pc;
    holder.m_tasks = new ArrayList<>();
    holder.m_tasks.addAll(resolveTasks(upgradeContext, true, pc));
    holder.m_tasks.add(resolveTask(upgradeContext, pc));
    holder.m_tasks.addAll(resolveTasks(upgradeContext, false, pc));

    serviceToHostMap.put(service, holder);
  }

  @Override
  public List<StageWrapper> build(UpgradeContext upgradeContext, List<StageWrapper> stageWrappers) {

    if (0 == serviceToHostMap.size()) {
      return stageWrappers;
    }

    List<StageWrapper> starterUpgrades = new ArrayList<>();
    List<StageWrapper> finisherUpgrades = new ArrayList<>();

    // !!! create a stage wrapper for the first host tasks
    // !!! create a stage wrapper for the service check on the first host
    // !!! create a stage wrapper for the remaining hosts

    serviceToHostMap.forEach((service, holder) -> {
      String component = holder.m_component.name;

      List<TaskWrapper> wrappers = buildWrappers(service, component, holder.m_tasks,
          Collections.singleton(holder.m_firstHost), true);

      String text = getStageText("Upgrading",
          upgradeContext.getComponentDisplay(service, component),
          Collections.singleton(holder.m_firstHost));

      // !!! this is a poor assumption
      StageWrapper.Type type = wrappers.get(0).getTasks().get(0).getStageWrapperType();

      StageWrapper stage = new StageWrapper(type, text, new HashMap<>(), wrappers);

      // !!! force the service check on the first host
      StageWrapper serviceCheck = new ServiceCheckStageWrapper(service,
          upgradeContext.getServiceDisplay(service), false, holder.m_firstHost);

      starterUpgrades.add(stage);
      starterUpgrades.add(serviceCheck);

      wrappers = buildWrappers(service, component, holder.m_tasks, holder.m_remainingHosts, false);

      text = getStageText("Upgrade Remaining",
          upgradeContext.getComponentDisplay(service, component),
          holder.m_remainingHosts);
      stage = new StageWrapper(type, text, new HashMap<>(), wrappers);

      finisherUpgrades.add(stage);
    });

    List<StageWrapper> results = new ArrayList<>(stageWrappers);

    results.addAll(starterUpgrades);
    results.addAll(finisherUpgrades);

    return results;
  }

  /**
   * Build the wrappers for the tasks.
   *
   * @param tasks
   *          the tasks to wrap
   * @param hosts
   *          the hosts where the tasks should run
   * @return
   */
  private List<TaskWrapper> buildWrappers(String service, String component,
      List<Task> tasks, Set<String> hosts, boolean firstHost) {

    List<TaskWrapper> results = new ArrayList<>();

    String ambariServerHostname = StageUtils.getHostName();

    for (Task task : tasks) {

      // only add the server-side task if there are actual hosts for the service/component
      if (task.getType().isServerAction()) {
        results.add(new TaskWrapper(service, component, Collections.singleton(ambariServerHostname), task));
        continue;
      }

      // FIXME how to handle master-only types

      // !!! the first host has already run tasks that are singular
      if (!firstHost && task.getType() == Type.EXECUTE) {
        ExecuteTask et = (ExecuteTask) task;

        // !!! singular types have already run when firstHost is true
        if (et.hosts != ExecuteHostType.ALL) {
          continue;
        }
      }

      results.add(new TaskWrapper(service, component, hosts, task));
    }

    return results;
  }

  /**
   * Temporary holder for building stage wrappers
   */
  private static class HostHolder {
    private ProcessingComponent m_component;
    private String m_firstHost;
    private Set<String> m_remainingHosts = new HashSet<>();
    private List<Task> m_tasks;
  }

}
