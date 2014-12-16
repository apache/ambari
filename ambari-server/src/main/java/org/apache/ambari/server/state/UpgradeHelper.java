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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;

import org.apache.ambari.server.AmbariException;
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
import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.apache.ambari.server.state.stack.UpgradePack;
import org.apache.ambari.server.state.stack.UpgradePack.ProcessingComponent;
import org.apache.ambari.server.state.stack.upgrade.ClusterGrouping;
import org.apache.ambari.server.state.stack.upgrade.Grouping;
import org.apache.ambari.server.state.stack.upgrade.StageWrapper;
import org.apache.ambari.server.state.stack.upgrade.StageWrapperBuilder;
import org.apache.ambari.server.state.stack.upgrade.TaskWrapper;
import org.apache.commons.lang.StringUtils;
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
   * @param mhr Master Host Resolver needed to get master and secondary hosts of several components like NAMENODE
   * @param upgradePack the upgrade pack
   * @return the list of holders
   */
  public List<UpgradeGroupHolder> createUpgrade(Cluster cluster, MasterHostResolver mhr, UpgradePack upgradePack) throws AmbariException {
    Map<String, Map<String, ProcessingComponent>> allTasks = upgradePack.getTasks();

    List<UpgradeGroupHolder> groups = new ArrayList<UpgradeGroupHolder>();

    for (Grouping group : upgradePack.getGroups()) {
      if (ClusterGrouping.class.isInstance(group)) {
        UpgradeGroupHolder groupHolder = getClusterGroupHolder(cluster, (ClusterGrouping) group);
        if (null != groupHolder) {
          groups.add(groupHolder);
          continue;
        }
      }

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

          HostsType hostsType = mhr.getMasterAndHosts(service.serviceName, component);
          if (null == hostsType) {
            continue;
          }
          
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
//              throw new AmbariException(MessageFormat.format("Could not find active and standby namenodes using hosts: {0}", StringUtils.join(hostsType.hosts, ", ").toString()));
            }
            
            builder.add(hostsType, service.serviceName, pc);
            
          } else {
            builder.add(hostsType, service.serviceName, pc);
          }
        }
      }

      List<StageWrapper> proxies = builder.build();

      groupHolder.items = proxies;
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
   * Special handling for ClusterGrouping.
   * @param cluster the cluster
   * @param grouping the grouping
   * @return the holder, or {@code null} if there are no clustergrouping tasks.
   */
  private UpgradeGroupHolder getClusterGroupHolder(Cluster cluster, ClusterGrouping grouping) {

    grouping.getBuilder().setHelpers(cluster);
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
