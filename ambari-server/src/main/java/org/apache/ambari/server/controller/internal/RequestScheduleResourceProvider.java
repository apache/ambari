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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.api.services.parsers.RequestBodyParser;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RequestScheduleRequest;
import org.apache.ambari.server.controller.RequestScheduleResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.scheduler.Batch;
import org.apache.ambari.server.state.scheduler.BatchRequest;
import org.apache.ambari.server.state.scheduler.BatchSettings;
import org.apache.ambari.server.state.scheduler.RequestExecution;
import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;
import org.apache.ambari.server.state.scheduler.Schedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestScheduleResourceProvider extends AbstractControllerResourceProvider {
  private static final Logger LOG = LoggerFactory.getLogger
    (RequestScheduleResourceProvider.class);

  protected static final String REQUEST_SCHEDULE_ID_PROPERTY_ID =
    PropertyHelper.getPropertyId("RequestSchedule", "id");
  protected static final String REQUEST_SCHEDULE_CLUSTER_NAME_PROPERTY_ID =
    PropertyHelper.getPropertyId("RequestSchedule", "cluster_name");
  protected static final String REQUEST_SCHEDULE_DESC_PROPERTY_ID =
    PropertyHelper.getPropertyId("RequestSchedule", "description");
  protected static final String REQUEST_SCHEDULE_STATUS_PROPERTY_ID =
    PropertyHelper.getPropertyId("RequestSchedule", "status");
  protected static final String REQUEST_SCHEDULE_LAST_STATUS_PROPERTY_ID =
    PropertyHelper.getPropertyId("RequestSchedule", "last_execution_status");
  protected static final String REQUEST_SCHEDULE_BATCH_PROPERTY_ID =
    PropertyHelper.getPropertyId("RequestSchedule", "batch");
  protected static final String REQUEST_SCHEDULE_SCHEDULE_PROPERTY_ID =
    PropertyHelper.getPropertyId("RequestSchedule", "schedule");
  protected static final String REQUEST_SCHEDULE_CREATE_USER_PROPERTY_ID =
    PropertyHelper.getPropertyId("RequestSchedule", "create_user");
  protected static final String REQUEST_SCHEDULE_AUTHENTICATED_USER_PROPERTY_ID =
    PropertyHelper.getPropertyId("RequestSchedule", "authenticated_user");
  protected static final String REQUEST_SCHEDULE_UPDATE_USER_PROPERTY_ID =
    PropertyHelper.getPropertyId("RequestSchedule", "update_user");
  protected static final String REQUEST_SCHEDULE_CREATE_TIME_PROPERTY_ID =
    PropertyHelper.getPropertyId("RequestSchedule", "create_time");
  protected static final String REQUEST_SCHEDULE_UPDATE_TIME_PROPERTY_ID =
    PropertyHelper.getPropertyId("RequestSchedule", "update_time");

  protected static final String REQUEST_SCHEDULE_BATCH_SEPARATION_PROPERTY_ID =
    PropertyHelper.getPropertyId("batch_settings", "batch_separation_in_seconds");
  protected static final String REQUEST_SCHEDULE_BATCH_TOLERATION_PROPERTY_ID =
    PropertyHelper.getPropertyId("batch_settings", "task_failure_tolerance");
  protected static final String REQUEST_SCHEDULE_BATCH_REQUESTS_PROPERTY_ID =
    PropertyHelper.getPropertyId(null, "requests");

  protected static final String BATCH_REQUEST_TYPE_PROPERTY_ID =
    PropertyHelper.getPropertyId(null, "type");
  protected static final String BATCH_REQUEST_URI_PROPERTY_ID =
    PropertyHelper.getPropertyId(null, "uri");
  protected static final String BATCH_REQUEST_ORDER_ID_PROPERTY_ID =
    PropertyHelper.getPropertyId(null, "order_id");
  protected static final String BATCH_REQUEST_BODY_PROPERTY_ID =
    PropertyHelper.getPropertyId(null, RequestBodyParser.REQUEST_BLOB_TITLE);

  protected static final String SCHEDULE_DAYS_OF_MONTH_PROPERTY_ID =
    PropertyHelper.getPropertyId(REQUEST_SCHEDULE_SCHEDULE_PROPERTY_ID, "days_of_month");
  protected static final String SCHEDULE_MINUTES_PROPERTY_ID =
    PropertyHelper.getPropertyId(REQUEST_SCHEDULE_SCHEDULE_PROPERTY_ID, "minutes");
  protected static final String SCHEDULE_HOURS_PROPERTY_ID =
    PropertyHelper.getPropertyId(REQUEST_SCHEDULE_SCHEDULE_PROPERTY_ID, "hours");
  protected static final String SCHEDULE_YEAR_PROPERTY_ID =
    PropertyHelper.getPropertyId(REQUEST_SCHEDULE_SCHEDULE_PROPERTY_ID, "year");
  protected static final String SCHEDULE_DAY_OF_WEEK_PROPERTY_ID =
    PropertyHelper.getPropertyId(REQUEST_SCHEDULE_SCHEDULE_PROPERTY_ID, "day_of_week");
  protected static final String SCHEDULE_MONTH_PROPERTY_ID =
    PropertyHelper.getPropertyId(REQUEST_SCHEDULE_SCHEDULE_PROPERTY_ID, "month");
  protected static final String SCHEDULE_START_TIME_PROPERTY_ID =
    PropertyHelper.getPropertyId(REQUEST_SCHEDULE_SCHEDULE_PROPERTY_ID, "startTime");
  protected static final String SCHEDULE_END_TIME_PROPERTY_ID =
    PropertyHelper.getPropertyId(REQUEST_SCHEDULE_SCHEDULE_PROPERTY_ID, "endTime");

  private static Set<String> pkPropertyIds = new HashSet<String>(Arrays
    .asList(new String[]{ REQUEST_SCHEDULE_ID_PROPERTY_ID }));

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds          the property ids
   * @param keyPropertyIds       the key property ids
   * @param managementController the management controller
   */

  protected RequestScheduleResourceProvider(Set<String> propertyIds,
      Map<Resource.Type, String> keyPropertyIds,
        AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }


  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  @Override
  public RequestStatus createResources(Request request) throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException,
      NoSuchParentResourceException {

    final Set<RequestScheduleRequest> requests = new HashSet<RequestScheduleRequest>();

    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(getRequestScheduleRequest(propertyMap));
    }

    Set<RequestScheduleResponse> responses =
      createResources(new Command<Set<RequestScheduleResponse>>() {
      @Override
      public Set<RequestScheduleResponse> invoke() throws AmbariException {
        return createRequestSchedules(requests);
      }
    });

    notifyCreate(Resource.Type.RequestSchedule, request);

    Set<Resource> associatedResources = new HashSet<Resource>();
    for (RequestScheduleResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.RequestSchedule);
      resource.setProperty(REQUEST_SCHEDULE_ID_PROPERTY_ID, response.getId());
      associatedResources.add(resource);
    }

    return getRequestStatus(null, associatedResources);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws
      SystemException, UnsupportedPropertyException, NoSuchResourceException,
      NoSuchParentResourceException {

    final Set<RequestScheduleRequest> requests = new HashSet<RequestScheduleRequest>();
    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      requests.add(getRequestScheduleRequest(propertyMap));
    }

    Set<RequestScheduleResponse> responses =
      getResources(new Command<Set<RequestScheduleResponse>>() {
        @Override
        public Set<RequestScheduleResponse> invoke() throws AmbariException {
          return getRequestSchedules(requests);
        }
      });

    Set<String>   requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources    = new HashSet<Resource>();

    for (RequestScheduleResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.RequestSchedule);

      setResourceProperty(resource, REQUEST_SCHEDULE_ID_PROPERTY_ID,
        response.getId(), requestedIds);
      setResourceProperty(resource, REQUEST_SCHEDULE_CLUSTER_NAME_PROPERTY_ID,
        response.getClusterName(), requestedIds);
      setResourceProperty(resource, REQUEST_SCHEDULE_DESC_PROPERTY_ID,
        response.getDescription(), requestedIds);
      setResourceProperty(resource, REQUEST_SCHEDULE_STATUS_PROPERTY_ID,
        response.getStatus(), requestedIds);
      setResourceProperty(resource, REQUEST_SCHEDULE_LAST_STATUS_PROPERTY_ID,
        response.getLastExecutionStatus(), requestedIds);
      setResourceProperty(resource, REQUEST_SCHEDULE_BATCH_PROPERTY_ID,
        response.getBatch(), requestedIds);
      setResourceProperty(resource, REQUEST_SCHEDULE_SCHEDULE_PROPERTY_ID,
        response.getSchedule(), requestedIds);
      setResourceProperty(resource, REQUEST_SCHEDULE_CREATE_USER_PROPERTY_ID,
        response.getCreateUser(), requestedIds);
      setResourceProperty(resource, REQUEST_SCHEDULE_AUTHENTICATED_USER_PROPERTY_ID,
        response.getAuthenticatedUserId(), requestedIds);
      setResourceProperty(resource, REQUEST_SCHEDULE_CREATE_TIME_PROPERTY_ID,
        response.getCreateTime(), requestedIds);
      setResourceProperty(resource, REQUEST_SCHEDULE_UPDATE_USER_PROPERTY_ID,
        response.getUpdateUser(), requestedIds);
      setResourceProperty(resource, REQUEST_SCHEDULE_UPDATE_TIME_PROPERTY_ID,
        response.getUpdateTime(), requestedIds);

      resources.add(resource);
    }

    return resources;
  }

  /**
   * Currently unsupported operation. Since strong guarantees are required
   * that no jobs are currently running.
   * @param request    the request object which defines the set of properties
   *                   for the resources to be updated
   * @param predicate  the predicate object which can be used to filter which
   *                   resources are updated
   *
   * @return
   * @throws SystemException
   * @throws UnsupportedPropertyException
   * @throws NoSuchResourceException
   * @throws NoSuchParentResourceException
   */
  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    final Set<RequestScheduleRequest> requests = new
      HashSet<RequestScheduleRequest>();

    Iterator<Map<String,Object>> iterator = request.getProperties().iterator();
    if (iterator.hasNext()) {
      for (Map<String, Object> propertyMap : getPropertyMaps(iterator.next(), predicate)) {
        requests.add(getRequestScheduleRequest(propertyMap));
      }

      modifyResources(new Command<Void>() {
        @Override
        public Void invoke() throws AmbariException {
          updateRequestSchedule(requests);
          return null;
        }
      });
    }

    notifyUpdate(Resource.Type.RequestSchedule, request, predicate);

    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate) throws
      SystemException, UnsupportedPropertyException, NoSuchResourceException,
      NoSuchParentResourceException {

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      final RequestScheduleRequest requestScheduleRequest =
        getRequestScheduleRequest(propertyMap);

      modifyResources(new Command<Void>() {
        @Override
        public Void invoke() throws AmbariException {
          deleteRequestSchedule(requestScheduleRequest);
          return null;
        }
      });
    }

    notifyDelete(Resource.Type.RequestSchedule, predicate);

    return getRequestStatus(null);
  }

  private synchronized void deleteRequestSchedule(RequestScheduleRequest request)
    throws AmbariException {

    if (request.getId() == null) {
      throw new AmbariException("Id is a required field.");
    }

    Clusters clusters = getManagementController().getClusters();

    Cluster cluster;
    try {
      cluster = clusters.getCluster(request.getClusterName());
    } catch (ClusterNotFoundException e) {
      throw new ParentObjectNotFoundException(
        "Attempted to delete a request schedule from a cluster which doesn't "
          + "exist", e);
    }

    RequestExecution requestExecution =
      cluster.getAllRequestExecutions().get(request.getId());

    if (requestExecution == null) {
      throw new AmbariException("Request Schedule not found "
        + ", clusterName = " + request.getClusterName()
        + ", description = " + request.getDescription()
        + ", id = " + request.getId());
    }

    String username = getManagementController().getAuthName();

    LOG.info("Disabling Request Schedule "
      + ", clusterName = " + request.getClusterName()
      + ", id = " + request.getId()
      + ", user = " + username);

    // Delete all jobs and triggers
    getManagementController().getExecutionScheduleManager()
      .deleteAllJobs(requestExecution);

    requestExecution.updateStatus(RequestExecution.Status.DISABLED);
  }

  private synchronized void updateRequestSchedule
    (Set<RequestScheduleRequest> requests) throws AmbariException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return;
    }

    Clusters clusters = getManagementController().getClusters();

    for (RequestScheduleRequest request : requests) {

      validateRequest(request);

      Cluster cluster;
      try {
        cluster = clusters.getCluster(request.getClusterName());
      } catch (ClusterNotFoundException e) {
        throw new ParentObjectNotFoundException(
          "Attempted to add a request schedule to a cluster which doesn't " +
            "exist", e);
      }

      if (request.getId() == null) {
        throw new AmbariException("Id is a required parameter.");
      }

      RequestExecution requestExecution =
        cluster.getAllRequestExecutions().get(request.getId());

      if (requestExecution == null) {
        throw new AmbariException("Request Schedule not found "
          + ", clusterName = " + request.getClusterName()
          + ", description = " + request.getDescription()
          + ", id = " + request.getId());
      }

      String username = getManagementController().getAuthName();
      Integer userId = getManagementController().getAuthId();

      requestExecution.setBatch(request.getBatch());
      requestExecution.setDescription(request.getDescription());
      requestExecution.setSchedule(request.getSchedule());
      if (request.getStatus() != null && isValidRequestScheduleStatus
          (request.getStatus())) {
        requestExecution.setStatus(RequestExecution.Status.valueOf(request.getStatus()));
      }
      requestExecution.setUpdateUser(username);
      requestExecution.setAuthenticatedUserId(userId);

      LOG.info("Persisting updated Request Schedule "
        + ", clusterName = " + request.getClusterName()
        + ", description = " + request.getDescription()
        + ", user = " + username);

      requestExecution.persist();

      // Update schedule for the batch
      getManagementController().getExecutionScheduleManager()
        .updateBatchSchedule(requestExecution);
    }
  }

  private synchronized Set<RequestScheduleResponse> createRequestSchedules
    (Set<RequestScheduleRequest> requests) throws AmbariException {

    if (requests.isEmpty()) {
      LOG.warn("Received an empty requests set");
      return null;
    }

    Set<RequestScheduleResponse> responses = new
      HashSet<RequestScheduleResponse>();

    Clusters clusters = getManagementController().getClusters();
    RequestExecutionFactory requestExecutionFactory =
      getManagementController().getRequestExecutionFactory();

    for (RequestScheduleRequest request : requests) {

      validateRequest(request);

      Cluster cluster;
      try {
        cluster = clusters.getCluster(request.getClusterName());
      } catch (ClusterNotFoundException e) {
        throw new ParentObjectNotFoundException(
          "Attempted to add a request schedule to a cluster which doesn't " +
            "exist", e);
      }

      String username = getManagementController().getAuthName();
      Integer userId = getManagementController().getAuthId();

      RequestExecution requestExecution = requestExecutionFactory.createNew
        (cluster, request.getBatch(), request.getSchedule());

      requestExecution.setCreateUser(username);
      requestExecution.setUpdateUser(username);
      requestExecution.setAuthenticatedUserId(userId);
      requestExecution.setStatus(RequestExecution.Status.SCHEDULED);

      LOG.info("Persisting new Request Schedule "
        + ", clusterName = " + request.getClusterName()
        + ", description = " + request.getDescription()
        + ", user = " + username);

      requestExecution.persist();
      cluster.addRequestExecution(requestExecution);

      // Setup batch schedule
      getManagementController().getExecutionScheduleManager()
        .scheduleBatch(requestExecution);

      RequestScheduleResponse response = new RequestScheduleResponse
        (requestExecution.getId(), requestExecution.getClusterName(),
          requestExecution.getDescription(), requestExecution.getStatus(),
          requestExecution.getLastExecutionStatus(),
          requestExecution.getBatch(), request.getSchedule(),
          requestExecution.getCreateUser(), requestExecution.getCreateTime(),
          requestExecution.getUpdateUser(), requestExecution.getUpdateTime(),
          requestExecution.getAuthenticatedUserId());

      responses.add(response);
    }

    return responses;
  }

  private void validateRequest(RequestScheduleRequest request) throws AmbariException {
    if (request.getClusterName() == null) {
      throw new IllegalArgumentException("Cluster name is required.");
    }
    Schedule schedule = request.getSchedule();
    if (schedule != null) {
      getManagementController().getExecutionScheduleManager()
        .validateSchedule(schedule);
    }
    Batch batch = request.getBatch();
    if (batch != null && !batch.getBatchRequests().isEmpty()) {
      // Verify requests can be ordered
      HashSet<Long> orderIdSet = new HashSet<Long>();
      for (BatchRequest batchRequest : batch.getBatchRequests()) {
        if (batchRequest.getOrderId() == null) {
          throw new AmbariException("No order id provided for batch request. " +
            "" + batchRequest);
        }
        if (orderIdSet.contains(batchRequest.getOrderId())) {
          throw new AmbariException("Duplicate order id provided for batch " +
            "request. " + batchRequest);
        }
        orderIdSet.add(batchRequest.getOrderId());
      }
    }
  }

  private synchronized Set<RequestScheduleResponse> getRequestSchedules
    (Set<RequestScheduleRequest> requests) throws AmbariException {

    Set<RequestScheduleResponse> responses = new
      HashSet<RequestScheduleResponse>();

    if (requests != null) {
      for (RequestScheduleRequest request : requests) {
        if (request.getClusterName() == null) {
          LOG.warn("Cluster name is a required field.");
          continue;
        }

        Cluster cluster = getManagementController().getClusters().getCluster
          (request.getClusterName());

        Map<Long, RequestExecution> allRequestExecutions =
          cluster.getAllRequestExecutions();

        // Find by id
        if (request.getId() != null) {
          RequestExecution requestExecution = allRequestExecutions.get
            (request.getId());
          if (requestExecution != null) {
            responses.add(requestExecution.convertToResponseWithBody());
          }
          continue;
        }
        // Find by status
        if (request.getStatus() != null) {
          for (RequestExecution requestExecution : allRequestExecutions.values()) {
            if (requestExecution.getStatus().equals(request.getStatus())) {
              responses.add(requestExecution.convertToResponse());
            }
          }
          continue;
        }
        // TODO: Find by status of Batch Request(s) and start time greater than requested time

        // Select all
        for (RequestExecution requestExecution : allRequestExecutions.values()) {
          responses.add(requestExecution.convertToResponse());
        }
      }
    }

    return responses;
  }

  private boolean isValidRequestScheduleStatus(String giveStatus) {
    for (RequestExecution.Status status : RequestExecution.Status.values()) {
      if (status.name().equalsIgnoreCase(giveStatus)) {
        return true;
      }
    }
    return false;
  }

  private RequestScheduleRequest getRequestScheduleRequest(Map<String, Object> properties) {
    Object idObj = properties.get(REQUEST_SCHEDULE_ID_PROPERTY_ID);
    Long id = null;
    if (idObj != null)  {
      id = idObj instanceof Long ? (Long) idObj :
        Long.parseLong((String) idObj);
    }

    RequestScheduleRequest requestScheduleRequest = new RequestScheduleRequest(
      id,
      (String) properties.get(REQUEST_SCHEDULE_CLUSTER_NAME_PROPERTY_ID),
      (String) properties.get(REQUEST_SCHEDULE_DESC_PROPERTY_ID),
      (String) properties.get(REQUEST_SCHEDULE_STATUS_PROPERTY_ID),
      null,
      null);

    Batch batch = new Batch();
    BatchSettings batchSettings = new BatchSettings();
    List<BatchRequest> batchRequests = new ArrayList<BatchRequest>();

    Object batchObject = properties.get(REQUEST_SCHEDULE_BATCH_PROPERTY_ID);
    if (batchObject != null && batchObject instanceof HashSet<?>) {
      try {
        HashSet<Map<String, Object>> batchMap = (HashSet<Map<String, Object>>) batchObject;

        for (Map<String, Object> batchEntry : batchMap) {
          if (batchEntry != null) {
            for (Map.Entry<String, Object> batchMapEntry : batchEntry.entrySet()) {
              if (batchMapEntry.getKey().equals
                  (REQUEST_SCHEDULE_BATCH_TOLERATION_PROPERTY_ID)) {
                batchSettings.setTaskFailureToleranceLimit(Integer.valueOf
                  ((String) batchMapEntry.getValue()));
              } else if (batchMapEntry.getKey().equals
                  (REQUEST_SCHEDULE_BATCH_SEPARATION_PROPERTY_ID)) {
                batchSettings.setBatchSeparationInSeconds(Integer.valueOf
                  ((String) batchMapEntry.getValue()));
              } else if (batchMapEntry.getKey().equals
                  (REQUEST_SCHEDULE_BATCH_REQUESTS_PROPERTY_ID)) {
                HashSet<Map<String, Object>> requestSet =
                  (HashSet<Map<String, Object>>) batchMapEntry.getValue();

                for (Map<String, Object> requestEntry : requestSet) {
                  if (requestEntry != null) {
                    BatchRequest batchRequest = new BatchRequest();
                    for (Map.Entry<String, Object> requestMapEntry :
                        requestEntry.entrySet()) {
                      if (requestMapEntry.getKey()
                                 .equals(BATCH_REQUEST_TYPE_PROPERTY_ID)) {
                        batchRequest.setType(BatchRequest.Type.valueOf
                          ((String) requestMapEntry.getValue()));
                      } else if (requestMapEntry.getKey()
                                 .equals(BATCH_REQUEST_URI_PROPERTY_ID)) {
                        batchRequest.setUri(
                          (String) requestMapEntry.getValue());
                      } else if (requestMapEntry.getKey()
                                .equals(BATCH_REQUEST_ORDER_ID_PROPERTY_ID)) {
                        batchRequest.setOrderId(Long.parseLong(
                          (String) requestMapEntry.getValue()));
                      } else if (requestMapEntry.getKey()
                                .equals(BATCH_REQUEST_BODY_PROPERTY_ID)) {
                        batchRequest.setBody(
                          (String) requestMapEntry.getValue());
                      }
                    }
                    batchRequests.add(batchRequest);
                  }
                }
              }
            }
          }
        }

        batch.getBatchRequests().addAll(batchRequests);
        batch.setBatchSettings(batchSettings);

      } catch (Exception e) {
        LOG.warn("Request Schedule batch json is unparseable. " +
          batchObject, e);
      }
    }

    requestScheduleRequest.setBatch(batch);

    Schedule schedule = new Schedule();
    for (Map.Entry<String, Object> propertyEntry : properties.entrySet()) {
      if (propertyEntry.getKey().equals(SCHEDULE_DAY_OF_WEEK_PROPERTY_ID)) {
        schedule.setDayOfWeek((String) propertyEntry.getValue());
      } else if (propertyEntry.getKey().equals(SCHEDULE_DAYS_OF_MONTH_PROPERTY_ID)) {
        schedule.setDaysOfMonth((String) propertyEntry.getValue());
      } else if (propertyEntry.getKey().equals(SCHEDULE_END_TIME_PROPERTY_ID)) {
        schedule.setEndTime((String) propertyEntry.getValue());
      } else if (propertyEntry.getKey().equals(SCHEDULE_HOURS_PROPERTY_ID)) {
        schedule.setHours((String) propertyEntry.getValue());
      } else if (propertyEntry.getKey().equals(SCHEDULE_MINUTES_PROPERTY_ID)) {
        schedule.setMinutes((String) propertyEntry.getValue());
      } else if (propertyEntry.getKey().equals(SCHEDULE_MONTH_PROPERTY_ID)) {
        schedule.setMonth((String) propertyEntry.getValue());
      } else if (propertyEntry.getKey().equals(SCHEDULE_START_TIME_PROPERTY_ID)) {
        schedule.setStartTime((String) propertyEntry.getValue());
      } else if (propertyEntry.getKey().equals(SCHEDULE_YEAR_PROPERTY_ID)) {
        schedule.setYear((String) propertyEntry.getValue());
      }
    }

    if (!schedule.isEmpty()) {
      requestScheduleRequest.setSchedule(schedule);
    }

    return requestScheduleRequest;
  }
}
