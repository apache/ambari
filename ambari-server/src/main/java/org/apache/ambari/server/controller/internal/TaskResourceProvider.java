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
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.TaskStatusRequest;
import org.apache.ambari.server.controller.TaskStatusResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Resource provider for task resources.
 */
class TaskResourceProvider extends ResourceProviderImpl{

  // ----- Property ID constants ---------------------------------------------

  // Tasks
  protected static final PropertyId TASK_CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("cluster_name","Tasks");
  protected static final PropertyId TASK_REQUEST_ID_PROPERTY_ID   = PropertyHelper.getPropertyId("request_id","Tasks");
  protected static final PropertyId TASK_ID_PROPERTY_ID           = PropertyHelper.getPropertyId("id","Tasks");
  protected static final PropertyId TASK_STAGE_ID_PROPERTY_ID     = PropertyHelper.getPropertyId("stage_id","Tasks");
  protected static final PropertyId TASK_HOST_NAME_PROPERTY_ID    = PropertyHelper.getPropertyId("host_name","Tasks");
  protected static final PropertyId TASK_ROLE_PROPERTY_ID         = PropertyHelper.getPropertyId("role","Tasks");
  protected static final PropertyId TASK_COMMAND_PROPERTY_ID      = PropertyHelper.getPropertyId("command","Tasks");
  protected static final PropertyId TASK_STATUS_PROPERTY_ID       = PropertyHelper.getPropertyId("status","Tasks");
  protected static final PropertyId TASK_EXIT_CODE_PROPERTY_ID    = PropertyHelper.getPropertyId("exit_code","Tasks");
  protected static final PropertyId TASK_STDERR_PROPERTY_ID       = PropertyHelper.getPropertyId("stderr","Tasks");
  protected static final PropertyId TASK_STOUT_PROPERTY_ID        = PropertyHelper.getPropertyId("stdout","Tasks");
  protected static final PropertyId TASK_START_TIME_PROPERTY_ID   = PropertyHelper.getPropertyId("start_time","Tasks");
  protected static final PropertyId TASK_ATTEMPT_CNT_PROPERTY_ID  = PropertyHelper.getPropertyId("attempt_cnt","Tasks");


  private static Set<PropertyId> pkPropertyIds =
      new HashSet<PropertyId>(Arrays.asList(new PropertyId[]{
          TASK_ID_PROPERTY_ID}));

  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds           the property ids
   * @param keyPropertyIds        the key property ids
   * @param managementController  the management controller
   */
  TaskResourceProvider(Set<PropertyId> propertyIds,
                       Map<Resource.Type, PropertyId> keyPropertyIds,
                       AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  // ----- ResourceProvider ------------------------------------------------

  @Override
  public RequestStatus createResources(Request request) throws AmbariException {
    throw new UnsupportedOperationException("Not currently supported.");
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate) throws AmbariException {
    Set<PropertyId> requestedIds = PropertyHelper.getRequestPropertyIds(getPropertyIds(), request, predicate);
    Map<PropertyId, Object> predicateProperties = getProperties(predicate);
    TaskStatusRequest taskStatusRequest = getRequest(predicateProperties);

    String clusterName = (String) predicateProperties.get(TASK_CLUSTER_NAME_PROPERTY_ID);
    Long   request_id  = new Long((String) predicateProperties.get(TASK_REQUEST_ID_PROPERTY_ID));

    // TODO : handle multiple requests
    LOG.info("Request to management controller " + taskStatusRequest.getRequestId() +
        " taskid " + taskStatusRequest.getTaskId());

    Set<TaskStatusResponse> responses = getManagementController().getTaskStatus(Collections.singleton(taskStatusRequest));
    LOG.info("Printing size of responses " + responses.size());
    for (TaskStatusResponse response: responses) {
      LOG.info("Printing response from management controller " + response.toString());
    }

    Set<Resource> resources = new HashSet<Resource>();
    for (TaskStatusResponse response : responses) {
      Resource resource = new ResourceImpl(Resource.Type.Task);

      setResourceProperty(resource, TASK_CLUSTER_NAME_PROPERTY_ID, clusterName, requestedIds);
      setResourceProperty(resource, TASK_REQUEST_ID_PROPERTY_ID, request_id, requestedIds);
      setResourceProperty(resource, TASK_ID_PROPERTY_ID, response.getTaskId(), requestedIds);
      setResourceProperty(resource, TASK_STAGE_ID_PROPERTY_ID, response.getStageId(), requestedIds);
      setResourceProperty(resource, TASK_HOST_NAME_PROPERTY_ID, response.getHostName(), requestedIds);
      setResourceProperty(resource, TASK_ROLE_PROPERTY_ID, response.getRole(), requestedIds);
      setResourceProperty(resource, TASK_COMMAND_PROPERTY_ID, response.getCommand(), requestedIds);
      setResourceProperty(resource, TASK_STATUS_PROPERTY_ID, response.getStatus(), requestedIds);
      setResourceProperty(resource, TASK_EXIT_CODE_PROPERTY_ID, response.getExitCode(), requestedIds);
      setResourceProperty(resource, TASK_STDERR_PROPERTY_ID, response.getStderr(), requestedIds);
      setResourceProperty(resource, TASK_STOUT_PROPERTY_ID, response.getStdout(), requestedIds);
      setResourceProperty(resource, TASK_START_TIME_PROPERTY_ID, response.getStartTime(), requestedIds);
      setResourceProperty(resource, TASK_ATTEMPT_CNT_PROPERTY_ID, response.getAttemptCount(), requestedIds);
      LOG.info("Creating resource " + resource.toString());
      resources.add(resource);
    }
    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate) throws AmbariException {
    throw new UnsupportedOperationException("Not currently supported.");
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate) throws AmbariException {
    throw new UnsupportedOperationException("Not currently supported.");
  }

  // ----- utility methods -------------------------------------------------

  @Override
  protected Set<PropertyId> getPKPropertyIds() {
    return pkPropertyIds;
  }

  /**
   * Get a component request object from a map of property values.
   *
   * @param properties  the predicate
   *
   * @return the component request object
   */
  private TaskStatusRequest getRequest(Map<PropertyId, Object> properties) {
    String taskId = (String) properties.get(TASK_ID_PROPERTY_ID);
    Long task_id = (taskId == null? null: Long.valueOf(taskId));
    return new TaskStatusRequest(
        Long.valueOf((String) properties.get(TASK_REQUEST_ID_PROPERTY_ID)),
        task_id);
  }
}
