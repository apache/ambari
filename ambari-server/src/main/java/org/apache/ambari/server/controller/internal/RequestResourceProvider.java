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
import org.apache.ambari.server.api.services.BaseRequest;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ExecuteActionRequest;
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
public class RequestResourceProvider extends AbstractControllerResourceProvider {

  // ----- Property ID constants ---------------------------------------------
  // Requests
  protected static final String REQUEST_CLUSTER_NAME_PROPERTY_ID = "Requests/cluster_name";
  protected static final String REQUEST_ID_PROPERTY_ID = "Requests/id";
  protected static final String REQUEST_STATUS_PROPERTY_ID = "Requests/request_status";
  protected static final String REQUEST_CONTEXT_ID = "Requests/request_context";
  public static final String REQUEST_SOURCE_SCHEDULE = "Requests/request_schedule";
  public static final String REQUEST_SOURCE_SCHEDULE_ID = "Requests/request_schedule/schedule_id";
  public static final String REQUEST_SOURCE_SCHEDULE_HREF = "Requests/request_schedule/href";
  protected static final String REQUEST_TYPE_ID = "Requests/type";
  protected static final String REQUEST_INPUTS_ID = "Requests/inputs";
  protected static final String REQUEST_RESOURCE_FILTER_ID = "Requests/resource_filters";
  protected static final String REQUEST_OPERATION_LEVEL_ID = "Requests/operation_level";
  protected static final String REQUEST_CREATE_TIME_ID = "Requests/create_time";
  protected static final String REQUEST_START_TIME_ID = "Requests/start_time";
  protected static final String REQUEST_END_TIME_ID = "Requests/end_time";
  protected static final String REQUEST_TASK_CNT_ID = "Requests/task_count";
  protected static final String REQUEST_FAILED_TASK_CNT_ID = "Requests/failed_task_count";
  protected static final String REQUEST_ABORTED_TASK_CNT_ID = "Requests/aborted_task_count";
  protected static final String REQUEST_TIMED_OUT_TASK_CNT_ID = "Requests/timed_out_task_count";
  protected static final String REQUEST_COMPLETED_TASK_CNT_ID = "Requests/completed_task_count";
  protected static final String REQUEST_QUEUED_TASK_CNT_ID = "Requests/queued_task_count";
  protected static final String REQUEST_PROGRESS_PERCENT_ID = "Requests/progress_percent";
  protected static final String COMMAND_ID = "command";
  protected static final String SERVICE_ID = "service_name";
  protected static final String COMPONENT_ID = "component_name";
  protected static final String HOSTS_ID = "hosts";
  protected static final String ACTION_ID = "action";
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

    String maxResultsRaw = request.getRequestInfoProperties().get(BaseRequest.PAGE_SIZE_PROPERTY_KEY);
    String ascOrderRaw = request.getRequestInfoProperties().get(BaseRequest.ASC_ORDER_PROPERTY_KEY);

    Integer maxResults = (maxResultsRaw == null ? null : Integer.parseInt(maxResultsRaw));
    Boolean ascOrder = (ascOrderRaw == null ? null : Boolean.parseBoolean(ascOrderRaw));

    Set<Map<String, Object>> propertyMaps = new HashSet<Map<String,Object>>();
    
    if (null == predicate) {
      resources.addAll(
          getRequestResources(null, null, null, maxResults, ascOrder, requestedIds));
    } else {
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
  
        resources.addAll(getRequestResources(clusterName, requestId, requestStatus, maxResults,
            ascOrder, requestedIds));
      }
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
  @SuppressWarnings("unchecked")
  private ExecuteActionRequest getActionRequest(Request request)
          throws UnsupportedOperationException {
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

    List<RequestResourceFilter> resourceFilterList = null;
    Set<Map<String, Object>> resourceFilters = null;
    Object resourceFilterObj = propertyMap.get(REQUEST_RESOURCE_FILTER_ID);
    if (resourceFilterObj != null && resourceFilterObj instanceof HashSet) {
      resourceFilters = (HashSet<Map<String, Object>>) resourceFilterObj;
      resourceFilterList = new ArrayList<RequestResourceFilter>();

      for (Map<String, Object> resourceMap : resourceFilters) {
        Object serviceName = resourceMap.get(SERVICE_ID);
        Object componentName = resourceMap.get(COMPONENT_ID);
        Object hosts = resourceMap.get(HOSTS_ID);
        List<String> hostList = null;
        if (hosts != null) {
          hostList = new ArrayList<String>();
          for (String hostName : ((String) hosts).split(",")) {
            hostList.add(hostName.trim());
          }
        }

        resourceFilterList.add(new RequestResourceFilter(
          serviceName != null ? (String) serviceName : null,
          componentName != null ? (String) componentName : null,
          hostList
        ));
      }
    }
    // Extract operation level property
    RequestOperationLevel operationLevel = null;
    if (requestInfoProperties.containsKey(RequestOperationLevel.OPERATION_LEVEL_ID)) {
      operationLevel = new RequestOperationLevel(requestInfoProperties);
    }

    Map<String, String> params = new HashMap<String, String>();
    String keyPrefix = INPUTS_ID + "/";
    for (String key : requestInfoProperties.keySet()) {
      if (key.startsWith(keyPrefix)) {
        params.put(key.substring(keyPrefix.length()), requestInfoProperties.get(key));
      }
    }
    return new ExecuteActionRequest(
      (String) propertyMap.get(REQUEST_CLUSTER_NAME_PROPERTY_ID),
      commandName,
      actionName,
      resourceFilterList,
      operationLevel,
      params);
  }

