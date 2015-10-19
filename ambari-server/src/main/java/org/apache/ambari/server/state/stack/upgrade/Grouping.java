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
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.state.UpgradeContext;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.UpgradePack.ProcessingComponent;
import org.apache.commons.lang.StringUtils;

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

  @XmlElement(name="allow-retry", defaultValue="true")
  public boolean allowRetry = true;

  @XmlElement(name="service")
  public List<UpgradePack.OrderService> services = new ArrayList<UpgradePack.OrderService>();

  @XmlElement(name="service-check", defaultValue="true")
  public boolean performServiceCheck = true;

  @XmlElement(name="direction")
  public Direction intendedDirection = null;

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

    /**
     * Add stages where the restart stages are ordered
     * E.g., preupgrade, restart hosts(0), ..., restart hosts(n-1), postupgrade
     * @param hostsType the order collection of hosts, which may have a master and secondary
     * @param service the service name
     * @param pc the ProcessingComponent derived from the upgrade pack.
     */
    @Override
    public void add(UpgradeContext ctx, HostsType hostsType, String service,
       boolean clientOnly, ProcessingComponent pc) {

      boolean forUpgrade = ctx.getDirection().isUpgrade();

      // Construct the pre tasks during Upgrade/Downgrade direction.
      List<TaskBucket> buckets = buckets(resolveTasks(forUpgrade, true, pc));
      for (TaskBucket bucket : buckets) {
        List<TaskWrapper> preTasks = TaskWrapperBuilder.getTaskList(service, pc.name, hostsType, bucket.tasks);
        Set<String> preTasksEffectiveHosts = TaskWrapperBuilder.getEffectiveHosts(preTasks);
        if (!preTasksEffectiveHosts.isEmpty()) {
          StageWrapper stage = new StageWrapper(
              bucket.type,
              getStageText("Preparing", ctx.getComponentDisplay(service, pc.name), preTasksEffectiveHosts),
              preTasks
              );
          m_stages.add(stage);
        }
      }

      // Add the processing component
      if (null != pc.tasks && 1 == pc.tasks.size()) {
        Task t = pc.tasks.get(0);

        for (String hostName : hostsType.hosts) {
          StageWrapper stage = new StageWrapper(
              t.getStageWrapperType(),
              getStageText(t.getActionVerb(), ctx.getComponentDisplay(service, pc.name), Collections.singleton(hostName)),
              new TaskWrapper(service, pc.name, Collections.singleton(hostName), t));
          m_stages.add(stage);
        }
      }

      // Construct the post tasks during Upgrade/Downgrade direction.
      buckets = buckets(resolveTasks(forUpgrade, false, pc));
      for (TaskBucket bucket : buckets) {
        List<TaskWrapper> postTasks = TaskWrapperBuilder.getTaskList(service, pc.name, hostsType, bucket.tasks);
        Set<String> postTasksEffectiveHosts = TaskWrapperBuilder.getEffectiveHosts(postTasks);
        if (!postTasksEffectiveHosts.isEmpty()) {
          StageWrapper stage = new StageWrapper(
              bucket.type,
              getStageText("Completing", ctx.getComponentDisplay(service, pc.name), postTasksEffectiveHosts),
              postTasks
              );
          m_stages.add(stage);
        }
      }

      // Potentially add a service check
      if (this.m_serviceCheck && !clientOnly) {
        m_servicesToCheck.add(service);
      }
    }

    /**
     * Determine if service checks need to be ran after the stages.
     * @param upgradeContext the upgrade context
     * @return Return the stages, which may potentially be followed by service checks.
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
