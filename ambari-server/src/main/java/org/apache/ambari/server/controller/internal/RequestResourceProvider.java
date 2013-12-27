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
package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.controller.ExecuteActionRequest;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.state.Clusters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resource provider for request resources.
 */
class RequestResourceProvider extends AbstractControllerResourceProvider {

  // ----- Property ID constants ---------------------------------------------
  // Requests
  protected static final String REQUEST_CLUSTER_NAME_PROPERTY_ID = "Requests/cluster_name";
  protected static final String REQUEST_ID_PROPERTY_ID = "Requests/id";
  protected static final String REQUEST_STATUS_PROPERTY_ID = "Requests/request_status";
  protected static final String REQUEST_CONTEXT_ID = "Requests/request_context";
  protected static final String REQUEST_TASK_CNT_ID = "Requests/task_count";
  protected static final String REQUEST_FAILED_TASK_CNT_ID = "Requests/failed_task_count";
  protected static final String REQUEST_ABORTED_TASK_CNT_ID = "Requests/aborted_task_count";
  protected static final String REQUEST_TIMED_OUT_TASK_CNT_ID = "Requests/timed_out_task_count";
  protected static final String REQUEST_COMPLETED_TASK_CNT_ID = "Requests/completed_task_count";
  protected static final String REQUEST_QUEUED_TASK_CNT_ID = "Requests/queued_task_count";
  protected static final String REQUEST_PROGRESS_PERCENT_ID = "Requests/progress_percent";
  protected static final String COMMAND_ID = "command";
  protected static final String ACTION_ID = "action";
  protected static final String HOSTS_ID = "hosts";
  protected static final String SERVICE_NAME_ID = "service_name";
  protected static final String COMPONENT_NAME_ID = "component_name";
  protected static final String INPUTS_ID = "parameters";
  private static Set<String> pkPropertyIds =
      new HashSet<String>(Arrays.asList(new String[]{
          REQUEST_ID_PROPERTY_ID}));

  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds          the property ids
   * @param keyPropertyIds       the key property ids
   * @param managementController the management controller
   */
  RequestResourceProvider(Set<String> propertyIds,
                          Map<Resource.Type, String> keyPropertyIds,
                          AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  // ----- ResourceProvider ------------------------------------------------

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException, UnsupportedPropertyException, NoSuchParentResourceException, ResourceAlreadyExistsException {
    if (request.getProperties().size() > 1) {
      throw new UnsupportedOperationException("Multiple actions/commands cannot be executed at the same time.");
    }
    final ExecuteActionRequest actionRequest = getActionRequest(request);
    final Map<String, String> requestInfoProperties = request.getRequestInfoProperties();
    return getRequestStatus(createResources(new Command<RequestStatusResponse>() {
      @Override
      public RequestStatusResponse invoke() throws AmbariException {
        return getManagementController().createAction(actionRequest, requestInfoProperties);
      }
    }));
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources = new HashSet<Resource>();

    for (Map<String, Object> properties : getPropertyMaps(predicate)) {
      String clusterName = (String) properties.get(REQUEST_CLUSTER_NAME_PROPERTY_ID);

      Long requestId = null;
      if (properties.get(REQUEST_ID_PROPERTY_ID) != null) {
        requestId = Long.valueOf((String) properties.get(REQUEST_ID_PROPERTY_ID));
      }

      String requestStatus = null;
      if (properties.get(REQUEST_STATUS_PROPERTY_ID) != null) {
        requestStatus = (String) properties.get(REQUEST_STATUS_PROPERTY_ID);
      }
      resources.addAll(getRequestResources(clusterName, requestId, requestStatus, requestedIds));
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Not currently supported.");
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Not currently supported.");
  }

  // ----- AbstractResourceProvider -----------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }


  // ----- utility methods --------------------------------------------------

  // Get request to execute an action/command
  private ExecuteActionRequest getActionRequest(Request request) {
    Map<String, String> requestInfoProperties = request.getRequestInfoProperties();
    Map<String, Object> propertyMap = request.getProperties().iterator().next();

    Boolean isCommand = requestInfoProperties.containsKey(COMMAND_ID);
    String commandName = null;
    String actionName = null;
    if (isCommand) {
      if (requestInfoProperties.containsKey(ACTION_ID)) {
        throw new UnsupportedOperationException("Both command and action cannot be specified.");
      }
      commandName = requestInfoProperties.get(COMMAND_ID);
    } else {
      if (!requestInfoProperties.containsKey(ACTION_ID)) {
        throw new UnsupportedOperationException("Either command or action must be specified.");
      }
      actionName = requestInfoProperties.get(ACTION_ID);
    }

    String hostList = requestInfoProperties.get(HOSTS_ID);
    List<String> hosts = new ArrayList<String>();
    if (hostList != null && !hostList.isEmpty()) {
      for (String hostname : hostList.split(",")) {
        String trimmedName = hostname.trim();
        if (!trimmedName.isEmpty()) {
          hosts.add(hostname.trim());
        }
      }
    }

    String serviceName = requestInfoProperties.get(SERVICE_NAME_ID);
    String componentName = requestInfoProperties.get(COMPONENT_NAME_ID);

    Map<String, String> params = new HashMap<String, String>();
    String keyPrefix = "/" + INPUTS_ID + "/";
    for (String key : requestInfoProperties.keySet()) {
      if (key.startsWith(keyPrefix)) {
        params.put(key.substring(keyPrefix.length()), requestInfoProperties.get(key));
      }
    }

    return new ExecuteActionRequest(
        (String) propertyMap.get(REQUEST_CLUSTER_NAME_PROPERTY_ID),
        commandName,
        actionName,
        serviceName,
        componentName,
        hosts,
        params);
  }

  // Get all of the request resources for the given properties
  private Set<Resource> getRequestResources(String clusterName,
                                            Long requestId,
                                            String requestStatus,
                                            Set<String> requestedPropertyIds)
      throws NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> response = new HashSet<Resource>();
    ActionManager actionManager = getManagementController().getActionManager();

    if (clusterName != null) {
      Clusters clusters = getManagementController().getClusters();
      //validate that cluster exists, throws exception if it doesn't.
      try {
        clusters.getCluster(clusterName);
      } catch (AmbariException e) {
        throw new NoSuchParentResourceException(e.getMessage(), e);
      }
    }

    if (requestId == null) {
      org.apache.ambari.server.actionmanager.RequestStatus status = null;
      if (requestStatus != null) {
        status = org.apache.ambari.server.actionmanager.RequestStatus.valueOf(requestStatus);
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a Get Request Status request"
            + ", requestId=null"
            + ", requestStatus=" + status);
      }
      response.addAll(getRequestResources(clusterName, actionManager,
          actionManager.getRequestsByStatus(status), requestedPropertyIds));
    } else {
      Collection<Resource> responses = getRequestResources(
          clusterName, actionManager, Collections.singletonList(requestId), requestedPropertyIds);

      if (responses.isEmpty()) {
        throw new NoSuchResourceException("Request resource doesn't exist.");
      }
      response.addAll(responses);
    }
    return response;
  }

