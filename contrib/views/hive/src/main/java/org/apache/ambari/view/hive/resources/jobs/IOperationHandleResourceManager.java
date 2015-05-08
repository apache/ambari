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

import org.apache.ambari.view.hive.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive.resources.IResourceManager;
import org.apache.ambari.view.hive.resources.jobs.viewJobs.Job;
import org.apache.hive.service.cli.thrift.TOperationHandle;

import java.util.List;

public interface IOperationHandleResourceManager extends IResourceManager<StoredOperationHandle> {
  List<StoredOperationHandle> readJobRelatedHandles(Job job);

  List<Job> getHandleRelatedJobs(StoredOperationHandle operationHandle);

  Job getJobByHandle(StoredOperationHandle handle) throws ItemNotFound;

  void putHandleForJob(TOperationHandle h, Job job);

  boolean containsHandleForJob(Job job);

  StoredOperationHandle getHandleForJob(Job job) throws ItemNotFound;
}
