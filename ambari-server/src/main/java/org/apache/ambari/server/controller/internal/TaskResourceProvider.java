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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.TaskStatusRequest;
import org.apache.ambari.server.controller.TaskStatusResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Resource provider for task resources.
 */
class TaskResourceProvider extends AbstractControllerResourceProvider {

  // ----- Property ID constants ---------------------------------------------

  // Tasks
  protected static final String TASK_CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("Tasks", "cluster_name");
  protected static final String TASK_REQUEST_ID_PROPERTY_ID   = PropertyHelper.getPropertyId("Tasks", "request_id");
  protected static final String TASK_ID_PROPERTY_ID           = PropertyHelper.getPropertyId("Tasks", "id");
  protected static final String TASK_STAGE_ID_PROPERTY_ID     = PropertyHelper.getPropertyId("Tasks", "stage_id");
  protected static final String TASK_HOST_NAME_PROPERTY_ID    = PropertyHelper.getPropertyId("Tasks", "host_name");
  protected static final String TASK_ROLE_PROPERTY_ID         = PropertyHelper.getPropertyId("Tasks", "role");
  protected static final String TASK_COMMAND_PROPERTY_ID      = PropertyHelper.getPropertyId("Tasks", "command");
  protected static final String TASK_STATUS_PROPERTY_ID       = PropertyHelper.getPropertyId("Tasks", "status");
  protected static final String TASK_EXIT_CODE_PROPERTY_ID    = PropertyHelper.getPropertyId("Tasks", "exit_code");
  protected static final String TASK_STDERR_PROPERTY_ID       = PropertyHelper.getPropertyId("Tasks", "stderr");
  protected static final String TASK_STOUT_PROPERTY_ID        = PropertyHelper.getPropertyId("Tasks", "stdout");
  protected static final String TASK_OUTPUTLOG_PROPERTY_ID    = PropertyHelper.getPropertyId("Tasks", "output_log");
  protected static final String TASK_ERRORLOG_PROPERTY_ID     = PropertyHelper.getPropertyId("Tasks", "error_log");
  protected static final String TASK_STRUCT_OUT_PROPERTY_ID   = PropertyHelper.getPropertyId("Tasks", "structured_out");
  protected static final String TASK_START_TIME_PROPERTY_ID   = PropertyHelper.getPropertyId("Tasks", "start_time");
  protected static final String TASK_END_TIME_PROPERTY_ID     = PropertyHelper.getPropertyId("Tasks", "end_time");
  protected static final String TASK_ATTEMPT_CNT_PROPERTY_ID  = PropertyHelper.getPropertyId("Tasks", "attempt_cnt");
  protected static final String TASK_COMMAND_DET_PROPERTY_ID  = PropertyHelper.getPropertyId("Tasks", "command_detail");
  protected static final String TASK_CUST_CMD_NAME_PROPERTY_ID  = PropertyHelper.getPropertyId("Tasks", "custom_command_name");

  private static Set<String> pkPropertyIds =
      new HashSet<String>(Arrays.asList(new String[]{
          TASK_ID_PROPERTY_ID}));

