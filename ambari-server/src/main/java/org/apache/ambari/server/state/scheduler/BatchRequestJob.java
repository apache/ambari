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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.scheduler.AbstractLinearExecutionJob;
import org.apache.ambari.server.scheduler.ExecutionScheduleManager;

import java.util.Map;

public class BatchRequestJob extends AbstractLinearExecutionJob {
  public static final String BATCH_REQUEST_EXECUTION_ID_KEY =
    "BatchRequestJob.ExecutionId";
  public static final String BATCH_REQUEST_BATCH_ID_KEY =
    "BatchRequestJob.BatchId";

  public BatchRequestJob(ExecutionScheduleManager executionScheduleManager) {
    super(executionScheduleManager);
  }

  @Override
  protected void doWork(Map<String, Object> properties) throws AmbariException {

    String executionId = properties.get(BATCH_REQUEST_EXECUTION_ID_KEY) != null ?
      (String) properties.get(BATCH_REQUEST_EXECUTION_ID_KEY) : null;
    String batchId = properties.get(BATCH_REQUEST_BATCH_ID_KEY) != null ?
      (String) properties.get(BATCH_REQUEST_BATCH_ID_KEY) : null;


    if (executionId == null || batchId == null) {
      throw new AmbariException("Unable to retrieve persisted batch request"
        + ", execution_id = " + executionId
        + ", batch_id = " + batchId);
    }

    Long requestId = executionScheduleManager.executeBatchRequest
      (Long.parseLong(executionId), Long.parseLong(batchId));

    if (requestId != null) {
      // Wait on request completion

      BatchRequestResponse batchRequestResponse =
        executionScheduleManager.getBatchRequestResponse(requestId);
    }
  }
}
