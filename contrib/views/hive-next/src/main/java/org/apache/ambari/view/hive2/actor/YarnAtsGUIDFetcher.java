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

package org.apache.ambari.view.hive2.actor;

import org.apache.ambari.view.hive2.actor.message.HiveMessage;
import org.apache.ambari.view.hive2.actor.message.job.UpdateYarnAtsGuid;
import org.apache.ambari.view.hive2.persistence.Storage;
import org.apache.ambari.view.hive2.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive2.resources.jobs.viewJobs.JobImpl;
import org.apache.hive.jdbc.HiveStatement;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

/**
 * Queries YARN/ATS time to time to fetch the status of the ExecuteJob and updates database
 */
public class YarnAtsGUIDFetcher extends HiveActor {

  private final Storage storage;

  public YarnAtsGUIDFetcher(Storage storage) {
    this.storage = storage;
  }

  @Override
  public void handleMessage(HiveMessage hiveMessage) {
    Object message = hiveMessage.getMessage();
    if(message instanceof UpdateYarnAtsGuid) {
      updateGuid((UpdateYarnAtsGuid) message);
    }
  }

  private void updateGuid(UpdateYarnAtsGuid message) {
    HiveStatement statement = message.getStatement();
    String jobId = message.getJobId();
    String yarnAtsGuid = statement.getYarnATSGuid();

    // If ATS GUID is not yet generated, we will retry after 1 second
    if(yarnAtsGuid == null) {
      getContext().system().scheduler()
        .scheduleOnce(Duration.create(1, TimeUnit.SECONDS), getSelf(), message, getContext().dispatcher(), null);
    } else {
      try {
        JobImpl job = storage.load(JobImpl.class, jobId);
        job.setGuid(yarnAtsGuid);
        storage.store(JobImpl.class, job);
      } catch (ItemNotFound itemNotFound) {
        // Cannot do anything if the job is not present
      }
    }
  }
}
