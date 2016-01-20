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

import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.state.UpgradeContext;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.UpgradePack.ProcessingComponent;
import org.apache.ambari.server.utils.SetUtils;
import org.apache.commons.lang.StringUtils;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
@XmlSeeAlso(value = { ColocatedGrouping.class, ClusterGrouping.class, UpdateStackGrouping.class, ServiceCheckGrouping.class, RestartGrouping.class, StartGrouping.class, StopGrouping.class })
public class Grouping {

  @XmlAttribute(name="name")
  public String name;

  @XmlAttribute(name="title")
  public String title;

  @XmlElement(name="skippable", defaultValue="false")
  public boolean skippable = false;

  @XmlElement(name = "supports-auto-skip-failure", defaultValue = "true")
  public boolean supportsAutoSkipOnFailure = true;

  @XmlElement(name="allow-retry", defaultValue="true")
  public boolean allowRetry = true;

  @XmlElement(name="service")
  public List<UpgradePack.OrderService> services = new ArrayList<UpgradePack.OrderService>();

  @XmlElement(name="service-check", defaultValue="true")
  public boolean performServiceCheck = true;

  @XmlElement(name="direction")
  public Direction intendedDirection = null;

  @XmlElement(name="parallel-scheduler")
  public ParallelScheduler parallelScheduler;

  /**
   * Gets the default builder.
   */
  public StageWrapperBuilder getBuilder() {
    return new DefaultBuilder(this, performServiceCheck);
  }

  private static class DefaultBuilder extends StageWrapperBuilder {

    private List<StageWrapper> m_stages = new ArrayList<StageWrapper>();
    private Set<String> m_servicesToCheck = new HashSet<String>();

    private boolean m_serviceCheck = true;

    private DefaultBuilder(Grouping grouping, boolean serviceCheck) {
      super(grouping);
      m_serviceCheck = serviceCheck;
    }

    @Override
    public void add(UpgradeContext ctx, HostsType hostsType, String service,
                    boolean clientOnly, ProcessingComponent pc, Map<String, String> params) {

      boolean forUpgrade = ctx.getDirection().isUpgrade();

      // Construct the pre tasks during Upgrade/Downgrade direction.
      // Buckets are grouped by the type, e.g., bucket of all Execute tasks, or all Configure tasks.
      List<TaskBucket> buckets = buckets(resolveTasks(forUpgrade, true, pc));
      for (TaskBucket bucket : buckets) {
        // The TaskWrappers take into account if a task is meant to run on all, any, or master.
        // A TaskWrapper may contain multiple tasks, but typically only one, and they all run on the same set of hosts.
        List<TaskWrapper> preTasks = TaskWrapperBuilder.getTaskList(service, pc.name, hostsType, bucket.tasks, params);
        List<List<TaskWrapper>> organizedTasks = organizeTaskWrappersBySyncRules(preTasks);
        for (List<TaskWrapper> tasks : organizedTasks) {
          addTasksToStageInBatches(tasks, "Preparing", ctx, service, pc, params);
        }
      }

      // Add the processing component
      if (null != pc.tasks && 1 == pc.tasks.size()) {
        Task t = pc.tasks.get(0);
        TaskWrapper tw = new TaskWrapper(service, pc.name, hostsType.hosts, params, Collections.singletonList(t));
        addTasksToStageInBatches(Collections.singletonList(tw), t.getActionVerb(), ctx, service, pc, params);
      }

      // Construct the post tasks during Upgrade/Downgrade direction.
      buckets = buckets(resolveTasks(forUpgrade, false, pc));
      for (TaskBucket bucket : buckets) {
        List<TaskWrapper> postTasks = TaskWrapperBuilder.getTaskList(service, pc.name, hostsType, bucket.tasks, params);
        List<List<TaskWrapper>> organizedTasks = organizeTaskWrappersBySyncRules(postTasks);
        for (List<TaskWrapper> tasks : organizedTasks) {
          addTasksToStageInBatches(tasks, "Completing", ctx, service, pc, params);
        }
      }

      // Potentially add a service check
      if (m_serviceCheck && !clientOnly) {
        m_servicesToCheck.add(service);
      }
    }

    /**
     * Split a list of TaskWrappers into a list of lists where a TaskWrapper that has any task with isSequential == true
     * must be a singleton in its own list.
     * @param tasks List of TaskWrappers to analyze
     * @return List of list of TaskWrappers, where each outer list is a separate stage.
     */
    private List<List<TaskWrapper>> organizeTaskWrappersBySyncRules(List<TaskWrapper> tasks) {
      List<List<TaskWrapper>> groupedTasks = new ArrayList<List<TaskWrapper>>();

      List<TaskWrapper> subTasks = new ArrayList<>();
      for (TaskWrapper tw : tasks) {
        // If an of this TaskWrapper's tasks must be on its own stage, write out the previous subtasks if possible into one complete stage.
        if (tw.isAnyTaskSequential()) {
          if (!subTasks.isEmpty()) {
            groupedTasks.add(subTasks);
            subTasks = new ArrayList<>();
          }
          groupedTasks.add(Collections.singletonList(tw));
        } else {
          subTasks.add(tw);
        }
      }

      if (!subTasks.isEmpty()) {
        groupedTasks.add(subTasks);
      }

      return groupedTasks;
    }

