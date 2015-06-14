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
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.Grouping;
import org.apache.ambari.server.state.stack.upgrade.ManualTask;
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
   * Enum used to define placeholder text for replacement
   */
  private static enum Placeholder {
    /**
     * No placeholder defined
     */
    OTHER(""),
    /**
     * A placeholder token that represents all of the hosts that a component is
     * deployed on. This can be used for cases where text needs to be rendered
     * with all of the hosts mentioned by their FQDN.
     */
    HOST_ALL("hosts.all"),
    /**
     * A placeholder token that represents a single, active master that a
     * component is deployed on. This can be used for cases where text needs to eb
     * rendered with a single master host FQDN inserted.
     */
    HOST_MASTER("hosts.master"),
    /**
     * The version that the stack is being upgraded or downgraded to, such as
     * {@code 2.2.1.0-1234}.
     */
    VERSION("version"),
    /**
     * The lower case of the {@link Direction} value.
     */
    DIRECTION_TEXT("direction.text"),
    /**
     * The proper case of the {@link Direction} value.
     */
    DIRECTION_TEXT_PROPER("direction.text.proper"),
    /**
     * The past tense of the {@link Direction} value.
     */
    DIRECTION_PAST("direction.past"),
    /**
     * The proper past tense of the {@link Direction} value.
     */
    DIRECTION_PAST_PROPER("direction.past.proper"),
    /**
     * The plural tense of the {@link Direction} value.
     */
    DIRECTION_PLURAL("direction.plural"),
    /**
     * The proper plural tense of the {@link Direction} value.
     */
    DIRECTION_PLURAL_PROPER("direction.plural.proper"),
    /**
     * The verbal noun of the {@link Direction} value.
     */
    DIRECTION_VERB("direction.verb"),
    /**
     * The proper verbal noun of the {@link Direction} value.
     */
    DIRECTION_VERB_PROPER("direction.verb.proper"),
    /**
     * Unhealthy hosts if they are specified.
     */
    UNHEALTHY_HOSTS("hosts.unhealthy");

    private String pattern;
    private Placeholder(String key) {
      pattern = "{{" + key + "}}";
    }

    static Placeholder find(String pattern) {
      for (Placeholder p : values()) {
        if (p.pattern.equals(pattern)) {
          return p;
        }
      }

      return OTHER;
    }
  }

  /**
   * Used to render parameter placeholders in {@link ManualTask}s after the
   * {@link StageWrapperBuilder} has finished building out all of the stages.
   */
  @Inject
  private Provider<ConfigHelper> m_configHelper;

  @Inject
  private Provider<AmbariMetaInfo> m_ambariMetaInfo;


  /**
   * Generates a list of UpgradeGroupHolder items that are used to execute either
   * an upgrade or a downgrade.
   *
   * @param upgradePack
   *          the upgrade pack
   * @param context
   *          the context that wraps key fields required to perform an upgrade
   * @return the list of holders
   */
  public List<UpgradeGroupHolder> createSequence(UpgradePack upgradePack,
      UpgradeContext context) throws AmbariException {

    context.setAmbariMetaInfo(m_ambariMetaInfo.get());
    Cluster cluster = context.getCluster();
    MasterHostResolver mhr = context.getResolver();

    Map<String, Map<String, ProcessingComponent>> allTasks = upgradePack.getTasks();
    List<UpgradeGroupHolder> groups = new ArrayList<UpgradeGroupHolder>();

    for (Grouping group : upgradePack.getGroups(context.getDirection())) {

      UpgradeGroupHolder groupHolder = new UpgradeGroupHolder();
      groupHolder.name = group.name;
      groupHolder.title = group.title;
      groupHolder.skippable = group.skippable;
      groupHolder.allowRetry = group.allowRetry;

      // !!! all downgrades are skippable
      if (context.getDirection().isDowngrade()) {
        groupHolder.skippable = true;
      }

      StageWrapperBuilder builder = group.getBuilder();

      List<UpgradePack.OrderService> services = group.services;

      if (context.getDirection().isDowngrade() && !services.isEmpty()) {
        List<UpgradePack.OrderService> reverse = new ArrayList<UpgradePack.OrderService>(services);
        Collections.reverse(reverse);
        services = reverse;
      }

      // !!! cluster and service checks are empty here
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

          if (!hostsType.unhealthy.isEmpty()) {
            context.addUnhealthy(hostsType.unhealthy);
          }

          Service svc = cluster.getService(service.serviceName);
          ProcessingComponent pc = allTasks.get(service.serviceName).get(component);

          setDisplayNames(context, service.serviceName, component);

          // Special case for NAMENODE
          if (service.serviceName.equalsIgnoreCase("HDFS") && component.equalsIgnoreCase("NAMENODE")) {
            // !!! revisit if needed
            if (!hostsType.hosts.isEmpty() && hostsType.master != null && hostsType.secondary != null) {
              // The order is important, first do the standby, then the active namenode.
              LinkedHashSet<String> order = new LinkedHashSet<String>();

              order.add(hostsType.secondary);
              order.add(hostsType.master);

              // Override the hosts with the ordered collection
              hostsType.hosts = order;
            }

            builder.add(context, hostsType, service.serviceName,
                svc.isClientOnlyService(), pc);

          } else {
            builder.add(context, hostsType, service.serviceName,
                svc.isClientOnlyService(), pc);
          }
        }
      }

      List<StageWrapper> proxies = builder.build(context);

      if (!proxies.isEmpty()) {
        groupHolder.items = proxies;
        postProcess(context, groupHolder);
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
   * Walks through the UpgradeGroupHolder and updates titles and manual tasks,
   * replacing keyword tokens needed for display purposes
   *
   * @param ctx     the upgrade context
   * @param holder  the upgrade holder
   */
  private void postProcess(UpgradeContext ctx, UpgradeGroupHolder holder) {

    holder.title = tokenReplace(ctx, holder.title, null, null);

    for (StageWrapper stageWrapper : holder.items) {
      if (null != stageWrapper.getText()) {
        stageWrapper.setText(tokenReplace(ctx, stageWrapper.getText(),
            null, null));
      }

      for (TaskWrapper taskWrapper : stageWrapper.getTasks()) {
        for (Task task : taskWrapper.getTasks()) {
          if (null != task.summary) {
            task.summary = tokenReplace(ctx, task.summary, null, null);
          }

          if (task.getType() == Type.MANUAL) {
            ManualTask mt = (ManualTask) task;
            if (null != mt.message) {
              mt.message = tokenReplace(ctx, mt.message,
                  taskWrapper.getService(), taskWrapper.getComponent());
            }
          }
        }
      }
    }
  }

  /**
   * @param ctx       the upgrade context
   * @param source    the source string to replace tokens on
   * @param service   the service name if required
   * @param component the component name if required
   * @return the source string with tokens replaced, if any are found
   */
  private String tokenReplace(UpgradeContext ctx, String source, String service, String component) {
    Cluster cluster = ctx.getCluster();
    MasterHostResolver mhr = ctx.getResolver();
    String version = ctx.getVersion();

    String result = source;

    List<String> tokens = new ArrayList<String>(5);
    Matcher matcher = PLACEHOLDER_REGEX.matcher(source);
    while (matcher.find()) {
      tokens.add(matcher.group(1));
    }

    // iterate through all of the matched tokens
    for (String token : tokens) {
      String value = null;

      Placeholder p = Placeholder.find(token);
      switch (p) {
        case HOST_ALL: {
          if (null != service && null != component) {
            HostsType hostsType = mhr.getMasterAndHosts(service, component);

            if (null != hostsType) {
              value = StringUtils.join(hostsType.hosts, ", ");
            }
          }
          break;
        }
        case HOST_MASTER: {
          if (null != service && null != component) {
            HostsType hostsType = mhr.getMasterAndHosts(service, component);

            if (null != hostsType) {
              value = hostsType.master;
            }
          }
          break;
        }
        case VERSION:
          value = version;
          break;
        case DIRECTION_VERB:
        case DIRECTION_VERB_PROPER:
          value = ctx.getDirection().getVerb(p == Placeholder.DIRECTION_VERB_PROPER);
          break;
        case DIRECTION_PAST:
        case DIRECTION_PAST_PROPER:
          value = ctx.getDirection().getPast(p == Placeholder.DIRECTION_PAST_PROPER);
          break;
        case DIRECTION_PLURAL:
        case DIRECTION_PLURAL_PROPER:
          value = ctx.getDirection().getPlural(p == Placeholder.DIRECTION_PLURAL_PROPER);
          break;
        case DIRECTION_TEXT:
        case DIRECTION_TEXT_PROPER:
          value = ctx.getDirection().getText(p == Placeholder.DIRECTION_TEXT_PROPER);
          break;
        case UNHEALTHY_HOSTS:
          value = StringUtils.join(ctx.getUnhealthy().keySet(), ", ");
          break;
        default:
          value = m_configHelper.get().getPlaceholderValueFromDesiredConfigurations(
              cluster, token);
          break;
      }

      // replace the token in the message with the value
      if (null != value) {
        result = result.replace(token, value);
      }
    }

    return result;
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
   * Helper to set service and component display names on the context
   * @param context   the context to update
   * @param service   the service name
   * @param component the component name
   */
  private void setDisplayNames(UpgradeContext context, String service, String component) {
    StackId stackId = context.getCluster().getDesiredStackVersion();
    try {
      ServiceInfo serviceInfo = m_ambariMetaInfo.get().getService(stackId.getStackName(),
          stackId.getStackVersion(), service);
      context.setServiceDisplay(service, serviceInfo.getDisplayName());

      ComponentInfo compInfo = serviceInfo.getComponentByName(component);
      context.setComponentDisplay(service, component, compInfo.getDisplayName());

    } catch (AmbariException e) {
      LOG.debug("Could not get service detail", e);
    }


  }

}
