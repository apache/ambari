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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.state.UpgradeContext;
import org.apache.ambari.server.state.stack.UpgradePack.ProcessingComponent;
import org.apache.ambari.server.state.stack.upgrade.StageWrapper.Type;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Used for co-located services grouped together.
 */
@XmlType(name="colocated")
public class ColocatedGrouping extends Grouping {

  private static Logger LOG = LoggerFactory.getLogger(ColocatedGrouping.class);

  @XmlElement(name="batch")
  public Batch batch;


  /**
   * {@inheritDoc}
   */
  @Override
  public StageWrapperBuilder getBuilder() {
    return new MultiHomedBuilder(this, batch, performServiceCheck);
  }

  private static class MultiHomedBuilder extends StageWrapperBuilder {

    private Batch m_batch;
    private boolean m_serviceCheck = true;

    // !!! host -> list of tasks
    private Map<String, List<TaskProxy>> initialBatch = new LinkedHashMap<String, List<TaskProxy>>();
    private Map<String, List<TaskProxy>> finalBatches = new LinkedHashMap<String, List<TaskProxy>>();


    private MultiHomedBuilder(Grouping grouping, Batch batch, boolean serviceCheck) {
      super(grouping);

      m_batch = batch;
      m_serviceCheck = serviceCheck;
    }

