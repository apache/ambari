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
package org.apache.ambari.server.controller.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.orm.entities.HostRoleCommandEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.topology.LogicalRequest;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.ambari.server.utils.StageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

/**
 * Resource provider for task resources.
 */
@StaticallyInject
public class TaskResourceProvider extends AbstractControllerResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(TaskResourceProvider.class);
  private static final Set<RoleAuthorization> VIEW_AUTHORIZATIONS = EnumSet.of(
      RoleAuthorization.CLUSTER_VIEW_STATUS_INFO,
      RoleAuthorization.HOST_VIEW_STATUS_INFO,
      RoleAuthorization.SERVICE_VIEW_STATUS_INFO);

  // ----- Property ID constants ---------------------------------------------

  // Tasks
  public static final String TASK_CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("Tasks", "cluster_name");
  public static final String TASK_REQUEST_ID_PROPERTY_ID   = PropertyHelper.getPropertyId("Tasks", "request_id");
  public static final String TASK_ID_PROPERTY_ID           = PropertyHelper.getPropertyId("Tasks", "id");
  public static final String TASK_STAGE_ID_PROPERTY_ID     = PropertyHelper.getPropertyId("Tasks", "stage_id");
  public static final String TASK_HOST_NAME_PROPERTY_ID    = PropertyHelper.getPropertyId("Tasks", "host_name");
  public static final String TASK_ROLE_PROPERTY_ID         = PropertyHelper.getPropertyId("Tasks", "role");
  public static final String TASK_COMMAND_PROPERTY_ID      = PropertyHelper.getPropertyId("Tasks", "command");
  public static final String TASK_STATUS_PROPERTY_ID       = PropertyHelper.getPropertyId("Tasks", "status");
  public static final String TASK_EXIT_CODE_PROPERTY_ID    = PropertyHelper.getPropertyId("Tasks", "exit_code");
  public static final String TASK_STDERR_PROPERTY_ID       = PropertyHelper.getPropertyId("Tasks", "stderr");
  public static final String TASK_STOUT_PROPERTY_ID        = PropertyHelper.getPropertyId("Tasks", "stdout");
  public static final String TASK_OUTPUTLOG_PROPERTY_ID    = PropertyHelper.getPropertyId("Tasks", "output_log");
  public static final String TASK_ERRORLOG_PROPERTY_ID     = PropertyHelper.getPropertyId("Tasks", "error_log");
  public static final String TASK_STRUCT_OUT_PROPERTY_ID   = PropertyHelper.getPropertyId("Tasks", "structured_out");
  public static final String TASK_START_TIME_PROPERTY_ID   = PropertyHelper.getPropertyId("Tasks", "start_time");
  public static final String TASK_END_TIME_PROPERTY_ID     = PropertyHelper.getPropertyId("Tasks", "end_time");
  public static final String TASK_ATTEMPT_CNT_PROPERTY_ID  = PropertyHelper.getPropertyId("Tasks", "attempt_cnt");
  public static final String TASK_COMMAND_DET_PROPERTY_ID  = PropertyHelper.getPropertyId("Tasks", "command_detail");
  public static final String TASK_CUST_CMD_NAME_PROPERTY_ID  = PropertyHelper.getPropertyId("Tasks", "custom_command_name");
  public static final String TASK_COMMAND_OPS_DISPLAY_NAME  = PropertyHelper.getPropertyId("Tasks", "ops_display_name");


  /**
   * The property ids for a task resource.
   */
  static final Set<String> PROPERTY_IDS = new HashSet<>();

  // These are static so that they can be referenced by other classes such as UpgradeSummaryResourceProvider.java
  static {
    // properties
    PROPERTY_IDS.add(TASK_CLUSTER_NAME_PROPERTY_ID);
    PROPERTY_IDS.add(TASK_REQUEST_ID_PROPERTY_ID);
    PROPERTY_IDS.add(TASK_ID_PROPERTY_ID);
    PROPERTY_IDS.add(TASK_STAGE_ID_PROPERTY_ID);
    PROPERTY_IDS.add(TASK_HOST_NAME_PROPERTY_ID);
    PROPERTY_IDS.add(TASK_ROLE_PROPERTY_ID);
    PROPERTY_IDS.add(TASK_COMMAND_PROPERTY_ID);
    PROPERTY_IDS.add(TASK_STATUS_PROPERTY_ID);
    PROPERTY_IDS.add(TASK_EXIT_CODE_PROPERTY_ID);
    PROPERTY_IDS.add(TASK_STDERR_PROPERTY_ID);
    PROPERTY_IDS.add(TASK_STOUT_PROPERTY_ID);
    PROPERTY_IDS.add(TASK_OUTPUTLOG_PROPERTY_ID);
    PROPERTY_IDS.add(TASK_ERRORLOG_PROPERTY_ID);
    PROPERTY_IDS.add(TASK_STRUCT_OUT_PROPERTY_ID);
    PROPERTY_IDS.add(TASK_START_TIME_PROPERTY_ID);
    PROPERTY_IDS.add(TASK_END_TIME_PROPERTY_ID);
    PROPERTY_IDS.add(TASK_ATTEMPT_CNT_PROPERTY_ID);
    PROPERTY_IDS.add(TASK_COMMAND_DET_PROPERTY_ID);
    PROPERTY_IDS.add(TASK_CUST_CMD_NAME_PROPERTY_ID);
    PROPERTY_IDS.add(TASK_COMMAND_OPS_DISPLAY_NAME);
  }

  /**
   * The key property ids for a task resource.
   */
  private static final Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.Cluster, TASK_CLUSTER_NAME_PROPERTY_ID)
      .put(Resource.Type.Request, TASK_REQUEST_ID_PROPERTY_ID)
      .put(Resource.Type.Upgrade, TASK_REQUEST_ID_PROPERTY_ID)
      .put(Resource.Type.Stage, TASK_STAGE_ID_PROPERTY_ID)
      .put(Resource.Type.UpgradeItem, TASK_STAGE_ID_PROPERTY_ID)
      .put(Resource.Type.Task, TASK_ID_PROPERTY_ID)
      .build();

  /**
   * Used for querying tasks.
   */
  @Inject
  static HostRoleCommandDAO s_dao;

  /**
   * Used for constructing instances of {@link HostRoleCommand} from {@link HostRoleCommandEntity}.
   */
  @Inject
  private static HostRoleCommandFactory s_hostRoleCommandFactory;

  @Inject
  static TopologyManager s_topologyManager;

  /**
   * Thread-safe Jackson JSON mapper.
   */
  private static final ObjectMapper mapper = new ObjectMapper();

  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param managementController  the management controller
   */
  TaskResourceProvider(AmbariManagementController managementController) {
    super(Resource.Type.Task, PROPERTY_IDS, keyPropertyIds, managementController);
  }

  // ----- ResourceProvider ------------------------------------------------

  @Override
  public RequestStatus createResources(Request request) {
    throw new UnsupportedOperationException("Not currently supported.");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> results = new LinkedHashSet<>();
    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    TaskQuery query = getTaskQuery(predicate);
    List<HostRoleCommandEntity> entities = s_dao.findAll(request, predicate);
    Collection<ClusterTask> commands = new ArrayList<>(100);
    Set<TaskIdentity> taskIdentities = new HashSet<>();
    Set<Long> persistedRequestIds = new HashSet<>();

    for (HostRoleCommandEntity entity : entities) {
      if (entity.getStage() == null) {
        throw new AuthorizationException("The task has no authoritative cluster owner");
      }
      validateTaskRelationship(entity);
      persistedRequestIds.add(entity.getRequestId());
      if (query.directLookup && !query.matchesIdentifiers(entity)) {
        throw new AuthorizationException("The task does not belong to the requested parent");
      }
      TaskOwner owner = authorizeOwner(entity.getStage().getClusterId(), query.directLookup);
      if (owner.authorized) {
        if (query.directLookup && !query.matchesCluster(owner.cluster)) {
          throw new AuthorizationException("The task does not belong to the requested cluster");
        }
        HostRoleCommand command = s_hostRoleCommandFactory.createExisting(entity);
        commands.add(new ClusterTask(command, owner.cluster));
        taskIdentities.add(new TaskIdentity(command.getRequestId(), command.getTaskId()));
      }
    }

    Set<Long> topologyRequestIds = new HashSet<>(query.requestIds);
    topologyRequestIds.removeAll(persistedRequestIds);
    Collection<LogicalRequest> logicalRequests;
    if (query.requestIds.isEmpty()) {
      logicalRequests = s_topologyManager.getRequests(Collections.emptyList());
    } else if (topologyRequestIds.isEmpty()) {
      logicalRequests = Collections.emptyList();
    } else {
      logicalRequests = s_topologyManager.getRequests(topologyRequestIds);
    }
    for (LogicalRequest logicalRequest : logicalRequests) {
      for (HostRoleCommand command : logicalRequest.getCommands()) {
        if (command.getRequestId() != logicalRequest.getRequestId()) {
          throw new AuthorizationException("The topology task parent relationship is inconsistent");
        }
        if (query.directLookup && !query.matchesIdentifiers(command)) {
          continue;
        }
        TaskIdentity identity = new TaskIdentity(command.getRequestId(), command.getTaskId());
        if (taskIdentities.contains(identity)) {
          continue;
        }
        TaskOwner owner = authorizeOwner(logicalRequest.getClusterId(), query.directLookup);
        if (owner.authorized) {
          if (query.directLookup && !query.matchesCluster(owner.cluster)) {
            throw new AuthorizationException("The task does not belong to the requested cluster");
          }
          commands.add(new ClusterTask(command, owner.cluster));
          taskIdentities.add(identity);
        }
      }
    }

    LOG.debug("Retrieved {} commands for request {}", commands.size(), request);

    // convert each entity into a response
    for (ClusterTask clusterTask : commands) {
      Resource resource = toResource(clusterTask, requestedIds);
      if (predicate == null || predicate.evaluate(resource)) {
        results.add(resource);
      }
    }

    return results;
  }

  private TaskOwner authorizeOwner(Long actualClusterId, boolean failIfUnauthorized)
      throws AuthorizationException {
    if (actualClusterId == null || actualClusterId == -1L) {
      if (!AuthorizationHelper.isAuthorized(
          ResourceType.AMBARI, null, RoleAuthorization.AMBARI_VIEW_STATUS_INFO)) {
        if (failIfUnauthorized) {
          throw new AuthorizationException("The authenticated user is not authorized to access the task");
        }
        return TaskOwner.excluded();
      }
      return TaskOwner.authorized(null);
    }
    try {
      Cluster actualCluster = getManagementController().getClusters().getClusterById(actualClusterId);
      if (AuthorizationHelper.isAuthorized(
          ResourceType.CLUSTER, actualCluster.getResourceId(), VIEW_AUTHORIZATIONS)) {
        return TaskOwner.authorized(actualCluster);
      }
    } catch (Exception e) {
      throw new AuthorizationException("The task cluster is unavailable");
    }
    if (failIfUnauthorized) {
      throw new AuthorizationException("The authenticated user is not authorized to access the task");
    }
    return TaskOwner.excluded();
  }

  private TaskQuery getTaskQuery(Predicate predicate) {
    Set<Long> requestIds = new HashSet<>();
    Set<Long> stageIds = new HashSet<>();
    Set<Long> taskIds = new HashSet<>();
    Set<Map<String, Object>> propertyMaps = Collections.emptySet();
    String clusterName = null;
    if (predicate != null) {
      propertyMaps = getPropertyMaps(predicate);
      for (Map<String, Object> propertyMap : propertyMaps) {
        addLong(requestIds, propertyMap.get(TASK_REQUEST_ID_PROPERTY_ID), "request ID");
        addLong(stageIds, propertyMap.get(TASK_STAGE_ID_PROPERTY_ID), "stage ID");
        addLong(taskIds, propertyMap.get(TASK_ID_PROPERTY_ID), "task ID");
      }
    }
    boolean directLookup = propertyMaps.size() == 1 && !taskIds.isEmpty();
    if (directLookup) {
      Object value = propertyMaps.iterator().next().get(TASK_CLUSTER_NAME_PROPERTY_ID);
      clusterName = value == null ? null : String.valueOf(value);
    }
    return new TaskQuery(clusterName, requestIds, stageIds, taskIds, directLookup);
  }

  private void addLong(Set<Long> values, Object value, String label) {
    if (value != null) {
      try {
        values.add(Long.valueOf(String.valueOf(value)));
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid task " + label, e);
      }
    }
  }

  private void validateTaskRelationship(HostRoleCommandEntity entity)
      throws AuthorizationException {
    if (!Objects.equals(entity.getStage().getRequestId(), entity.getRequestId())
        || !Objects.equals(entity.getStage().getStageId(), entity.getStageId())) {
      throw new AuthorizationException("The task parent relationship is inconsistent");
    }
  }

  private Resource toResource(ClusterTask clusterTask, Set<String> requestedIds) {
    HostRoleCommand hostRoleCommand = clusterTask.command;
    Resource resource = new ResourceImpl(Resource.Type.Task);

    if (clusterTask.cluster != null) {
      setResourceProperty(resource, TASK_CLUSTER_NAME_PROPERTY_ID,
          clusterTask.cluster.getClusterName(), requestedIds);
    }

    setResourceProperty(resource, TASK_REQUEST_ID_PROPERTY_ID, hostRoleCommand.getRequestId(), requestedIds);
    setResourceProperty(resource, TASK_ID_PROPERTY_ID, hostRoleCommand.getTaskId(), requestedIds);
    setResourceProperty(resource, TASK_STAGE_ID_PROPERTY_ID, hostRoleCommand.getStageId(), requestedIds);
    setResourceProperty(resource, TASK_HOST_NAME_PROPERTY_ID, ensureHostname(hostRoleCommand.getHostName()), requestedIds);
    setResourceProperty(resource, TASK_ROLE_PROPERTY_ID, hostRoleCommand.getRole().toString(), requestedIds);
    setResourceProperty(resource, TASK_COMMAND_PROPERTY_ID, hostRoleCommand.getRoleCommand(), requestedIds);
    setResourceProperty(resource, TASK_STATUS_PROPERTY_ID, hostRoleCommand.getStatus(), requestedIds);
    setResourceProperty(resource, TASK_EXIT_CODE_PROPERTY_ID, hostRoleCommand.getExitCode(), requestedIds);
    setResourceProperty(resource, TASK_STDERR_PROPERTY_ID, hostRoleCommand.getStderr(), requestedIds);
    setResourceProperty(resource, TASK_STOUT_PROPERTY_ID, hostRoleCommand.getStdout(), requestedIds);
    setResourceProperty(resource, TASK_OUTPUTLOG_PROPERTY_ID, hostRoleCommand.getOutputLog(), requestedIds);
    setResourceProperty(resource, TASK_ERRORLOG_PROPERTY_ID, hostRoleCommand.getErrorLog(), requestedIds);
    setResourceProperty(resource, TASK_STRUCT_OUT_PROPERTY_ID,
        parseStructuredOutput(hostRoleCommand.getStructuredOut()), requestedIds);
    setResourceProperty(resource, TASK_START_TIME_PROPERTY_ID, hostRoleCommand.getStartTime(), requestedIds);
    setResourceProperty(resource, TASK_END_TIME_PROPERTY_ID, hostRoleCommand.getEndTime(), requestedIds);
    setResourceProperty(resource, TASK_ATTEMPT_CNT_PROPERTY_ID, hostRoleCommand.getAttemptCount(), requestedIds);

    if (hostRoleCommand.getCustomCommandName() != null) {
      setResourceProperty(resource, TASK_CUST_CMD_NAME_PROPERTY_ID,
          hostRoleCommand.getCustomCommandName(), requestedIds);
    }

    if (hostRoleCommand.getCommandDetail() == null) {
      setResourceProperty(resource, TASK_COMMAND_DET_PROPERTY_ID,
          String.format("%s %s", hostRoleCommand.getRole().toString(),
              hostRoleCommand.getRoleCommand()), requestedIds);
    } else {
      setResourceProperty(resource, TASK_COMMAND_DET_PROPERTY_ID,
          hostRoleCommand.getCommandDetail(), requestedIds);
    }

    setResourceProperty(resource, TASK_COMMAND_OPS_DISPLAY_NAME,
        hostRoleCommand.getOpsDisplayName(), requestedIds);
    return resource;
  }

  /**
   * Converts the specified JSON string into a {@link Map}. For now, use Jackson
   * instead of gson since none of the integers will convert properly without a
   * well-defined first-class object to map to.
   *
   * @param structuredOutput
   *          the JSON string to convert.
   * @return the converted JSON as key-value pairs, or {@code null} if an
   *         exception was encountered or if the JSON string was empty.
   */
  Map<?, ?> parseStructuredOutput(String structuredOutput) {
    if (null == structuredOutput || structuredOutput.isEmpty()) {
      return null;
    }

    Map<?, ?> result = null;

    try {
      result = mapper.readValue(structuredOutput, Map.class);
    } catch (Exception exception) {
      LOG.warn("Unable to parse task structured output ({})",
          exception.getClass().getSimpleName());
    }
    return result;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Not currently supported.");
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Not currently supported.");
  }

  // ----- utility methods -------------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }

  /**
   * Ensures that a hostname is returned. If null (indicating the host is the Ambari server), the
   * hostname of the Ambari server is returned.
   *
   * @param hostName a hostname
   * @return the specified hostname or the hostname of the Ambari Server
   */
  protected String ensureHostname(String hostName) {
    return (hostName == null) ? StageUtils.getHostName() : hostName;
  }

  private static final class ClusterTask {
    private final HostRoleCommand command;
    private final Cluster cluster;

    private ClusterTask(HostRoleCommand command, Cluster cluster) {
      this.command = command;
      this.cluster = cluster;
    }
  }

  private static final class TaskOwner {
    private final boolean authorized;
    private final Cluster cluster;

    private TaskOwner(boolean authorized, Cluster cluster) {
      this.authorized = authorized;
      this.cluster = cluster;
    }

    private static TaskOwner authorized(Cluster cluster) {
      return new TaskOwner(true, cluster);
    }

    private static TaskOwner excluded() {
      return new TaskOwner(false, null);
    }
  }

  private static final class TaskQuery {
    private final String clusterName;
    private final Set<Long> requestIds;
    private final Set<Long> stageIds;
    private final Set<Long> taskIds;
    private final boolean directLookup;

    private TaskQuery(String clusterName, Set<Long> requestIds, Set<Long> stageIds,
        Set<Long> taskIds, boolean directLookup) {
      this.clusterName = clusterName;
      this.requestIds = requestIds;
      this.stageIds = stageIds;
      this.taskIds = taskIds;
      this.directLookup = directLookup;
    }

    private boolean matchesIdentifiers(HostRoleCommandEntity entity) {
      return (requestIds.isEmpty() || requestIds.contains(entity.getRequestId()))
          && (stageIds.isEmpty() || stageIds.contains(entity.getStageId()))
          && (taskIds.isEmpty() || taskIds.contains(entity.getTaskId()));
    }

    private boolean matchesIdentifiers(HostRoleCommand command) {
      return (requestIds.isEmpty() || requestIds.contains(command.getRequestId()))
          && (stageIds.isEmpty() || stageIds.contains(command.getStageId()))
          && (taskIds.isEmpty() || taskIds.contains(command.getTaskId()));
    }

    private boolean matchesCluster(Cluster cluster) {
      return clusterName == null || cluster != null
          && clusterName.equals(cluster.getClusterName());
    }
  }

  private static final class TaskIdentity {
    private final long requestId;
    private final long taskId;

    private TaskIdentity(long requestId, long taskId) {
      this.requestId = requestId;
      this.taskId = taskId;
    }

    @Override
    public boolean equals(Object object) {
      if (this == object) {
        return true;
      }
      if (!(object instanceof TaskIdentity)) {
        return false;
      }
      TaskIdentity that = (TaskIdentity) object;
      return requestId == that.requestId && taskId == that.taskId;
    }

    @Override
    public int hashCode() {
      return Objects.hash(requestId, taskId);
    }
  }

}
