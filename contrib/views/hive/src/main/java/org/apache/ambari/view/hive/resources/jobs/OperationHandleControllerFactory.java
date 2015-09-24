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
import org.apache.ambari.view.hive.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive.resources.jobs.viewJobs.Job;
import org.apache.ambari.view.hive.utils.SharedObjectsFactory;

public class OperationHandleControllerFactory {
  private IOperationHandleResourceManager operationHandlesStorage;
  private ViewContext context;

  public OperationHandleControllerFactory(ViewContext context, SharedObjectsFactory storageFactory) {
    this.context = context;
    this.operationHandlesStorage = new OperationHandleResourceManager(storageFactory);
  }

  public OperationHandleController createControllerForHandle(StoredOperationHandle storedOperationHandle) {
    return new OperationHandleController(context, storedOperationHandle, operationHandlesStorage);
  }

  public OperationHandleController getHandleForJob(Job job) throws ItemNotFound {
    StoredOperationHandle handle = operationHandlesStorage.getHandleForJob(job);
    return createControllerForHandle(handle);
  }
}
