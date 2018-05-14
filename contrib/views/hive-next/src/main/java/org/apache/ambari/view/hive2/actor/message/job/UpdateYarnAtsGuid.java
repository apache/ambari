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

package org.apache.ambari.view.hive2.actor.message.job;

import org.apache.hive.jdbc.HiveStatement;

public class UpdateYarnAtsGuid {
  private final int statementId;
  private final HiveStatement statement;
  private final String jobId;
  public UpdateYarnAtsGuid(int statementId, HiveStatement statement, String jobId) {
    this.statementId = statementId;
    this.statement = statement;
    this.jobId = jobId;
  }

  public int getStatementId() {
    return statementId;
  }

  public HiveStatement getStatement() {
    return statement;
  }

  public String getJobId() {
    return jobId;
  }
}
