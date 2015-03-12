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

package org.apache.ambari.view.hive.resources.jobs;


import org.apache.ambari.view.hive.client.Cursor;
import org.apache.ambari.view.hive.client.HiveClientException;
import org.apache.ambari.view.hive.client.IConnectionFactory;
import org.apache.ambari.view.hive.resources.jobs.viewJobs.Job;
import org.apache.ambari.view.hive.utils.ServiceFormattedException;
import org.apache.hive.service.cli.thrift.TGetOperationStatusResp;
import org.apache.hive.service.cli.thrift.TOperationHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OperationHandleController {
  private final static Logger LOG =
      LoggerFactory.getLogger(OperationHandleController.class);

  private IConnectionFactory connectionsFabric;
  private TOperationHandle operationHandle;
  private IOperationHandleResourceManager operationHandlesStorage;

  public OperationHandleController(IConnectionFactory connectionsFabric, TOperationHandle storedOperationHandle, IOperationHandleResourceManager operationHandlesStorage) {
    this.connectionsFabric = connectionsFabric;
    this.operationHandle = storedOperationHandle;
    this.operationHandlesStorage = operationHandlesStorage;
  }

  public TOperationHandle getStoredOperationHandle() {
    return operationHandle;
  }

  public void setStoredOperationHandle(TOperationHandle storedOperationHandle) {
    this.operationHandle = storedOperationHandle;
  }

  public String getOperationStatus() throws NoOperationStatusSetException, HiveClientException {
    TGetOperationStatusResp statusResp = connectionsFabric.getHiveConnection().getOperationStatus(operationHandle);
    if (!statusResp.isSetOperationState()) {
      throw new NoOperationStatusSetException("Operation state is not set");
    }

    String status;
    switch (statusResp.getOperationState()) {
      case INITIALIZED_STATE:
        status = Job.JOB_STATE_INITIALIZED;
        break;
      case RUNNING_STATE:
        status = Job.JOB_STATE_RUNNING;
        break;
      case FINISHED_STATE:
        status = Job.JOB_STATE_FINISHED;
        break;
      case CANCELED_STATE:
        status = Job.JOB_STATE_CANCELED;
        break;
      case CLOSED_STATE:
        status = Job.JOB_STATE_CLOSED;
        break;
      case ERROR_STATE:
        status = Job.JOB_STATE_ERROR;
        break;
      case UKNOWN_STATE:
        status = Job.JOB_STATE_UNKNOWN;
        break;
      case PENDING_STATE:
        status = Job.JOB_STATE_PENDING;
        break;
      default:
        throw new NoOperationStatusSetException("Unknown status " + statusResp.getOperationState());
    }
    return status;
  }

  public void cancel() {
    try {
      connectionsFabric.getHiveConnection().cancelOperation(operationHandle);
    } catch (HiveClientException e) {
      throw new ServiceFormattedException("Cancel failed: " + e.toString(), e);
    }
  }

  public void persistHandleForJob(Job job) {
    operationHandlesStorage.putHandleForJob(operationHandle, job);
  }

  public String getLogs() {
    return connectionsFabric.getHiveConnection().getLogs(operationHandle);
  }

  public Cursor getResults() {
    return connectionsFabric.getHiveConnection().getResults(operationHandle);
  }
}
