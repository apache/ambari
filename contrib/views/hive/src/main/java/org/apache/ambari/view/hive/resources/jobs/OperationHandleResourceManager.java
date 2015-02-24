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
import org.apache.ambari.view.hive.persistence.utils.FilteringStrategy;
import org.apache.ambari.view.hive.persistence.utils.Indexed;
import org.apache.ambari.view.hive.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive.resources.SharedCRUDResourceManager;
import org.apache.ambari.view.hive.utils.ServiceFormattedException;
import org.apache.hive.service.cli.thrift.TOperationHandle;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;

public class OperationHandleResourceManager extends SharedCRUDResourceManager<StoredOperationHandle> {
  /**
   * Constructor
   *
   * @param context       View Context instance
   */
  public OperationHandleResourceManager(ViewContext context) {
    super(StoredOperationHandle.class, context);
  }

  public List<StoredOperationHandle> readJobRelatedHandles(final Job job) {
    try {
      return getStorage().loadWhere(StoredOperationHandle.class, "jobId = " + job.getId());
    } catch (NotImplementedException e) {
      // fallback to filtering strategy
      return getStorage().loadAll(StoredOperationHandle.class, new FilteringStrategy() {
        @Override
        public boolean isConform(Indexed item) {
          StoredOperationHandle handle = (StoredOperationHandle) item;
          return (handle.getJobId() != null && handle.getJobId().equals(job.getId()));
        }
      });
    }
  }

  public void putHandleForJob(TOperationHandle h, Job job) {
    StoredOperationHandle handle = StoredOperationHandle.buildFromTOperationHandle(h);
    handle.setJobId(job.getId());

    List<StoredOperationHandle> jobRelatedHandles = readJobRelatedHandles(job);
    if (jobRelatedHandles.size() > 0) {
      handle.setId(jobRelatedHandles.get(0).getId());  // update existing
      try {
        update(handle, jobRelatedHandles.get(0).getId());
      } catch (ItemNotFound itemNotFound) {
        throw new ServiceFormattedException("Error when updating operation handle: " + itemNotFound.toString(), itemNotFound);
      }
    } else {
      create(handle);
    }
  }

  public boolean containsHandleForJob(Job job) {
    List<StoredOperationHandle> jobRelatedHandles = readJobRelatedHandles(job);
    return jobRelatedHandles.size() > 0;
  }

  public TOperationHandle getHandleForJob(Job job) throws ItemNotFound {
    List<StoredOperationHandle> jobRelatedHandles = readJobRelatedHandles(job);
    if (jobRelatedHandles.size() == 0)
      throw new ItemNotFound();
    return jobRelatedHandles.get(0).toTOperationHandle();
  }
}
