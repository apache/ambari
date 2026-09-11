/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.hooks.users;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.RequestStageContainer;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.hooks.AmbariEventFactory;
import org.apache.ambari.server.hooks.HookContext;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.serveraction.users.PostUserCreationHookServerAction;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostServerActionEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;

/**
 * Service in charge for handling user initialization related logic.
 * It's expected that this implementation encapsulates all the logic around the user initialization hook:
 * 1. validates the context  (all the input is available)
 * 2. checks if prerequisites are satisfied for the hook execution
 * 3. triggers the hook execution flow
 * 4. executes the flow (on a separate thread)
 */
@Singleton
public class UserHookService implements HookService {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserHookService.class);

  private static final String POST_USER_CREATION_REQUEST_CONTEXT = "Post user creation hook for [ %s ] users";
  private static final String INPUT_FILE_PREFIX = "user_hook_input_%s.csv";

  // constants for getting security related properties
  private static final String HADOOP_ENV = "hadoop-env";
  private static final String HDFS_USER_KEYTAB = "hdfs_user_keytab";
  private static final String HDFS_PRINCIPAL_NAME = "hdfs_principal_name";

  @Inject
  private AmbariEventFactory eventFactory;

  @Inject
  private AmbariEventPublisher ambariEventPublisher;

  @Inject
  private ActionManager actionManager;

  @Inject
  private RequestFactory requestFactory;

  @Inject
  private StageFactory stageFactory;

  @Inject
  private Configuration configuration;

  @Inject
  private Clusters clusters;

  @Inject
  private ObjectMapper objectMapper;

  // executed by the IoC framework after creating the object (guice)
  @Inject
  private void register() {
    ambariEventPublisher.register(this);
  }

  @Override
  public boolean execute(HookContext hookContext) {
    LOGGER.info("Executing user hook for {}. ", hookContext);

    PostUserCreationHookContext hookCtx = validateHookInput(hookContext);

    if (!configuration.isUserHookEnabled()) {
      LOGGER.warn("Post user creation hook disabled.");
      LOGGER.warn("Prerequisites for user hook are not satisfied. Hook not triggered");
      return false;
    }

    if (hookCtx.getUserGroups().isEmpty()) {
      LOGGER.info("No users found for executing user hook for");
      return false;
    }

    Cluster targetCluster = resolveTargetCluster();
    if (targetCluster == null) {
      LOGGER.warn("Prerequisites for user hook are not satisfied. Hook not triggered");
      return false;
    }

    UserCreatedEvent userCreatedEvent = (UserCreatedEvent) eventFactory.newUserCreatedEvent(
        hookCtx, targetCluster.getClusterId());

    LOGGER.info("Triggering user hook for user: {}", hookContext);
    ambariEventPublisher.publish(userCreatedEvent);

    return true;
  }

  @Subscribe
  public void onUserCreatedEvent(UserCreatedEvent event) throws AmbariException {
    LOGGER.info("Preparing hook execution for event: {}", event);

    try {
      Cluster cluster;
      try {
        cluster = clusters.getClusterById(event.getClusterId());
      } catch (AmbariException e) {
        LOGGER.warn("Post user creation hook target cluster ID {} is no longer available. Hook not triggered.",
            event.getClusterId());
        return;
      }
      ClusterData clsData = getClusterData(cluster);
      RequestStageContainer requestStageContainer = new RequestStageContainer(actionManager.getNextRequestId(), null, requestFactory, actionManager);

      PostUserCreationHookContext ctx = (PostUserCreationHookContext) event.getContext();

      String stageContextText = String.format(POST_USER_CREATION_REQUEST_CONTEXT, ctx.getUserGroups().size());

      Stage stage = stageFactory.createNew(requestStageContainer.getId(), configuration.getServerTempDir() + File.pathSeparatorChar + requestStageContainer.getId(), clsData.getClusterName(),
          clsData.getClusterId(), stageContextText, "{}", "{}");
      stage.setStageId(requestStageContainer.getLastStageId());

      ServiceComponentHostServerActionEvent serverActionEvent = new ServiceComponentHostServerActionEvent("ambari-server-host", System.currentTimeMillis());
      Map<String, String> commandParams = prepareCommandParams(ctx, clsData);

      stage.addServerActionCommand(PostUserCreationHookServerAction.class.getName(), "ambari", Role.AMBARI_SERVER_ACTION,
          RoleCommand.EXECUTE, clsData.getClusterName(), serverActionEvent, commandParams, stageContextText, null, null, false, false);

      requestStageContainer.addStages(Collections.singletonList(stage));
      requestStageContainer.persist();

    } catch (IOException e) {
      LOGGER.error("Failed to assemble stage for server action. Event: {}", event);
      throw new AmbariException("Failed to assemble stage for server action", e);
    }

  }

  private Map<String, String> prepareCommandParams(PostUserCreationHookContext context, ClusterData clusterData) throws IOException {

    Map<String, String> commandParams = new HashMap<>();

    commandParams.put(UserHookParams.SCRIPT.param(), configuration.getProperty(Configuration.POST_USER_CREATION_HOOK));

    commandParams.put(UserHookParams.CLUSTER_ID.param(), String.valueOf(clusterData.getClusterId()));
    commandParams.put(UserHookParams.CLUSTER_NAME.param(), clusterData.getClusterName());
    commandParams.put(UserHookParams.CLUSTER_SECURITY_TYPE.param(), clusterData.getSecurityType());

    commandParams.put(UserHookParams.CMD_HDFS_KEYTAB.param(), clusterData.getKeytab());
    commandParams.put(UserHookParams.CMD_HDFS_PRINCIPAL.param(), clusterData.getPrincipal());
    commandParams.put(UserHookParams.CMD_HDFS_USER.param(), clusterData.getHdfsUser());
    commandParams.put(UserHookParams.CMD_INPUT_FILE.param(), generateInputFileName());


    commandParams.put(UserHookParams.PAYLOAD.param(), objectMapper.writeValueAsString(context.getUserGroups()));

    return commandParams;
  }

  private String generateInputFileName() {
    String inputFileName = String.format(INPUT_FILE_PREFIX, Calendar.getInstance().getTimeInMillis());
    LOGGER.debug("Command input file name: {}", inputFileName);

    return configuration.getServerTempDir() + File.separator + inputFileName;
  }

  private Cluster resolveTargetCluster() {
    Map<String, Cluster> availableClusters = clusters.getClusters();
    if (availableClusters.isEmpty()) {
      LOGGER.warn("There's no cluster found. Post user creation hook won't be executed.");
      return null;
    }

    String configuredClusterName = StringUtils.trimToNull(
        configuration.getProperty(Configuration.POST_USER_CREATION_HOOK_CLUSTER_NAME));
    if (configuredClusterName != null) {
      Cluster configuredCluster = availableClusters.get(configuredClusterName);
      if (configuredCluster == null) {
        LOGGER.warn("Configured post user creation hook cluster '{}' does not exist. Hook not triggered.",
            configuredClusterName);
      }
      return configuredCluster;
    }

    if (availableClusters.size() != 1) {
      LOGGER.warn("Post user creation hook cluster must be configured when {} clusters exist. Hook not triggered.",
          availableClusters.size());
      return null;
    }
    return availableClusters.values().iterator().next();
  }

  private PostUserCreationHookContext validateHookInput(HookContext hookContext) {
    // perform any other validation steps, such as existence of fields etc...
    return (PostUserCreationHookContext) hookContext;
  }

  private ClusterData getClusterData(Cluster cluster) {
    //default value for unsecure clusters
    String keyTab = "NA";
    String principal = "NA";

    switch (cluster.getSecurityType()) {
      case KERBEROS:
        // get the principal
        Map<String, String> hadoopEnv = cluster.getDesiredConfigByType(HADOOP_ENV).getProperties();
        keyTab = hadoopEnv.get(HDFS_USER_KEYTAB);
        principal = hadoopEnv.get(HDFS_PRINCIPAL_NAME);
        break;
      case NONE:
        // break; let the flow enter the default case
      default:
        LOGGER.debug("The cluster security is not set. Security type: {}", cluster.getSecurityType());
        break;
    }


    return new ClusterData(cluster.getClusterName(), cluster.getClusterId(), cluster.getSecurityType().name(), principal, keyTab, getHdfsUser(cluster));
  }

  private String getHdfsUser(Cluster cluster) {
    String hdfsUser = cluster.getDesiredConfigByType("hadoop-env").getProperties().get("hdfs_user");
    return hdfsUser;
  }


  /**
   * Local representation of cluster data.
   */
  private static final class ClusterData {
    private String clusterName;
    private Long clusterId;
    private String securityType;
    private String principal;
    private String keytab;

    private String hdfsUser;

    public ClusterData(String clusterName, Long clusterId, String securityType, String principal, String keytab, String hdfsUser) {
      this.clusterName = clusterName;
      this.clusterId = clusterId;
      this.securityType = securityType;
      this.principal = principal;
      this.keytab = keytab;
      this.hdfsUser = hdfsUser;
    }

    public String getClusterName() {
      return clusterName;
    }

    public Long getClusterId() {
      return clusterId;
    }

    public String getSecurityType() {
      return securityType;
    }

    public String getPrincipal() {
      return principal;
    }

    public String getKeytab() {
      return keytab;
    }

    public String getHdfsUser() {
      return hdfsUser;
    }
  }
}
