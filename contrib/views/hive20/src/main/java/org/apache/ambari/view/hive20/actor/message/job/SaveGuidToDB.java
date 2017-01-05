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

package org.apache.ambari.view.hive20.actor.message.job;

/**
 * Message to ask JdbcConnector for job to update the GUID for the current statement in the database for the job.
 */
public class SaveGuidToDB {
  private final int statementId;
  private final String guid;
  private final String jobId;

  public SaveGuidToDB(int statementId, String guid, String jobId) {
    this.statementId = statementId;
    this.guid = guid;
    this.jobId = jobId;
  }

  public int getStatementId() {
    return statementId;
  }

  public String getGuid() {
    return guid;
  }

  public String getJobId() {
    return jobId;
  }
}
