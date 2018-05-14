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

package org.apache.ambari.view.hive2.resources.jobs;

import org.apache.ambari.view.hive2.resources.jobs.atsJobs.TezVertexId;
import org.apache.ambari.view.hive2.resources.jobs.rm.RMParser;
import org.apache.ambari.view.hive2.resources.jobs.viewJobs.Job;
import org.apache.ambari.view.hive2.utils.ServiceFormattedException;
import org.apache.ambari.view.hive2.utils.SharedObjectsFactory;

import java.util.List;

public class ProgressRetriever {
  private final Progress progress;
  private final Job job;
  private final SharedObjectsFactory sharedObjects;

  public ProgressRetriever(Job job, SharedObjectsFactory sharedObjects) {
    this.job = job;
    this.sharedObjects = sharedObjects;

    this.progress = new Progress();
  }

  public Progress getProgress() {
    jobCheck();

    progress.dagProgress = sharedObjects.getRMParser().getDAGProgress(
        job.getApplicationId(), job.getDagId());

    List<TezVertexId> vertices = sharedObjects.getATSParser().getVerticesForDAGId(job.getDagId());
    progress.vertexProgresses = sharedObjects.getRMParser().getDAGVerticesProgress(job.getApplicationId(), job.getDagId(), vertices);

    return progress;
  }

  public void jobCheck() {
    if (job.getApplicationId() == null || job.getApplicationId().isEmpty()) {
      throw new ServiceFormattedException("E070 ApplicationId is not defined yet");
    }
    if (job.getDagId() == null || job.getDagId().isEmpty()) {
      throw new ServiceFormattedException("E080 DagID is not defined yet");
    }
  }

  public static class Progress {
    public Double dagProgress;
    public List<RMParser.VertexProgress> vertexProgresses;
  }
}
