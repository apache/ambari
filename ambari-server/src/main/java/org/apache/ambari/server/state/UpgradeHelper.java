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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.internal.RequestResourceProvider;
import org.apache.ambari.server.controller.internal.StageResourceProvider;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.QueryResponse;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.stack.MasterHostResolver;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.UpgradePack.ProcessingComponent;
import org.apache.ambari.server.state.stack.upgrade.ClusterGrouping;
import org.apache.ambari.server.state.stack.upgrade.Grouping;
import org.apache.ambari.server.state.stack.upgrade.ManualTask;
import org.apache.ambari.server.state.stack.upgrade.ServiceCheckGrouping;
import org.apache.ambari.server.state.stack.upgrade.StageWrapper;
import org.apache.ambari.server.state.stack.upgrade.StageWrapperBuilder;
import org.apache.ambari.server.state.stack.upgrade.Task;
import org.apache.ambari.server.state.stack.upgrade.Task.Type;
import org.apache.ambari.server.state.stack.upgrade.TaskWrapper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Class to assist with upgrading a cluster.
 */
@Singleton
public class UpgradeHelper {

  private static final Logger LOG = LoggerFactory.getLogger(UpgradeHelper.class);

  /**
   * Matches on placeholder values such as
   *
   * <pre>
   * {{host}}
   * </pre>
   * and
   * <pre>
   * {{hdfs-site/foo}}
   * </pre>
   */
  private static final Pattern PLACEHOLDER_REGEX = Pattern.compile("(\\{\\{.*?\\}\\})");

  /**
   * A placeholder token that represents all of the hosts that a component is
   * deployed on. This can be used for cases where text needs to be rendered
   * with all of the hosts mentioned by their FQDN.
   */
  private static final String PLACEHOLDER_HOST_ALL = "{{hosts.all}}";

  /**
   * A placeholder token that represents a single, active master that a
   * component is deployed on. This can be used for cases where text needs to eb
   * rendered with a single master host FQDN inserted.
   */
  private static final String PLACEHOLDER_HOST_MASTER = "{{hosts.master}}";

  /**
   * The version that the stack is being upgraded or downgraded to, such as
   * {@code 2.2.1.0-1234}.
   */
  private static final String PLACEHOLDER_VERSION = "{{version}}";

  /**
   * Used to render parameter placeholders in {@link ManualTask}s after the
   * {@link StageWrapperBuilder} has finished building out all of the stages.
   */
  @Inject
  private Provider<ConfigHelper> m_configHelper;

  @Inject
  private Provider<AmbariMetaInfo> m_ambariMetaInfo;

  /**
   * Generates a list of UpgradeGroupHolder items that are used to execute a
   * downgrade
   *
   * @param mhr
   *          Master Host Resolver needed to get master and secondary hosts of
   *          several components like NAMENODE
   * @param upgradePack
   *          the upgrade pack
   * @param version
   *          the version of the stack that the downgrade to, such as
   *          {@code 2.2.0.0-2041}.
   * @return the list of holders
   */
  public List<UpgradeGroupHolder> createDowngrade(MasterHostResolver mhr,
      UpgradePack upgradePack, String version) throws AmbariException {
    return createSequence(mhr, upgradePack, version, false);
  }

  /**
   * Generates a list of UpgradeGroupHolder items that are used to execute an
   * upgrade
   *
   * @param mhr
   *          Master Host Resolver needed to get master and secondary hosts of
   *          several components like NAMENODE
   * @param upgradePack
   *          the upgrade pack
   * @param version
   *          the version of the stack that the upgrade is to, such as
   *          {@code 2.2.0.0-2041}.
   * @return the list of holders
   */
  public List<UpgradeGroupHolder> createUpgrade(MasterHostResolver mhr,
      UpgradePack upgradePack, String version) throws AmbariException {
    return createSequence(mhr, upgradePack, version, true);
  }