  /**
   * Thread-safe Jackson JSON mapper.
   */
  private static final ObjectMapper mapper = new ObjectMapper();

  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds           the property ids
   * @param keyPropertyIds        the key property ids
   * @param managementController  the management controller
   */
  TaskResourceProvider(Set<String> propertyIds,
                       Map<Resource.Type, String> keyPropertyIds,
                       AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  // ----- ResourceProvider ------------------------------------------------

  @Override
  public RequestStatus createResources(Request request) {
    throw new UnsupportedOperationException("Not currently supported.");
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Map<String, Set<TaskStatusRequest>> requestsMap = new HashMap<String, Set<TaskStatusRequest>>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      String clusterName = (String) propertyMap.get(TASK_CLUSTER_NAME_PROPERTY_ID);

      Set<TaskStatusRequest> requests = requestsMap.get(clusterName);
      if (requests == null) {
        requests = new HashSet<TaskStatusRequest>();
        requestsMap.put(clusterName, requests);
      }
      requests.add(getRequest(propertyMap));
    }

    Set<Resource> resources = null;

    for (Map.Entry<String, Set<TaskStatusRequest>> entry : requestsMap.entrySet()) {

      final Set<TaskStatusRequest> requests = entry.getValue();

      Set<TaskStatusResponse> responses = getResources(new Command<Set<TaskStatusResponse>>() {
        @Override
        public Set<TaskStatusResponse> invoke() throws AmbariException {
          return getManagementController().getTaskStatus(requests);
        }
      });

      if (LOG.isDebugEnabled()) {
        LOG.debug("Printing size of responses " + responses.size());
        for (TaskStatusResponse response : responses) {
          LOG.debug("Printing response from management controller "
              + response.toString());
        }
      }

      resources = new HashSet<Resource>();
      for (TaskStatusResponse response : responses) {
        Resource resource = new ResourceImpl(Resource.Type.Task);

        // !!! shocked this isn't broken.  the key can be null for non-cluster tasks
        if (null != entry.getKey())
          setResourceProperty(resource, TASK_CLUSTER_NAME_PROPERTY_ID, entry.getKey(), requestedIds);  
        
        setResourceProperty(resource, TASK_REQUEST_ID_PROPERTY_ID, response.getRequestId(), requestedIds);
        setResourceProperty(resource, TASK_ID_PROPERTY_ID, response.getTaskId(), requestedIds);
        setResourceProperty(resource, TASK_STAGE_ID_PROPERTY_ID, response.getStageId(), requestedIds);
        setResourceProperty(resource, TASK_HOST_NAME_PROPERTY_ID, response.getHostName(), requestedIds);
        setResourceProperty(resource, TASK_ROLE_PROPERTY_ID, response.getRole(), requestedIds);
        setResourceProperty(resource, TASK_COMMAND_PROPERTY_ID, response.getCommand(), requestedIds);
        setResourceProperty(resource, TASK_STATUS_PROPERTY_ID, response.getStatus(), requestedIds);
        setResourceProperty(resource, TASK_EXIT_CODE_PROPERTY_ID, response.getExitCode(), requestedIds);
        setResourceProperty(resource, TASK_STDERR_PROPERTY_ID, response.getStderr(), requestedIds);
        setResourceProperty(resource, TASK_STOUT_PROPERTY_ID, response.getStdout(), requestedIds);
        setResourceProperty(resource, TASK_OUTPUTLOG_PROPERTY_ID, response.getOutputLog(), requestedIds);
        setResourceProperty(resource, TASK_ERRORLOG_PROPERTY_ID, response.getErrorLog(), requestedIds);
        setResourceProperty(resource, TASK_STRUCT_OUT_PROPERTY_ID, parseStructuredOutput(response.getStructuredOut()), requestedIds);
        setResourceProperty(resource, TASK_START_TIME_PROPERTY_ID, response.getStartTime(), requestedIds);
        setResourceProperty(resource, TASK_END_TIME_PROPERTY_ID, response.getEndTime(), requestedIds);
        setResourceProperty(resource, TASK_ATTEMPT_CNT_PROPERTY_ID, response.getAttemptCount(), requestedIds);

        if (response.getCustomCommandName() != null) {
          setResourceProperty(resource, TASK_CUST_CMD_NAME_PROPERTY_ID, response.getCustomCommandName(), requestedIds);
        }

        if (response.getCommandDetail() == null) {
          setResourceProperty(resource, TASK_COMMAND_DET_PROPERTY_ID,
              String.format("%s %s", response.getRole(), response.getCommand()), requestedIds);
        } else {
          setResourceProperty(resource, TASK_COMMAND_DET_PROPERTY_ID, response.getCommandDetail(), requestedIds);
        }

        resources.add(resource);
      }
    }
    return resources;
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
    if (null == structuredOutput || structuredOutput.isEmpty())
      return null;

    Map<?, ?> result = null;

    try {
      result = mapper.readValue(structuredOutput, Map.class);
    } catch (Exception excepton) {
      LOG.warn("Unable to parse task structured output: {}", structuredOutput);
    }
    return result;
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

  // ----- utility methods -------------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  /**
   * Get a component request object from a map of property values.
   *
   * @param properties  the predicate
   *
   * @return the component request object
   */
  private TaskStatusRequest getRequest(Map<String, Object> properties) {
    String taskId = (String) properties.get(TASK_ID_PROPERTY_ID);
    Long task_id = (taskId == null? null: Long.valueOf(taskId));
    return new TaskStatusRequest(
        Long.valueOf((String) properties.get(TASK_REQUEST_ID_PROPERTY_ID)),
        task_id);
  }
}