    @Override
    public void add(UpgradeContext context, HostsType hostsType, String service,
        boolean clientOnly, ProcessingComponent pc, Map<String, String> params) {

      int count = Double.valueOf(Math.ceil(
          (double) m_batch.percent / 100 * hostsType.hosts.size())).intValue();

      int i = 0;
      for (String host : hostsType.hosts) {
        // This class required inserting a single host into the collection
        HostsType singleHostsType = new HostsType();
        singleHostsType.hosts.add(host);

        Map<String, List<TaskProxy>> targetMap = ((i++) < count) ? initialBatch : finalBatches;
        List<TaskProxy> targetList = targetMap.get(host);
        if (null == targetList) {
          targetList = new ArrayList<TaskProxy>();
          targetMap.put(host, targetList);
        }

        TaskProxy proxy = null;

        List<Task> tasks = resolveTasks(context, true, pc);

        if (null != tasks && tasks.size() > 0) {
          // Our assumption is that all of the tasks in the StageWrapper are of
          // the same type.
          StageWrapper.Type type = tasks.get(0).getStageWrapperType();

          proxy = new TaskProxy();
          proxy.clientOnly = clientOnly;
          proxy.message = getStageText("Preparing",
              context.getComponentDisplay(service, pc.name), Collections.singleton(host));
          proxy.tasks.addAll(TaskWrapperBuilder.getTaskList(service, pc.name, singleHostsType, tasks, params));
          proxy.service = service;
          proxy.component = pc.name;
          proxy.type = type;
          targetList.add(proxy);
        }

        // !!! FIXME upgrade definition have only one step, and it better be a restart
        Task t = resolveTask(context, pc);
        if (null != t && RestartTask.class.isInstance(t)) {
          proxy = new TaskProxy();
          proxy.clientOnly = clientOnly;
          proxy.tasks.add(new TaskWrapper(service, pc.name, Collections.singleton(host), params, t));
          proxy.restart = true;
          proxy.service = service;
          proxy.component = pc.name;
          proxy.type = Type.RESTART;
          proxy.message = getStageText("Restarting",
              context.getComponentDisplay(service, pc.name), Collections.singleton(host));
          targetList.add(proxy);
        }

        tasks = resolveTasks(context, false, pc);

        if (null != tasks && tasks.size() > 0) {
          // Our assumption is that all of the tasks in the StageWrapper are of
          // the same type.
          StageWrapper.Type type = tasks.get(0).getStageWrapperType();

          proxy = new TaskProxy();
          proxy.clientOnly = clientOnly;
          proxy.component = pc.name;
          proxy.service = service;
          proxy.type = type;
          proxy.tasks.addAll(TaskWrapperBuilder.getTaskList(service, pc.name, singleHostsType, tasks, params));
          proxy.message = getStageText("Completing",
              context.getComponentDisplay(service, pc.name), Collections.singleton(host));
          targetList.add(proxy);
        }
      }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public List<StageWrapper> build(UpgradeContext upgradeContext,
        List<StageWrapper> stageWrappers) {
      List<StageWrapper> results = new ArrayList<StageWrapper>(stageWrappers);

      if (LOG.isDebugEnabled()) {
        LOG.debug("RU initial: {}", initialBatch);
        LOG.debug("RU final: {}", finalBatches);
      }

      List<StageWrapper> befores = fromProxies(upgradeContext.getDirection(), initialBatch);
      results.addAll(befores);

      if (!befores.isEmpty()) {

        ManualTask task = new ManualTask();
        task.summary = m_batch.summary;
        List<String> messages =  new ArrayList<String>();
        messages.add(m_batch.message);
        task.messages = messages;
        formatFirstBatch(upgradeContext, task, befores);

        StageWrapper wrapper = new StageWrapper(
            StageWrapper.Type.SERVER_SIDE_ACTION,
            "Validate Partial " + upgradeContext.getDirection().getText(true),
            new TaskWrapper(null, null, Collections.<String>emptySet(), task));

        results.add(wrapper);
      }

      results.addAll(fromProxies(upgradeContext.getDirection(), finalBatches));

      return results;
    }

    private List<StageWrapper> fromProxies(Direction direction,
        Map<String, List<TaskProxy>> wrappers) {
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
              wrapper = new StageWrapper(t.type, t.message, t.getTasksArray());
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

      if (direction.isUpgrade() && m_serviceCheck && serviceChecks.size() > 0) {
        // !!! add the service check task
        List<TaskWrapper> tasks = new ArrayList<TaskWrapper>();
        Set<String> displays = new HashSet<String>();
        for (String service : serviceChecks) {
          tasks.add(new TaskWrapper(service, "", Collections.<String>emptySet(), new ServiceCheckTask()));
          displays.add(service);
        }

        StageWrapper wrapper = new StageWrapper(
            StageWrapper.Type.SERVICE_CHECK,
            "Service Check " + StringUtils.join(displays, ", "),
            tasks.toArray(new TaskWrapper[tasks.size()]));

        results.add(wrapper);
      }

      return results;
    }

    /**
     * Formats the first batch's text and adds json for use if needed.
     * @param ctx       the upgrade context to load component display names
     * @param task      the manual task representing the verification message
     * @param wrappers  the list of stage wrappers
     */
    private void formatFirstBatch(UpgradeContext ctx, ManualTask task, List<StageWrapper> wrappers) {
      Set<String> names = new LinkedHashSet<String>();
      Map<String, Set<String>> compLocations = new HashMap<String, Set<String>>();

      for (StageWrapper sw : wrappers) {
        for (TaskWrapper tw : sw.getTasks()) {
          if (StringUtils.isNotEmpty(tw.getService()) &&
              StringUtils.isNotBlank(tw.getComponent())) {

            for (String host : tw.getHosts()) {
              if (!compLocations.containsKey(host)) {
                compLocations.put(host, new HashSet<String>());
              }
              compLocations.get(host).add(tw.getComponent());
            }

            names.add(ctx.getComponentDisplay(
                tw.getService(), tw.getComponent()));
          }
        }
      }

      for(int i = 0; i < task.messages.size(); i++){
        String message = task.messages.get(i);
        // !!! add the display names to the message, if needed
        if (message.contains("{{components}}")) {
          StringBuilder sb = new StringBuilder();

          List<String> compNames = new ArrayList<String>(names);

          if (compNames.size() == 1) {
            sb.append(compNames.get(0));
          } else if (names.size() > 1) {
            String last = compNames.remove(compNames.size() - 1);
            sb.append(StringUtils.join(compNames, ", "));
            sb.append(" and ").append(last);
          }

          message = message.replace("{{components}}", sb.toString());

          //Add the updated message back to the message list.
          task.messages.set(i, message);
        }
      }

      // !!! build the structured out to attach to the manual task
      JsonArray arr = new JsonArray();
      for (Entry<String, Set<String>> entry : compLocations.entrySet()) {
        JsonObject obj = new JsonObject();
        obj.addProperty("host_name", entry.getKey());

        JsonArray comps = new JsonArray();
        for (String comp : entry.getValue()) {
          comps.add(new JsonPrimitive(comp));
        }
        obj.add("components", comps);

        arr.add(obj);
      }

      JsonObject master = new JsonObject();
      master.add("topology", arr);

      task.structuredOut = master.toString();
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
    private Type type;
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
