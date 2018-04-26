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

package org.apache.ambari.server.controller.utilities;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.apache.ambari.server.Role.DATANODE;
import static org.apache.ambari.server.Role.NAMENODE;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.controller.ActionExecutionContext;
import org.apache.ambari.server.controller.AmbariCustomCommandExecutionHelper;
import org.apache.ambari.server.controller.internal.RequestResourceFilter;
import org.apache.ambari.server.controller.internal.RequestStageContainer;
import org.apache.ambari.server.events.HostsRemovedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.utils.StageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Sends REFRESH_NODE command to the NAMENODE after deleting a host that was running a DATANODE
 */
@Singleton
public class NodeRefresher {
  private final static Logger LOG = LoggerFactory.getLogger(NodeRefresher.class);
  private static final String REFRESH_NODE = "REFRESH_NODES";
  private static final String REQUEST_CONTEXT = "Refresh NameNode";
  private final AmbariCustomCommandExecutionHelper executionHelper;
  private final StageFactory stageFactory;
  private final ActionManager actionManager;
  private final RequestFactory requestFactory;

  @Inject
  public NodeRefresher(AmbariCustomCommandExecutionHelper executionHelper, StageFactory stageFactory, ActionManager actionManager, RequestFactory requestFactory) {
    this.executionHelper = executionHelper;
    this.stageFactory = stageFactory;
    this.actionManager = actionManager;
    this.requestFactory = requestFactory;
  }

  public void register(AmbariEventPublisher eventPublisher) {
    eventPublisher.register(this);
  }

  @Subscribe
  public void onHostRemoved(HostsRemovedEvent event) {
    if (event.hasComponent(DATANODE)) {
      for (Cluster cluster : event.getClusters()) {
        try {
          LOG.info("Sending {} command to NAMENODE after host was removed {}", REFRESH_NODE, event);
          refresh(nameNode(cluster), cluster);
        } catch (AmbariException e) {
          LOG.warn("Could not send " + REFRESH_NODE + " to NAMENODE after deleting host: " + event, e);
        }
      }
    }
  }

  private ServiceComponent nameNode(Cluster cluster) throws AmbariException {
    return cluster.getService("HDFS").getServiceComponent(NAMENODE.name());
  }

  protected void refresh(ServiceComponent namenode, Cluster cluster) throws AmbariException {
    RequestStageContainer stageContainer = stageContainer(cluster);
    Stage stage = createNewStage(stageContainer, cluster, REQUEST_CONTEXT);
    ActionExecutionContext exec = new ActionExecutionContext(cluster.getClusterName(), REFRESH_NODE, filters(namenode), emptyMap());
    executionHelper.addExecutionCommandsToStage(exec, stage, emptyMap(), null);
    stageContainer.persist();
  }

  private RequestStageContainer stageContainer(Cluster cluster) throws AmbariException {
    RequestStageContainer requestStageContainer = new RequestStageContainer(
      actionManager.getNextRequestId(),
      null,
      requestFactory,
      actionManager);
    requestStageContainer.setClusterHostInfo(clusterHostInfo(cluster));
    return requestStageContainer;
  }

  private Stage createNewStage(RequestStageContainer stageContainer, Cluster cluster, String requestContext)
    throws AmbariException
  {
    Stage stage = stageFactory.createNew(stageContainer.getId(),
      "/tmp/ambari",
      cluster.getClusterName(),
      cluster.getClusterId(),
      requestContext,
      "{}",
      hostParams(cluster));
    stageContainer.addStages(singletonList(stage));
    stage.setStageId(stageContainer.getLastStageId() <= 0 ? 1 : stageContainer.getLastStageId() +1);
    return stage;
  }

  private String hostParams(Cluster cluster) throws AmbariException {
    Map<String, String> params = executionHelper.createDefaultHostParams(cluster, cluster.getDesiredStackVersion());
    return StageUtils.getGson().toJson(params);
  }

  private List<RequestResourceFilter> filters(ServiceComponent namenode) {
    return singletonList(new RequestResourceFilter(namenode.getServiceName(), namenode.getName(), hosts(namenode)));
  }

  private List<String> hosts(ServiceComponent component) {
    return component.getServiceComponentsHosts().stream()
      .findAny()
      .map(Collections::singletonList)
      .orElse(emptyList());
  }

  private String clusterHostInfo(Cluster cluster) throws AmbariException {
    return StageUtils.getGson().toJson(StageUtils.getClusterHostInfo(cluster));
  }
}