  // Get all of the request resources for the given set of request ids
  private Collection<Resource> getRequestResources(String clusterName,
                                                   ActionManager actionManager,
                                                   List<Long> requestIds,
                                                   Set<String> requestedPropertyIds) {

    List<HostRoleCommand> hostRoleCommands = actionManager.getAllTasksByRequestIds(requestIds);
    Map<Long, String> requestContexts = actionManager.getRequestContext(requestIds);
    Map<Long, Resource> resourceMap = new HashMap<Long, Resource>();

    // group by request id
    Map<Long, Set<HostRoleCommand>> commandMap = new HashMap<Long, Set<HostRoleCommand>>();

    for (HostRoleCommand hostRoleCommand : hostRoleCommands) {
      Long requestId = hostRoleCommand.getRequestId();
      Set<HostRoleCommand> commands = commandMap.get(requestId);

      if (commands == null) {
        commands = new HashSet<HostRoleCommand>();
        commandMap.put(requestId, commands);
      }
      commands.add(hostRoleCommand);
    }

    for (Map.Entry<Long, Set<HostRoleCommand>> entry : commandMap.entrySet()) {
      Long requestId = entry.getKey();
      Set<HostRoleCommand> commands = entry.getValue();
      String context = requestContexts.get(requestId);

      resourceMap.put(requestId,
          getRequestResource(clusterName, requestId, context, commands, requestedPropertyIds));
    }
    return resourceMap.values();
  }

