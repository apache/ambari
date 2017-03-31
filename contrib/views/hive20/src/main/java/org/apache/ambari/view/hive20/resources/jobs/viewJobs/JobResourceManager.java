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

package org.apache.ambari.view.hive20.resources.jobs.viewJobs;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive20.persistence.utils.FilteringStrategy;
import org.apache.ambari.view.hive20.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive20.resources.PersonalCRUDResourceManager;
import org.apache.ambari.view.hive20.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Object that provides CRUD operations for job objects
 */
public class JobResourceManager extends PersonalCRUDResourceManager<Job> {
  private final static Logger LOG =
      LoggerFactory.getLogger(JobResourceManager.class);

  private IJobControllerFactory jobControllerFactory;

  /**
   * Constructor
   * @param context View Context instance
   */
  public JobResourceManager(SharedObjectsFactory sharedObjectsFactory, ViewContext context) {
    super(JobImpl.class, sharedObjectsFactory, context);
    jobControllerFactory = sharedObjectsFactory.getJobControllerFactory();
  }

  @Override
  public Job create(Job object) {
    super.create(object);
    JobController jobController = jobControllerFactory.createControllerForJob(object);

    try {

      jobController.afterCreation();
      saveIfModified(jobController);

    } catch (ServiceFormattedException e) {
      cleanupAfterErrorAndThrowAgain(object, e);
    }

    return object;
  }

  public void saveIfModified(JobController jobController) {
    if (jobController.isModified()) {
      save(jobController.getJobPOJO());
      jobController.clearModified();
    }
  }


  @Override
  public Job read(Object id) throws ItemNotFound {
    return super.read(id);
  }

  @Override
  public List<Job> readAll(FilteringStrategy filteringStrategy) {
    return super.readAll(filteringStrategy);
  }

  @Override
  public void delete(Object resourceId) throws ItemNotFound {
    super.delete(resourceId);
  }

  public JobController readController(Object id) throws ItemNotFound {
    Job job = read(id);
    return jobControllerFactory.createControllerForJob(job);
  }
}
