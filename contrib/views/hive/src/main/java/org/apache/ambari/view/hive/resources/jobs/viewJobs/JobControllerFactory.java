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

package org.apache.ambari.view.hive.resources.jobs.viewJobs;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive.utils.SharedObjectsFactory;

public class JobControllerFactory implements IJobControllerFactory {
  private SharedObjectsFactory sharedObjectsFactory;
  private ViewContext context;

  public JobControllerFactory(ViewContext context, SharedObjectsFactory sharedObjectsFactory) {
    this.sharedObjectsFactory = sharedObjectsFactory;
    this.context = context;
  }

  @Override
  public JobController createControllerForJob(Job job) {
    return new JobControllerImpl(context, job,
        sharedObjectsFactory.getOperationHandleControllerFactory(),
        sharedObjectsFactory.getSavedQueryResourceManager(),
        sharedObjectsFactory.getATSParser(),
        sharedObjectsFactory.getHdfsApi());
  }
}
