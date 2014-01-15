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
package org.apache.ambari.server.state.scheduler;

import org.apache.ambari.server.controller.RequestScheduleResponse;

/**
 * Request Execution is a type of resource that supports scheduling a request
 * or a group of requests for execution by the ActionManager.
 */
public interface RequestExecution {
  /**
   * Primary key of Request Execution
   * @return
   */
  public Long getId();

  /**
   * Cluster name to which request schedule belongs
   * @return
   */
  public String getClusterName();

  /**
   * Get the batch of requests along with batch settings
   * @return
   */
  public Batch getBatch();

  /**
   * Set batch of requests and batch settings
   */
  public void setBatch(Batch batch);

  /**
   * Get schedule for the execution
   * @return
   */
  public Schedule getSchedule();

  /**
   * Set schedule for the execution
   */
  public void setSchedule(Schedule schedule);

  /**
   * Get @RequestScheduleResponse for this Request Execution
   * @return
   */
  public RequestScheduleResponse convertToResponse();

  /**
   * Persist the Request Execution and schedule
   */
  public void persist();

  /**
   * Refresh entity from DB.
   */
  public void refresh();

  /**
   * Delete Request Schedule entity
   */
  public void delete();

  /**
   * Get status of schedule
   */
  public String getStatus();

  /**
   * Set request execution description
   */
  public void setDescription(String description);

  /**
   * Get description of the request execution
   */
  public String getDescription();

  /**
   * Set status of the schedule
   */
  public void setStatus(Status status);

  /**
   * Set datetime:status of last request that was executed
   */
  public void setLastExecutionStatus(String status);

  /**
   * Set create username
   */
  public void setCreateUser(String username);

  /**
   * Set create username
   */
  public void setUpdateUser(String username);

  /**
   * Get created time
   */
  public String getCreateTime();

  /**
   * Get updated time
   */
  public String getUpdateTime();

  /**
   * Get create user
   */
  public String getCreateUser();

  /**
   * Get update user
   */
  public String getUpdateUser();

  /**
   * Get status of the last batch of requests
   * @return
   */
  public String getLastExecutionStatus();

  /**
   * Get response with request body
   */
  public RequestScheduleResponse convertToResponseWithBody();

  /**
   * Get the request body for a batch request
   */
  public String getRequestBody(Long batchId);

  /**
   * Get batch request with specified order id
   */
  BatchRequest getBatchRequest(long batchId);

  /**
   * Updates batch request data
   * @param batchId order id of batch request
   * @param batchRequestResponse
   * @param statusOnly true if only status should be updated
   */
  void updateBatchRequest(long batchId, BatchRequestResponse batchRequestResponse, boolean statusOnly);

  /**
   * Update status and save RequestExecution
   */
  public void updateStatus(Status status);

  /**
   * Status of the Request execution
   */
  public enum Status {
    SCHEDULED,
    COMPLETED,
    DISABLED
  }
}
