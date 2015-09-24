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


import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive.client.Cursor;
import org.apache.ambari.view.hive.client.HiveClientException;
import org.apache.ambari.view.hive.client.UserLocalConnection;
import org.apache.ambari.view.hive.resources.jobs.viewJobs.Job;
import org.apache.ambari.view.hive.utils.HiveClientFormattedException;
import org.apache.hive.service.cli.thrift.TGetOperationStatusResp;
import org.apache.hive.service.cli.thrift.TOperationHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OperationHandleController {
  private final static Logger LOG =
      LoggerFactory.getLogger(OperationHandleController.class);
  private final TOperationHandle operationHandle;
  private ViewContext context;
  private final StoredOperationHandle storedOperationHandle;
  private final IOperationHandleResourceManager operationHandlesStorage;

  protected UserLocalConnection connectionLocal = new UserLocalConnection();

  public OperationHandleController(ViewContext context, StoredOperationHandle storedOperationHandle,
                                   IOperationHandleResourceManager operationHandlesStorage) {
    this.context = context;
    this.storedOperationHandle = storedOperationHandle;
    this.operationHandle = storedOperationHandle.toTOperationHandle();
    this.operationHandlesStorage = operationHandlesStorage;
  }

  public StoredOperationHandle getStoredOperationHandle() {
    return storedOperationHandle;
  }

  public OperationStatus getOperationStatus() throws NoOperationStatusSetException, HiveClientException {
    TGetOperationStatusResp statusResp = connectionLocal.get(context).getOperationStatus(operationHandle);

    if (!statusResp.isSetOperationState()) {
      throw new NoOperationStatusSetException();
    }

    OperationStatus opStatus = new OperationStatus();
    opStatus.sqlState = statusResp.getSqlState();
    opStatus.message = statusResp.getErrorMessage();

    switch (statusResp.getOperationState()) {
      case INITIALIZED_STATE:
        opStatus.status = Job.JOB_STATE_INITIALIZED;
        break;
      case RUNNING_STATE:
        opStatus.status = Job.JOB_STATE_RUNNING;
        break;
      case FINISHED_STATE:
        opStatus.status = Job.JOB_STATE_FINISHED;
        break;
      case CANCELED_STATE:
        opStatus.status = Job.JOB_STATE_CANCELED;
        break;
      case CLOSED_STATE:
        opStatus.status = Job.JOB_STATE_CLOSED;
        break;
      case ERROR_STATE:
        opStatus.status = Job.JOB_STATE_ERROR;
        break;
      case UKNOWN_STATE:
        opStatus.status = Job.JOB_STATE_UNKNOWN;
        break;
      case PENDING_STATE:
        opStatus.status = Job.JOB_STATE_PENDING;
        break;
      default:
        throw new NoOperationStatusSetException();
    }

    return opStatus;
  }

  public void cancel() {
    try {
      connectionLocal.get(context).cancelOperation(operationHandle);
    } catch (HiveClientException e) {
      throw new HiveClientFormattedException(e);
    }
  }

  public void persistHandleForJob(Job job) {
    operationHandlesStorage.putHandleForJob(operationHandle, job);
  }

  public String getLogs() {
    String logs;
    try {
      logs = connectionLocal.get(context).getLogs(operationHandle);
    } catch (HiveClientFormattedException ex) {
      logs = "";
      LOG.info(String.format("Logs are not available yet for job #%s [%s]\n%s",
          storedOperationHandle.getJobId(), storedOperationHandle.getGuid(), ex.toString()));
    }
    return logs;
  }

  public Cursor getResults() {
    return connectionLocal.get(context).getResults(operationHandle);
  }

  public boolean hasResults() {
    return operationHandle.isHasResultSet();
  }

  public static class OperationStatus {
    public String status;
    public String sqlState;
    public String message;
  }
}
