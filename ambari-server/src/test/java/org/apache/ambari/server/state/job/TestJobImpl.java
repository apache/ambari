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

package org.apache.ambari.server.state.job;

import org.apache.ambari.server.state.job.Job;
import org.apache.ambari.server.state.job.JobCompletedEvent;
import org.apache.ambari.server.state.job.JobEvent;
import org.apache.ambari.server.state.job.JobFailedEvent;
import org.apache.ambari.server.state.job.JobId;
import org.apache.ambari.server.state.job.JobImpl;
import org.apache.ambari.server.state.job.JobProgressUpdateEvent;
import org.apache.ambari.server.state.job.JobState;
import org.apache.ambari.server.state.job.JobType;
import org.apache.ambari.server.state.job.NewJobEvent;
import org.junit.Assert;
import org.junit.Test;

public class TestJobImpl {

  private Job createNewJob(long id, String jobName, long startTime) {
    JobId jId = new JobId(id, new JobType(jobName));
    Job job = new JobImpl(jId, startTime);
    return job;
  }

  private Job getRunningJob(long id, String jobName, long startTime)
      throws Exception {
    Job job = createNewJob(id, jobName, startTime);
    verifyProgressUpdate(job, ++startTime);
    return job;
  }

  private Job getCompletedJob(long id, String jobName, long startTime,
      boolean failedJob) throws Exception {
    Job job = getRunningJob(1, "JobNameFoo", startTime);
    completeJob(job, failedJob, ++startTime);
    return job;
  }

  private void verifyNewJob(Job job, long startTime) {
    Assert.assertEquals(JobState.INIT, job.getState());
    Assert.assertEquals(startTime, job.getStartTime());
  }


  @Test
  public void testNewJob() {
    long currentTime = System.currentTimeMillis();
    Job job = createNewJob(1, "JobNameFoo", currentTime);
    verifyNewJob(job, currentTime);
  }

  private void verifyProgressUpdate(Job job, long updateTime)
      throws Exception {
    JobProgressUpdateEvent e = new JobProgressUpdateEvent(job.getId(),
        updateTime);
    job.handleEvent(e);
    Assert.assertEquals(JobState.IN_PROGRESS, job.getState());
    Assert.assertEquals(updateTime, job.getLastUpdateTime());
  }


  @Test
  public void testJobProgressUpdates() throws Exception {
    long currentTime = 1;
    Job job = createNewJob(1, "JobNameFoo", currentTime);
    verifyNewJob(job, currentTime);

    verifyProgressUpdate(job, ++currentTime);
    verifyProgressUpdate(job, ++currentTime);
    verifyProgressUpdate(job, ++currentTime);

  }

  private void completeJob(Job job, boolean failJob, long endTime)
      throws Exception {
    JobEvent e = null;
    JobState endState = null;
    if (failJob) {
      e = new JobFailedEvent(job.getId(), endTime);
      endState = JobState.FAILED;
    } else {
      e = new JobCompletedEvent(job.getId(), endTime);
      endState = JobState.COMPLETED;
    }
    job.handleEvent(e);
    Assert.assertEquals(endState, job.getState());
    Assert.assertEquals(endTime, job.getLastUpdateTime());
    Assert.assertEquals(endTime, job.getCompletionTime());
  }


  @Test
  public void testJobSuccessfulCompletion() throws Exception {
    long currentTime = 1;
    Job job = getRunningJob(1, "JobNameFoo", currentTime);
    completeJob(job, false, ++currentTime);
  }

  @Test
  public void testJobFailedCompletion() throws Exception {
    long currentTime = 1;
    Job job = getRunningJob(1, "JobNameFoo", currentTime);
    completeJob(job, true, ++currentTime);
  }

  @Test
  public void completeNewJob() throws Exception {
    long currentTime = 1;
    Job job = createNewJob(1, "JobNameFoo", currentTime);
    verifyNewJob(job, currentTime);
    completeJob(job, false, ++currentTime);
  }

  @Test
  public void failNewJob() throws Exception {
    long currentTime = 1;
    Job job = createNewJob(1, "JobNameFoo", currentTime);
    verifyNewJob(job, currentTime);
    completeJob(job, true, ++currentTime);
  }

  @Test
  public void reInitCompletedJob() throws Exception {
    Job job = getCompletedJob(1, "JobNameFoo", 1, false);
    JobId jId = new JobId(2, new JobType("JobNameFoo"));
    NewJobEvent e = new NewJobEvent(jId, 100);
    job.handleEvent(e);
    Assert.assertEquals(JobState.INIT, job.getState());
    Assert.assertEquals(100, job.getStartTime());
    Assert.assertEquals(-1, job.getLastUpdateTime());
    Assert.assertEquals(-1, job.getCompletionTime());
    Assert.assertEquals(2, job.getId().jobId);
  }


}