  /**
   * Generates a list of UpgradeGroupHolder items that are used to execute an
   * upgrade
   *
   * @param mhr
   *          Master Host Resolver needed to get master and secondary hosts of
   *          several components like NAMENODE
   * @param upgradePack
   *          the upgrade pack
   * @param version
   *          the version of the stack that the upgrade or downgrade is to, such
   *          as {@code 2.2.0.0-2041}.
   * @param forUpgrade
   *          {@code true} if the sequence is for an upgrade, {@code false} if
   *          for a downgrade
   * @return the list of holders
   *
   */
  private List<UpgradeGroupHolder> createSequence(MasterHostResolver mhr,
      UpgradePack upgradePack, String version, boolean forUpgrade)
      throws AmbariException {

    Cluster cluster = mhr.getCluster();
    Map<String, Map<String, ProcessingComponent>> allTasks = upgradePack.getTasks();
    List<UpgradeGroupHolder> groups = new ArrayList<UpgradeGroupHolder>();


    for (Grouping group : upgradePack.getGroups(forUpgrade)) {
      if (ClusterGrouping.class.isInstance(group)) {
        UpgradeGroupHolder groupHolder = getClusterGroupHolder(
            cluster, (ClusterGrouping) group, forUpgrade ? null : version);

        if (null != groupHolder) {
          groups.add(groupHolder);
        }

        continue;
      } else if (ServiceCheckGrouping.class.isInstance(group)) {
        ServiceCheckGrouping scg = (ServiceCheckGrouping) group;

        scg.getBuilder().setHelpers(cluster, m_ambariMetaInfo.get());

        List<StageWrapper> wrappers = scg.getBuilder().build();

        if (!wrappers.isEmpty()) {
          UpgradeGroupHolder groupHolder = new UpgradeGroupHolder();
          groupHolder.name = group.name;
          groupHolder.title = group.title;
          groupHolder.skippable = group.skippable;
          groupHolder.allowRetry = group.allowRetry;
          groupHolder.items = wrappers;
          groups.add(groupHolder);
        }

        continue;
      }

      UpgradeGroupHolder groupHolder = new UpgradeGroupHolder();
      groupHolder.name = group.name;
      groupHolder.title = group.title;
      groupHolder.skippable = group.skippable;
      groupHolder.allowRetry = group.allowRetry;

      StageWrapperBuilder builder = group.getBuilder();

      List<UpgradePack.OrderService> services = group.services;

      if (!forUpgrade) {
        List<UpgradePack.OrderService> reverse = new ArrayList<UpgradePack.OrderService>(services);
        Collections.reverse(reverse);
        services = reverse;
      }

      for (UpgradePack.OrderService service : services) {

        if (!allTasks.containsKey(service.serviceName)) {
          continue;
        }

        for (String component : service.components) {
          if (!allTasks.get(service.serviceName).containsKey(component)) {
            continue;
          }

          HostsType hostsType = mhr.getMasterAndHosts(service.serviceName, component);
          if (null == hostsType) {
            continue;
          }

          Service svc = cluster.getService(service.serviceName);
          ProcessingComponent pc = allTasks.get(service.serviceName).get(component);

          // Special case for NAMENODE
          if (service.serviceName.equalsIgnoreCase("HDFS") && component.equalsIgnoreCase("NAMENODE")) {
            // !!! revisit if needed
            if (hostsType.master != null && hostsType.secondary != null) {
              // The order is important, first do the standby, then the active namenode.
              Set<String> order = new LinkedHashSet<String>();

              // TODO Upgrade Pack, somehow, running the secondary first causes them to swap, even before the restart.
              order.add(hostsType.master);
              order.add(hostsType.secondary);

              // Override the hosts with the ordered collection
              hostsType.hosts = order;
            } else {
                throw new AmbariException(MessageFormat.format("Could not find active and standby namenodes using hosts: {0}", StringUtils.join(hostsType.hosts, ", ").toString()));
            }

            builder.add(hostsType, service.serviceName, forUpgrade,
                svc.isClientOnlyService(), pc);

          } else {
            builder.add(hostsType, service.serviceName, forUpgrade,
                svc.isClientOnlyService(), pc);
          }
        }
      }

      List<StageWrapper> proxies = builder.build();

      // post process all of the tasks
      postProcessTasks(proxies, mhr, version);

      if (!proxies.isEmpty()) {
        groupHolder.items = proxies;
        groups.add(groupHolder);
      }
    }

    if (LOG.isDebugEnabled()) {
      for (UpgradeGroupHolder group : groups) {
        LOG.debug(group.name);

        int i = 0;
        for (StageWrapper proxy : group.items) {
          LOG.debug("  Stage {}", Integer.valueOf(i++));
          int j = 0;

          for (TaskWrapper task : proxy.getTasks()) {
            LOG.debug("    Task {} {}", Integer.valueOf(j++), task);
          }
        }
      }
    }

    return groups;
  }

