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

package org.apache.ambari.view.hive20.actor.message;

import com.google.common.base.Optional;

/**
 * Message sent by JdbcConnector to StatementExecutor to run a statement
 */
public class RunStatement {
  /**
   * This is the execution id meant to identify the executing statement sequence
   */
  private final int id;
  private final String statement;
  private final String logFile;
  private final String jobId;
  private final boolean startLogAggregation;
  private final boolean startGUIDFetch;

  public RunStatement(int id, String statement, String jobId, boolean startLogAggregation, String logFile, boolean startGUIDFetch) {
    this.id = id;
    this.statement = statement;
    this.jobId = jobId;
    this.logFile = logFile;
    this.startLogAggregation = startLogAggregation;
    this.startGUIDFetch = startGUIDFetch;
  }

  public RunStatement(int id, String statement) {
    this(id, statement, null, false, null, false);
  }

  public int getId() {
    return id;
  }

  public String getStatement() {
    return statement;
  }

  public Optional<String> getLogFile() {
    return Optional.fromNullable(logFile);
  }

  public boolean shouldStartLogAggregation() {
    return startLogAggregation;
  }

  public boolean shouldStartGUIDFetch() {
    return startGUIDFetch;
  }

  public Optional<String> getJobId() {
    return Optional.fromNullable(jobId);
  }
}