    /**
     * Helper function to analyze a ProcessingComponent and add its task to stages, depending on the batch size.
     * @param tasks Collection of tasks for this stage
     * @param verb Verb string to use in the title of the task
     * @param ctx Upgrade Context
     * @param service Service
     * @param pc Processing Component
     * @param params Params to add to the stage.
     */
    private void addTasksToStageInBatches(List<TaskWrapper> tasks, String verb, UpgradeContext ctx, String service, ProcessingComponent pc, Map<String, String> params) {
      if (tasks == null || tasks.isEmpty() || tasks.get(0).getTasks() == null || tasks.get(0).getTasks().isEmpty()) {
        return;
      }

      // Our assumption is that all of the tasks in the StageWrapper are of the same type.
      StageWrapper.Type type = tasks.get(0).getTasks().get(0).getStageWrapperType();

      // Expand some of the TaskWrappers into multiple based on the batch size.
      for (TaskWrapper tw : tasks) {
        List<Set<String>> hostSets = null;
        if (m_grouping.parallelScheduler != null && m_grouping.parallelScheduler.maxDegreeOfParallelism > 0) {
          hostSets = SetUtils.split(tw.getHosts(), m_grouping.parallelScheduler.maxDegreeOfParallelism);
        } else {
          hostSets = SetUtils.split(tw.getHosts(), 1);
        }

        int numBatchesNeeded = hostSets.size();
        int batchNum = 0;
        for (Set<String> hostSubset : hostSets) {
          batchNum++;
          TaskWrapper expandedTW = new TaskWrapper(tw.getService(), tw.getComponent(), hostSubset, tw.getParams(), tw.getTasks());

          String stageText = getStageText(verb, ctx.getComponentDisplay(service, pc.name), hostSubset, batchNum, numBatchesNeeded);

          StageWrapper stage = new StageWrapper(
              type,
              stageText,
              params,
              new TaskWrapper(service, pc.name, hostSubset, params, tw.getTasks()));
          m_stages.add(stage);
        }
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StageWrapper> build(UpgradeContext upgradeContext,
        List<StageWrapper> stageWrappers) {

      // insert all pre-processed stage wrappers first
      if (!stageWrappers.isEmpty()) {
        m_stages.addAll(0, stageWrappers);
      }

      List<TaskWrapper> tasks = new ArrayList<TaskWrapper>();
      List<String> displays = new ArrayList<String>();
      for (String service : m_servicesToCheck) {
        tasks.add(new TaskWrapper(
            service, "", Collections.<String>emptySet(), new ServiceCheckTask()));

        displays.add(upgradeContext.getServiceDisplay(service));
      }

      if (upgradeContext.getDirection().isUpgrade() && m_serviceCheck
          && m_servicesToCheck.size() > 0) {
        StageWrapper wrapper = new StageWrapper(StageWrapper.Type.SERVICE_CHECK,
            "Service Check " + StringUtils.join(displays, ", "), tasks.toArray(new TaskWrapper[0]));

        m_stages.add(wrapper);
      }

      return m_stages;
    }
  }

  /**
   * Group all like-typed tasks together.  When they change, create a new type.
   */
  private static List<TaskBucket> buckets(List<Task> tasks) {
    if (null == tasks || tasks.isEmpty()) {
      return Collections.emptyList();
    }

    List<TaskBucket> holders = new ArrayList<TaskBucket>();

    TaskBucket current = null;

    int i = 0;
    for (Task t : tasks) {
      if (i == 0) {
        current = new TaskBucket(t);
        holders.add(current);
      } else if (i > 0 && t.getType() != tasks.get(i-1).getType()) {
        current = new TaskBucket(t);
        holders.add(current);
      } else {
        current.tasks.add(t);
      }

      i++;
    }

    return holders;
  }

  private static class TaskBucket {

    private StageWrapper.Type type;

    private List<Task> tasks = new ArrayList<Task>();

    private TaskBucket(Task initial) {
      switch (initial.getType()) {
        case CONFIGURE:
        case SERVER_ACTION:
        case MANUAL:
          type = StageWrapper.Type.SERVER_SIDE_ACTION;
          break;
        case EXECUTE:
          type = StageWrapper.Type.RU_TASKS;
          break;
        case CONFIGURE_FUNCTION:
          type = StageWrapper.Type.CONFIGURE;
          break;
        case RESTART:
          type = StageWrapper.Type.RESTART;
          break;
        case START:
          type = StageWrapper.Type.START;
          break;
        case STOP:
          type = StageWrapper.Type.STOP;
          break;
        case SERVICE_CHECK:
          type = StageWrapper.Type.SERVICE_CHECK;
          break;
      }
      tasks.add(initial);
    }
  }
}