  /**
   * Walks through the generated upgrade hierarchy and applies special
   * processing to any tasks that require it. An example of this are manual
   * tasks that have some parameter placeholders rendered.
   *
   * @param stageWrappers
   *          the stage wrappers (not {@code null}).
   * @param mhr
   *          a helper to resolve masters, slaves, and hosts (not {@code null}).
   * @param version
   *          the version of the stack being upgraded or dowgraded to, such as
   *          {@code 2.2.0.0-1234} (not {@code null}).
   */
  private void postProcessTasks(List<StageWrapper> stageWrappers,
      MasterHostResolver mhr, String version) throws AmbariException {
    Cluster cluster = mhr.getCluster();

    for (StageWrapper stageWrapper : stageWrappers) {
      List<TaskWrapper> taskWrappers = stageWrapper.getTasks();
      for (TaskWrapper taskWrapper : taskWrappers) {
        List<Task> tasks = taskWrapper.getTasks();

        // if the task is a manual task, then render any placeholders such
        // as {{hdfs-site/foo}}
        for (Task task : tasks) {
          if (task.getType() == Type.MANUAL) {
            List<String> tokens = new ArrayList<String>(5);
            ManualTask manualTask = (ManualTask) task;

            // if there is no message for some reason, skip this one
            if (null == manualTask.message) {
              continue;
            }

            Matcher matcher = PLACEHOLDER_REGEX.matcher(manualTask.message);
            while (matcher.find()) {
              tokens.add(matcher.group(1));
            }

            // iterate through all of the matched tokens
            for (String token : tokens) {
              String value = token;
              if (token.equals(PLACEHOLDER_HOST_ALL)) {
                HostsType hostsType = mhr.getMasterAndHosts(
                    taskWrapper.getService(), taskWrapper.getComponent());

                if (null != hostsType) {
                  value = StringUtils.join(hostsType.hosts, ", ");
                }
              } else if (token.equals(PLACEHOLDER_HOST_MASTER)) {
                HostsType hostsType = mhr.getMasterAndHosts(
                    taskWrapper.getService(), taskWrapper.getComponent());

                if (null != hostsType) {
                  value = hostsType.master;
                }
              } else if (token.equals(PLACEHOLDER_VERSION)) {
                value = version;
              } else {
                value = m_configHelper.get().getPlaceholderValueFromDesiredConfigurations(
                    cluster, token);
              }

              // replace the token in the message with the value
              if (null != value) {
                manualTask.message = manualTask.message.replace(token, value);
              }
            }
          }
        }
      }
    }
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
     * Indicate whether retry is allowed for the stages in this group.
     */
    public boolean allowRetry = true;

    /**
     * Indicates whether the stages in this group are skippable on failure.  If a
     * stage is skippable, a failed result can be skipped without failing the entire upgrade.
     */
    public boolean skippable = false;

    /**
     * List of stages for the group
     */
    public List<StageWrapper> items = new ArrayList<StageWrapper>();
  }



