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

package org.apache.ambari.view.hive20.client;

import com.google.common.base.Optional;
import org.apache.ambari.view.hive20.actor.message.SQLStatementJob;
import org.apache.ambari.view.hive20.actor.message.job.Failure;
import org.apache.ambari.view.hive20.resources.jobs.viewJobs.Job;

public interface AsyncJobRunner {

  void submitJob(ConnectionConfig connectionConfig, SQLStatementJob asyncJob, Job job);

  void cancelJob(String jobId, String username);

  Optional<NonPersistentCursor> getCursor(String jobId, String username);

  Optional<NonPersistentCursor> resetAndGetCursor(String jobId, String username);

  Optional<Failure> getError(String jobId, String username);

}
