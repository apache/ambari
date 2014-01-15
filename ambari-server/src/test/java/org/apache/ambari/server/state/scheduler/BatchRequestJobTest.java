/*
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

import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.scheduler.ExecutionScheduleManager;
import org.easymock.Capture;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;

public class BatchRequestJobTest {


  @Test
  public void testDoWork() throws Exception {
    ExecutionScheduleManager scheduleManagerMock = createMock(ExecutionScheduleManager.class);
    BatchRequestJob batchRequestJob = new BatchRequestJob(scheduleManagerMock, 100L);
    String clusterName = "mycluster";
    long requestId = 11L;
    long executionId = 31L;
    long batchId = 1L;

    Map<String, Object> properties = new HashMap<String, Object>();
    properties.put(BatchRequestJob.BATCH_REQUEST_EXECUTION_ID_KEY, executionId);
    properties.put(BatchRequestJob.BATCH_REQUEST_BATCH_ID_KEY, batchId);
    properties.put(BatchRequestJob.BATCH_REQUEST_CLUSTER_NAME_KEY, clusterName);


    BatchRequestResponse pendingResponse = new BatchRequestResponse();
    pendingResponse.setStatus(HostRoleStatus.PENDING.toString());
    BatchRequestResponse inProgressResponse = new BatchRequestResponse();
    inProgressResponse.setStatus(HostRoleStatus.IN_PROGRESS.toString());
    BatchRequestResponse completedResponse = new BatchRequestResponse();
    completedResponse.setStatus(HostRoleStatus.COMPLETED.toString());

    Capture<Long> executionIdCapture = new Capture<Long>();
    Capture<Long> batchIdCapture = new Capture<Long>();
    Capture<String> clusterNameCapture = new Capture<String>();


    expect(scheduleManagerMock.executeBatchRequest(captureLong(executionIdCapture), captureLong(batchIdCapture),
      capture(clusterNameCapture))).andReturn(requestId);

    expect(scheduleManagerMock.getBatchRequestResponse(requestId, clusterName)).
      andReturn(pendingResponse).times(2);
    expect(scheduleManagerMock.getBatchRequestResponse(requestId, clusterName)).
      andReturn(inProgressResponse).times(4);
    expect(scheduleManagerMock.getBatchRequestResponse(requestId, clusterName)).
      andReturn(completedResponse).once();

    scheduleManagerMock.updateBatchRequest(eq(executionId), eq(batchId), eq(clusterName),
        anyObject(BatchRequestResponse.class), eq(true));
    expectLastCall().anyTimes();

    replay(scheduleManagerMock);

    batchRequestJob.doWork(properties);

    verify(scheduleManagerMock);

    Assert.assertEquals(executionId, executionIdCapture.getValue().longValue());
    Assert.assertEquals(batchId, batchIdCapture.getValue().longValue());
    Assert.assertEquals(clusterName, clusterNameCapture.getValue());


  }
}