  /**
   * Gets a set of Stages resources to aggregate an UpgradeItem with Stage.
   *
   * @param clusterName the cluster name
   * @param requestId the request id containing the stages
   * @param stageIds the list of stages to fetch
   * @return the list of Stage resources
   * @throws UnsupportedPropertyException
   * @throws NoSuchResourceException
   * @throws NoSuchParentResourceException
   * @throws SystemException
   */
  // !!! FIXME this feels very wrong
  public Set<Resource> getStageResources(String clusterName, Long requestId, List<Long> stageIds)
      throws UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException, SystemException {
    ClusterController clusterController = ClusterControllerHelper.getClusterController();

    Request request = PropertyHelper.getReadRequest();


    Predicate p1 = new PredicateBuilder().property(StageResourceProvider.STAGE_CLUSTER_NAME).equals(clusterName).toPredicate();
    Predicate p2 = new PredicateBuilder().property(StageResourceProvider.STAGE_REQUEST_ID).equals(requestId).toPredicate();
    Predicate p3 = null;

    if (1 == stageIds.size()) {
      p3 = new PredicateBuilder().property(StageResourceProvider.STAGE_STAGE_ID).equals(stageIds.get(0)).toPredicate();
    } else if (stageIds.size() > 0) {
      PredicateBuilder pb = new PredicateBuilder();

      int i = 0;
      for (Long stageId : stageIds) {
        if (i++ < stageIds.size()-1) {
          pb = pb.property(StageResourceProvider.STAGE_STAGE_ID).equals(stageId).or();
        } else {
          pb.property(StageResourceProvider.STAGE_STAGE_ID).equals(stageId);
        }
      }

      p3 = pb.toPredicate();
    }

    QueryResponse response = clusterController.getResources(Resource.Type.Stage,
        request, new AndPredicate(p1, p2, p3));

    return response.getResources();
  }

  /**
   * Gets a Request resource to aggreate with an Upgrade
   * @param clusterName the cluster name
   * @param requestId the request id
   * @return the resource for the Request
   * @throws UnsupportedPropertyException
   * @throws NoSuchResourceException
   * @throws NoSuchParentResourceException
   * @throws SystemException
   */
  // !!! FIXME this feels very wrong
  public Resource getRequestResource(String clusterName, Long requestId)
      throws UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException, SystemException {

    ClusterController clusterController = ClusterControllerHelper.getClusterController();

    Request request = PropertyHelper.getReadRequest();

    Predicate predicate = new PredicateBuilder().property(RequestResourceProvider.REQUEST_CLUSTER_NAME_PROPERTY_ID).equals(clusterName).and()
        // !!! RequestResourceProvider is expecting a string, not a Long for the requestId
        .property(RequestResourceProvider.REQUEST_ID_PROPERTY_ID).equals(requestId.toString()).toPredicate();

    QueryResponse response = clusterController.getResources(Resource.Type.Request,
        request, predicate);

    Set<Resource> resources = response.getResources();
    if (1 != resources.size()) {
      throw new SystemException(String.format(
          "Cannot uniquely identify the request resource for %s", requestId));
    }

    return resources.iterator().next();
  }

  /**
   * Special handling for ClusterGrouping that is used for tasks that are
   * to run on a specific targeted HostComponent.
   *
   * @param cluster   the cluster
   * @param grouping  the grouping
   * @param version   the version used to create a {@link MasterHostResolver}
   * @return the holder, or {@code null} if there are no clustergrouping tasks.
   */
  private UpgradeGroupHolder getClusterGroupHolder(Cluster cluster,
      ClusterGrouping grouping, String version) {

    grouping.getBuilder().setHelpers(new MasterHostResolver(cluster, version));
    List<StageWrapper> wrappers = grouping.getBuilder().build();

    if (wrappers.size() > 0) {
      UpgradeGroupHolder holder = new UpgradeGroupHolder();
      holder.name = grouping.name;
      holder.title = grouping.title;
      holder.items = wrappers;

      return holder;
    }


    return null;

  }

}
