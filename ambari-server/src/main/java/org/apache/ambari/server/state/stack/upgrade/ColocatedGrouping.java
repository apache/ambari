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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.state.stack.UpgradePack.ProcessingComponent;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used for co-located services grouped together.
 */
@XmlType(name="colocated")
public class ColocatedGrouping extends Grouping {

  private static Logger LOG = LoggerFactory.getLogger(ColocatedGrouping.class);

  @XmlElement(name="batch")
  public Batch batch;


  @Override
  public StageWrapperBuilder getBuilder() {
    return new MultiHomedBuilder(batch);
  }

  private static class MultiHomedBuilder extends StageWrapperBuilder {

    private Batch batch;

    // !!! host -> list of tasks
    private Map<String, List<TaskProxy>> initialBatch = new LinkedHashMap<String, List<TaskProxy>>();
    private Map<String, List<TaskProxy>> finalBatches = new LinkedHashMap<String, List<TaskProxy>>();


    private MultiHomedBuilder(Batch batch) {
      this.batch = batch;
    }

    @Override
    public void add(HostsType hostsType, String service,
        boolean forUpgrade, boolean clientOnly, ProcessingComponent pc) {

      int count = Double.valueOf(Math.ceil(
          (double) batch.percent / 100 * hostsType.hosts.size())).intValue();

      int i = 0;
      for (String host : hostsType.hosts) {
        // This class required inserting a single host into the collection
        HostsType singleHostsType = new HostsType();
        singleHostsType.hosts = Collections.singleton(host);

        Map<String, List<TaskProxy>> targetMap = ((i++) < count) ? initialBatch : finalBatches;
        List<TaskProxy> targetList = targetMap.get(host);
        if (null == targetList) {
          targetList = new ArrayList<TaskProxy>();
          targetMap.put(host, targetList);
        }

        TaskProxy proxy = null;

        List<Task> tasks = resolveTasks(forUpgrade, true, pc);

        if (null != tasks && tasks.size() > 0) {
          proxy = new TaskProxy();
          proxy.clientOnly = clientOnly;
          proxy.message = getStageText("Preparing", pc.name, Collections.singleton(host));
          proxy.tasks.addAll(TaskWrapperBuilder.getTaskList(service, pc.name, singleHostsType, tasks));
          proxy.service = service;
          proxy.component = pc.name;
          targetList.add(proxy);
        }

        // !!! FIXME upgrade definition have only one step, and it better be a restart
        if (null != pc.tasks && 1 == pc.tasks.size()) {
          Task t = pc.tasks.get(0);
          if (RestartTask.class.isInstance(t)) {
            proxy = new TaskProxy();
            proxy.clientOnly = clientOnly;
            proxy.tasks.add(new TaskWrapper(service, pc.name, Collections.singleton(host), t));
            proxy.restart = true;
            proxy.service = service;
            proxy.component = pc.name;
            proxy.message = getStageText("Restarting ", pc.name, Collections.singleton(host));

            targetList.add(proxy);
          }
        }

        tasks = resolveTasks(forUpgrade, false, pc);

        if (null != tasks && tasks.size() > 0) {
          proxy = new TaskProxy();
          proxy.clientOnly = clientOnly;
          proxy.component = pc.name;
          proxy.service = service;
          proxy.tasks.addAll(TaskWrapperBuilder.getTaskList(service, pc.name, singleHostsType, tasks));
          proxy.message = getStageText("Completing", pc.name, Collections.singleton(host));
          targetList.add(proxy);
        }
      }
    }


    @Override
    public List<StageWrapper> build() {
      List<StageWrapper> results = new ArrayList<StageWrapper>();

      if (LOG.isDebugEnabled()) {
        LOG.debug("RU initial: {}", initialBatch);
        LOG.debug("RU final: {}", finalBatches);
      }

      results.addAll(fromProxies(initialBatch));

      // !!! TODO when manual tasks are ready
      ManualTask task = new ManualTask();
      task.summary = batch.summary;
      task.message = batch.message;

      StageWrapper wrapper = new StageWrapper(
          StageWrapper.Type.SERVER_SIDE_ACTION,
          "Validate Partial Upgrade",
          new TaskWrapper(null, null, Collections.<String>emptySet(), task));
      results.add(wrapper);

      results.addAll(fromProxies(finalBatches));

      return results;
    }

    private List<StageWrapper> fromProxies(Map<String, List<TaskProxy>> wrappers) {
      List<StageWrapper> results = new ArrayList<StageWrapper>();

      Set<String> serviceChecks = new HashSet<String>();

      for (Entry<String, List<TaskProxy>> entry : wrappers.entrySet()) {

        // !!! stage per host, per type
        StageWrapper wrapper = null;
        List<StageWrapper> execwrappers = new ArrayList<StageWrapper>();

        for (TaskProxy t : entry.getValue()) {
          if (!t.clientOnly) {
            serviceChecks.add(t.service);
          }

          if (!t.restart) {
            if (null == wrapper) {
              wrapper = new StageWrapper(StageWrapper.Type.RU_TASKS, t.message, t.getTasksArray());
            }
          } else {
            execwrappers.add(new StageWrapper(StageWrapper.Type.RESTART, t.message, t.getTasksArray()));
          }
        }

        if (null != wrapper) {
          results.add(wrapper);
        }

        if (execwrappers.size() > 0) {
          results.addAll(execwrappers);
        }

      }

      if (serviceChecks.size() > 0) {
        // !!! add the service check task
        List<TaskWrapper> tasks = new ArrayList<TaskWrapper>();
        for (String service : serviceChecks) {
          tasks.add(new TaskWrapper(service, "", Collections.<String>emptySet(), new ServiceCheckTask()));
        }

        StageWrapper wrapper = new StageWrapper(
            StageWrapper.Type.SERVICE_CHECK,
            "Service Check " + StringUtils.join(serviceChecks, ", "),
            tasks.toArray(new TaskWrapper[tasks.size()]));

        results.add(wrapper);
      }

      return results;
    }

  }

  /**
   * Represents all the tasks that need to be run for a host
   */
  private static class TaskProxy {
    private boolean restart = false;
    private String service;
    private String component;
    private String message;
    private boolean clientOnly = false;
    private List<TaskWrapper> tasks = new ArrayList<TaskWrapper>();

    @Override
    public String toString() {
      String s = "";
      for (TaskWrapper t : tasks) {
        s += component + "/" + t.getTasks() + " ";
      }

      return s;
    }

    private TaskWrapper[] getTasksArray() {
      return tasks.toArray(new TaskWrapper[0]);
    }
  }

}
