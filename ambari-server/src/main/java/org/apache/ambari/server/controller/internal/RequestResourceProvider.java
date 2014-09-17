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
import org.apache.ambari.server.controller.RequestRequest;
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
import java.util.Iterator;
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
  protected static final String REQUEST_ABORT_REASON_PROPERTY_ID = "Requests/abort_reason";
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
  protected static final String REQUEST_EXCLUSIVE_ID = "Requests/exclusive";
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
  protected static final String EXLUSIVE_ID = "exclusive";
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
  public RequestStatus updateResources(Request requestInfo, Predicate predicate)
          throws SystemException, UnsupportedPropertyException,
          NoSuchResourceException, NoSuchParentResourceException {
    AmbariManagementController amc = getManagementController();
    final Set<RequestRequest> requests = new HashSet<RequestRequest>();

    Iterator<Map<String,Object>> iterator = requestInfo.getProperties().iterator();
    if (iterator.hasNext()) {
      for (Map<String, Object> propertyMap : getPropertyMaps(iterator.next(), predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }
    // Validate
    List<org.apache.ambari.server.actionmanager.Request> targets =
            new ArrayList<org.apache.ambari.server.actionmanager.Request>();
    for (RequestRequest updateRequest : requests) {
      ActionManager actionManager = amc.getActionManager();
      List<org.apache.ambari.server.actionmanager.Request> internalRequests =
              actionManager.getRequests(Collections.singletonList(updateRequest.getRequestId()));
      if (internalRequests.size() == 0) {
        throw new IllegalArgumentException(
                String.format("Request %s does not exist", updateRequest.getRequestId()));
      }
      // There should be only one request with this id (or no request at all)
      org.apache.ambari.server.actionmanager.Request internalRequest = internalRequests.get(0);
      // Validate update request (check constraints on state value and presense of abort reason)
      if (updateRequest.getAbortReason() == null || updateRequest.getAbortReason().isEmpty()) {
        throw new IllegalArgumentException("Abort reason can not be empty.");
      }

      HostRoleStatus internalRequestStatus = calculateSummaryStatus(
              calculateRequestStatusCounters(internalRequest),
              internalRequest.getCommands().size()
      );
      if (updateRequest.getStatus() != HostRoleStatus.ABORTED) {
        throw new IllegalArgumentException(
                String.format("%s is wrong value. The only allowed value " +
                                "for updating request status is ABORTED",
                        updateRequest.getStatus()));
      }
      if (internalRequestStatus.isCompletedState()) {
        throw new IllegalArgumentException(
                String.format("Can not set request that is in %s state to %s state.",
                        internalRequestStatus.toString(), updateRequest.getStatus()));
      }
      // Validation passed
      targets.add(internalRequest);
    }
    // Perform update
    Iterator<RequestRequest> reqIterator = requests.iterator();
    for (int i = 0; i < targets.size(); i++) {
      org.apache.ambari.server.actionmanager.Request target = targets.get(i);
      String reason = reqIterator.next().getAbortReason();
      amc.getActionManager().cancelRequest(target.getRequestId(), reason);
    }
    return getRequestStatus(null);
  }

  private RequestRequest getRequest(Map<String, Object> propertyMap) {
    // Cluster name may be empty for custom actions
    String clusterNameStr = (String) propertyMap.get(REQUEST_CLUSTER_NAME_PROPERTY_ID);
    String requestIdStr = (String) propertyMap.get(REQUEST_ID_PROPERTY_ID);
    long requestId = Integer.valueOf(requestIdStr);
    String requestStatusStr = (String) propertyMap.get(REQUEST_STATUS_PROPERTY_ID);
    HostRoleStatus requestStatus = null;
    if (requestStatusStr != null) {
      // This conversion may throw IllegalArgumentException, it is OK
      // in this case it will be mapped to HTTP 400 Bad Request
      requestStatus = HostRoleStatus.valueOf(requestStatusStr);
    }
    String abortReason = (String) propertyMap.get(REQUEST_ABORT_REASON_PROPERTY_ID);
    RequestRequest requestRequest = new RequestRequest(clusterNameStr, requestId);
    requestRequest.setStatus(requestStatus);
    requestRequest.setAbortReason(abortReason);
    return requestRequest;

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

    boolean exclusive = false;
    if (requestInfoProperties.containsKey(EXLUSIVE_ID)) {
      exclusive = Boolean.valueOf(requestInfoProperties.get(EXLUSIVE_ID).trim());
    }

    return new ExecuteActionRequest(
      (String) propertyMap.get(REQUEST_CLUSTER_NAME_PROPERTY_ID),
      commandName,
      actionName,
      resourceFilterList,
      operationLevel,
      params, exclusive);
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
    setResourceProperty(resource, REQUEST_EXCLUSIVE_ID, request.isExclusive(), requestedPropertyIds);

    if (request.getRequestScheduleId() != null) {
      setResourceProperty(resource, REQUEST_SOURCE_SCHEDULE_ID, request.getRequestScheduleId(), requestedPropertyIds);
    } else {
      setResourceProperty(resource, REQUEST_SOURCE_SCHEDULE, null, requestedPropertyIds);
    }
    int taskCount = request.getCommands().size();

    Map<HostRoleStatus, Integer> hostRoleStatusCounters = calculateRequestStatusCounters(request);
    HostRoleStatus requestStatus = calculateSummaryStatus(hostRoleStatusCounters, taskCount);

    double progressPercent =
            ((hostRoleStatusCounters.get(HostRoleStatus.QUEUED) * 0.09 +
                    hostRoleStatusCounters.get(HostRoleStatus.IN_PROGRESS) * 0.35 +
                    hostRoleStatusCounters.get(HostRoleStatus.COMPLETED)) / (double) taskCount) * 100.0;

    setResourceProperty(resource, REQUEST_STATUS_PROPERTY_ID, requestStatus.toString(), requestedPropertyIds);
    setResourceProperty(resource, REQUEST_TASK_CNT_ID, taskCount, requestedPropertyIds);
    setResourceProperty(resource, REQUEST_FAILED_TASK_CNT_ID,
            hostRoleStatusCounters.get(HostRoleStatus.FAILED), requestedPropertyIds);
    setResourceProperty(resource, REQUEST_ABORTED_TASK_CNT_ID,
            hostRoleStatusCounters.get(HostRoleStatus.ABORTED), requestedPropertyIds);
    setResourceProperty(resource, REQUEST_TIMED_OUT_TASK_CNT_ID,
            hostRoleStatusCounters.get(HostRoleStatus.TIMEDOUT), requestedPropertyIds);
    setResourceProperty(resource, REQUEST_QUEUED_TASK_CNT_ID,
            hostRoleStatusCounters.get(HostRoleStatus.QUEUED), requestedPropertyIds);
    setResourceProperty(resource, REQUEST_COMPLETED_TASK_CNT_ID,
            hostRoleStatusCounters.get(HostRoleStatus.COMPLETED), requestedPropertyIds);
    setResourceProperty(resource, REQUEST_PROGRESS_PERCENT_ID, progressPercent, requestedPropertyIds);

    return resource;
  }

  /**
   * Returns counts of tasks that are in various states.
   */
  private Map<HostRoleStatus, Integer> calculateRequestStatusCounters(org.apache.ambari.server.actionmanager.Request request) {
    List<HostRoleCommand> commands = request.getCommands();
    Map<HostRoleStatus, Integer> counters = new HashMap<HostRoleStatus, Integer>();
    int totalTasks = commands.size();
    // initialize
    for (HostRoleStatus hostRoleStatus : HostRoleStatus.values()) {
      counters.put(hostRoleStatus, 0);
    }
    // calculate counts
    for (HostRoleCommand hostRoleCommand : commands) {
      HostRoleStatus status = hostRoleCommand.getStatus();
      if (status.isCompletedState() &&
            status != HostRoleStatus.COMPLETED ) { // we don't want to count twice
        // Increase total number of completed tasks;
        counters.put(HostRoleStatus.COMPLETED, counters.get(HostRoleStatus.COMPLETED) + 1);
      }
      // Increment counter for particular status
      counters.put(status, counters.get(status) + 1);
    }
    // We overwrite the value to have the sum converged
    counters.put(HostRoleStatus.IN_PROGRESS,
            totalTasks - counters.get(HostRoleStatus.COMPLETED)-
                    counters.get(HostRoleStatus.QUEUED)-
                    counters.get(HostRoleStatus.PENDING));
    return counters;
  }

  /**
   * @param counters counts of tasks that are in various states.
   * @param totalTasks total number of tasks in request
   * @return summary request status based on statuses of tasks in different
   * states.
   */
  private HostRoleStatus calculateSummaryStatus(Map<HostRoleStatus, Integer> counters, int totalTasks) {
    return counters.get(HostRoleStatus.FAILED) > 0 ? HostRoleStatus.FAILED :
            // TODO (dlysnichenko): maybe change order of FAILED and ABORTED?
            counters.get(HostRoleStatus.ABORTED) > 0 ? HostRoleStatus.ABORTED :
                    counters.get(HostRoleStatus.TIMEDOUT) > 0 ? HostRoleStatus.TIMEDOUT :
                            counters.get(HostRoleStatus.IN_PROGRESS) > 0 ? HostRoleStatus.IN_PROGRESS :
                                    counters.get(HostRoleStatus.COMPLETED) == totalTasks ? HostRoleStatus.COMPLETED :
                                            HostRoleStatus.PENDING;
  }
}