  // Get a request resource from the given set of host role commands.
  private Resource getRequestResource(String clusterName,
                                      Long requestId,
                                      String context,
                                      Set<HostRoleCommand> commands,
                                      Set<String> requestedPropertyIds) {
    Resource resource = new ResourceImpl(Resource.Type.Request);

    setResourceProperty(resource, REQUEST_CLUSTER_NAME_PROPERTY_ID, clusterName, requestedPropertyIds);
    setResourceProperty(resource, REQUEST_ID_PROPERTY_ID, requestId, requestedPropertyIds);
    setResourceProperty(resource, REQUEST_CONTEXT_ID, context, requestedPropertyIds);

    int taskCount = commands.size();
    int completedTaskCount = 0;
    int queuedTaskCount = 0;
    int pendingTaskCount = 0;
    int failedTaskCount = 0;
    int abortedTaskCount = 0;
    int timedOutTaskCount = 0;

    for (HostRoleCommand hostRoleCommand : commands) {
      HostRoleStatus status = hostRoleCommand.getStatus();
      if (status.isCompletedState()) {
        completedTaskCount++;

        switch (status) {
          case ABORTED:
            abortedTaskCount++;
            break;
          case FAILED:
            failedTaskCount++;
            break;
          case TIMEDOUT:
            timedOutTaskCount++;
            break;
        }
      } else if (status.equals(HostRoleStatus.QUEUED)) {
        queuedTaskCount++;
      } else if (status.equals(HostRoleStatus.PENDING)) {
        pendingTaskCount++;
      }
    }

    int inProgressTaskCount = taskCount - completedTaskCount - queuedTaskCount - pendingTaskCount;

    // determine request status
    HostRoleStatus requestStatus = failedTaskCount > 0 ? HostRoleStatus.FAILED :
        abortedTaskCount > 0 ? HostRoleStatus.ABORTED :
            timedOutTaskCount > 0 ? HostRoleStatus.TIMEDOUT :
                inProgressTaskCount > 0 ? HostRoleStatus.IN_PROGRESS :
                    completedTaskCount == taskCount ? HostRoleStatus.COMPLETED :
                        HostRoleStatus.PENDING;
    double progressPercent =
        ((queuedTaskCount * 0.09 + inProgressTaskCount * 0.35 + completedTaskCount) / (double) taskCount) * 100.0;

    setResourceProperty(resource, REQUEST_STATUS_PROPERTY_ID, requestStatus.toString(), requestedPropertyIds);
    setResourceProperty(resource, REQUEST_TASK_CNT_ID, taskCount, requestedPropertyIds);
    setResourceProperty(resource, REQUEST_FAILED_TASK_CNT_ID, failedTaskCount, requestedPropertyIds);
    setResourceProperty(resource, REQUEST_ABORTED_TASK_CNT_ID, abortedTaskCount, requestedPropertyIds);
    setResourceProperty(resource, REQUEST_TIMED_OUT_TASK_CNT_ID, timedOutTaskCount, requestedPropertyIds);
    setResourceProperty(resource, REQUEST_QUEUED_TASK_CNT_ID, queuedTaskCount, requestedPropertyIds);
    setResourceProperty(resource, REQUEST_COMPLETED_TASK_CNT_ID, completedTaskCount, requestedPropertyIds);
    setResourceProperty(resource, REQUEST_PROGRESS_PERCENT_ID, progressPercent, requestedPropertyIds);

    return resource;
  }
}