  // Get all of the request resources for the given properties
  private Set<Resource> getRequestResources(String clusterName,
                                            Long requestId,
                                            String requestStatus,
                                            Integer maxResults,
                                            Boolean ascOrder,
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

      List<Long> requestIds = actionManager.getRequestsByStatus(status,
        maxResults != null ? maxResults : BaseRequest.DEFAULT_PAGE_SIZE,
        ascOrder != null ? ascOrder : false);
      LOG.debug("List<Long> requestIds = actionManager.getRequestsByStatus = {}", requestIds.size());

      response.addAll(getRequestResources(clusterName, actionManager, requestIds,
          requestedPropertyIds));
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

    List<org.apache.ambari.server.actionmanager.Request> requests = actionManager.getRequests(requestIds);
    LOG.debug("requests = actionManager.getRequests(requestIds)={}", requests.size());

    Map<Long, Resource> resourceMap = new HashMap<Long, Resource>();

    for (org.apache.ambari.server.actionmanager.Request request : requests) {
      if ((null == clusterName && null == request.getClusterName()) ||
          (null != clusterName && null != request.getClusterName() && clusterName.equals(request.getClusterName())))
        resourceMap.put(request.getRequestId(), getRequestResource(request, requestedPropertyIds));
    }

    return resourceMap.values();
  }

  private Resource getRequestResource(final org.apache.ambari.server.actionmanager.Request request,
                                      Set<String> requestedPropertyIds) {
    Resource resource = new ResourceImpl(Resource.Type.Request);

    if (null != request.getClusterName())
      setResourceProperty(resource, REQUEST_CLUSTER_NAME_PROPERTY_ID, request.getClusterName(), requestedPropertyIds);

    setResourceProperty(resource, REQUEST_ID_PROPERTY_ID, request.getRequestId(), requestedPropertyIds);
    setResourceProperty(resource, REQUEST_CONTEXT_ID, request.getRequestContext(), requestedPropertyIds);
    setResourceProperty(resource, REQUEST_TYPE_ID, request.getRequestType(), requestedPropertyIds);
    setResourceProperty(resource, REQUEST_INPUTS_ID, request.getInputs(), requestedPropertyIds);
    setResourceProperty(resource, REQUEST_RESOURCE_FILTER_ID, request.getResourceFilters(), requestedPropertyIds);

    RequestOperationLevel operationLevel = request.getOperationLevel();
    String opLevelStr = null;
    if (operationLevel != null) {
      opLevelStr = RequestOperationLevel.getExternalLevelName(
              operationLevel.getLevel().toString());
    }
    setResourceProperty(resource, REQUEST_OPERATION_LEVEL_ID, opLevelStr, requestedPropertyIds);

    setResourceProperty(resource, REQUEST_CREATE_TIME_ID, request.getCreateTime(), requestedPropertyIds);
    setResourceProperty(resource, REQUEST_START_TIME_ID, request.getStartTime(), requestedPropertyIds);
    setResourceProperty(resource, REQUEST_END_TIME_ID, request.getEndTime(), requestedPropertyIds);
    if (request.getRequestScheduleId() != null) {
      setResourceProperty(resource, REQUEST_SOURCE_SCHEDULE_ID, request.getRequestScheduleId(), requestedPropertyIds);
    } else {
      setResourceProperty(resource, REQUEST_SOURCE_SCHEDULE, null, requestedPropertyIds);
    }

    List<HostRoleCommand> commands = request.getCommands();

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

    LOG.debug("taskCount={}, inProgressTaskCount={}, completedTaskCount={}, queuedTaskCount={}, " +
      "pendingTaskCount={}, failedTaskCount={}, abortedTaskCount={}, timedOutTaskCount={}",
      taskCount, inProgressTaskCount, completedTaskCount, queuedTaskCount, pendingTaskCount,
      failedTaskCount, abortedTaskCount, timedOutTaskCount);

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
