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

import org.apache.ambari.view.hive.persistence.IStorageFactory;
import org.apache.ambari.view.hive.persistence.utils.FilteringStrategy;
import org.apache.ambari.view.hive.persistence.utils.Indexed;
import org.apache.ambari.view.hive.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive.resources.SharedCRUDResourceManager;
import org.apache.ambari.view.hive.resources.jobs.viewJobs.Job;
import org.apache.ambari.view.hive.utils.ServiceFormattedException;
import org.apache.hive.service.cli.thrift.TOperationHandle;

import java.util.List;

public class OperationHandleResourceManager extends SharedCRUDResourceManager<StoredOperationHandle>
    implements IOperationHandleResourceManager {
  /**
   * Constructor
   */
  public OperationHandleResourceManager(IStorageFactory storageFabric) {
    super(StoredOperationHandle.class, storageFabric);
  }

  @Override
  public List<StoredOperationHandle> readJobRelatedHandles(final Job job) {
    return storageFactory.getStorage().loadAll(StoredOperationHandle.class, new FilteringStrategy() {
      @Override
      public boolean isConform(Indexed item) {
        StoredOperationHandle handle = (StoredOperationHandle) item;
        return (handle.getJobId() != null && handle.getJobId().equals(job.getId()));
      }

      @Override
      public String whereStatement() {
        return "jobId = '" + job.getId() + "'";
      }
    });
  }

  @Override
  public StoredOperationHandle getHandleForJob(Job job) throws ItemNotFound {
    List<StoredOperationHandle> jobRelatedHandles = readJobRelatedHandles(job);
    if (jobRelatedHandles.size() == 0)
      throw new ItemNotFound();
    return jobRelatedHandles.get(0);
  }

  @Override
  public List<Job> getHandleRelatedJobs(final StoredOperationHandle operationHandle) {
    return storageFactory.getStorage().loadAll(Job.class, new FilteringStrategy() {
      @Override
      public boolean isConform(Indexed item) {
        Job job = (Job) item;
        return (job.getId() != null && job.getId().equals(operationHandle.getJobId()));
      }

      @Override
      public String whereStatement() {
        return "id = '" + operationHandle.getJobId() + "'";
      }
    });
  }

  @Override
  public Job getJobByHandle(StoredOperationHandle handle) throws ItemNotFound {
    List<Job> handleRelatedJobs = getHandleRelatedJobs(handle);
    if (handleRelatedJobs.size() == 0)
      throw new ItemNotFound();
    return handleRelatedJobs.get(0);
  }

  @Override
  public void putHandleForJob(TOperationHandle h, Job job) {
    StoredOperationHandle handle = StoredOperationHandle.buildFromTOperationHandle(h);
    handle.setJobId(job.getId());

    List<StoredOperationHandle> jobRelatedHandles = readJobRelatedHandles(job);
    if (jobRelatedHandles.size() > 0) {
      handle.setId(jobRelatedHandles.get(0).getId());  // update existing
      try {
        update(handle, jobRelatedHandles.get(0).getId());
      } catch (ItemNotFound itemNotFound) {
        throw new ServiceFormattedException("E050 Error when updating operation handle: " + itemNotFound.toString(), itemNotFound);
      }
    } else {
      create(handle);
    }
  }

  @Override
  public boolean containsHandleForJob(Job job) {
    List<StoredOperationHandle> jobRelatedHandles = readJobRelatedHandles(job);
    return jobRelatedHandles.size() > 0;
  }
}
