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
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.UpgradePack.ProcessingComponent;
import org.apache.commons.lang.StringUtils;

/**
 *
 */
@XmlSeeAlso(value = { ColocatedGrouping.class, ClusterGrouping.class })
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

  /**
   * Gets the default builder.
   */
  public StageWrapperBuilder getBuilder() {
    return new DefaultBuilder();
  }


  private static class DefaultBuilder extends StageWrapperBuilder {

    private List<StageWrapper> stages = new ArrayList<StageWrapper>();
    private Set<String> serviceChecks = new HashSet<String>();

    /**
     * Add stages where the restart stages are ordered
     * E.g., preupgrade, restart hosts(0), ..., restart hosts(n-1), postupgrade
     * @param hostsType the order collection of hosts, which may have a master and secondary
     * @param service the service name
     * @param pc the ProcessingComponent derived from the upgrade pack.
     */
    @Override
    public void add(HostsType hostsType, String service, boolean clientOnly, ProcessingComponent pc) {

      List<TaskBucket> buckets = buckets(pc.preTasks);
      for (TaskBucket bucket : buckets) {
        List<TaskWrapper> preTasks = TaskWrapperBuilder.getTaskList(service, pc.name, hostsType, bucket.tasks);
        Set<String> preTasksEffectiveHosts = TaskWrapperBuilder.getEffectiveHosts(preTasks);
        StageWrapper stage = new StageWrapper(
            bucket.type,
            getStageText("Preparing", pc.name, preTasksEffectiveHosts),
            preTasks
            );
        stages.add(stage);
      }

      // !!! FIXME upgrade definition have only one step, and it better be a restart
      if (null != pc.tasks && 1 == pc.tasks.size()) {
        Task t = pc.tasks.get(0);
        if (RestartTask.class.isInstance(t)) {
          for (String hostName : hostsType.hosts) {
            StageWrapper stage = new StageWrapper(
                StageWrapper.Type.RESTART,
                getStageText("Restarting", pc.name, Collections.singleton(hostName)),
                new TaskWrapper(service, pc.name, Collections.singleton(hostName), t));
            stages.add(stage);
          }
        }
      }

      buckets = buckets(pc.postTasks);
      for (TaskBucket bucket : buckets) {
        List<TaskWrapper> postTasks = TaskWrapperBuilder.getTaskList(service, pc.name, hostsType, bucket.tasks);
        Set<String> postTasksEffectiveHosts = TaskWrapperBuilder.getEffectiveHosts(postTasks);
        StageWrapper stage = new StageWrapper(
            bucket.type,
            getStageText("Completing", pc.name, postTasksEffectiveHosts),
            postTasks
            );
        stages.add(stage);
      }

      if (!clientOnly) {
        serviceChecks.add(service);
      }
    }

    @Override
    public List<StageWrapper> build() {

      List<TaskWrapper> tasks = new ArrayList<TaskWrapper>();
      for (String service : serviceChecks) {
        tasks.add(new TaskWrapper(
            service, "", Collections.<String>emptySet(), new ServiceCheckTask()));
      }

      if (serviceChecks.size() > 0) {
        StageWrapper wrapper = new StageWrapper(
            StageWrapper.Type.SERVICE_CHECK,
            "Service Check " + StringUtils.join(serviceChecks, ", "),
            tasks.toArray(new TaskWrapper[0])
            );

        stages.add(wrapper);
      }

      return stages;
    }
  }

  /**
   * Group all like-typed tasks together.  When they change, create a new type.
   */
  private static List<TaskBucket> buckets(List<Task> tasks) {
    if (null == tasks || tasks.isEmpty())
      return Collections.emptyList();

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
        case SERVICE_CHECK:
          type = StageWrapper.Type.SERVICE_CHECK;
          break;
      }
      tasks.add(initial);
    }
  }
}
