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
import org.apache.ambari.view.hive20.actor.message.job.Failure;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

/**
 * Message used to send execution complete message.
 * It may contain a ResultSet if the execution returns a ResultSet.
 */
public class ResultInformation {
  /**
   * Execution id to identify the result correspondence of the result with the request
   */
  private final int id;

  /**
   * If the execution returns a ResultSet then this will refer to the ResultSet
   */
  private final ResultSet resultSet;

  private final Failure failure;

  private final boolean cancelled;
  private DatabaseMetaData databaseMetaData;

  private ResultInformation(int id, ResultSet resultSet, Failure failure, boolean cancelled) {
    this.id = id;
    this.resultSet = resultSet;
    this.failure = failure;
    this.cancelled = cancelled;
  }

  public ResultInformation(int id, ResultSet resultSet) {
    this(id, resultSet, null, false);
  }

  public ResultInformation(int id) {
    this(id, null, null, false);
  }

  public ResultInformation(int id, ResultSet resultSet, DatabaseMetaData metaData, Failure failure, boolean cancelled ) {
    this(id, null, null, false);
    this.databaseMetaData = metaData;
  }

  public ResultInformation(int id, Failure failure) {
    this(id, null, failure, false);
  }

  public ResultInformation(int id, boolean cancelled) {
    this(id, null, null, cancelled);
  }

  public ResultInformation(int id, DatabaseMetaData metaData) {
    this(id, null, metaData, null, false);
  }

  public int getId() {
    return id;
  }

  public Optional<ResultSet> getResultSet() {
    return Optional.fromNullable(resultSet);
  }

  public Optional<Failure> getFailure() {
    return Optional.fromNullable(failure);
  }

  public boolean isCancelled() {
    return cancelled;
  }

  public Optional<DatabaseMetaData> getDatabaseMetaData() {
    return Optional.fromNullable(databaseMetaData);
  }

  public void setDatabaseMetaData(DatabaseMetaData databaseMetaData) {
    this.databaseMetaData = databaseMetaData;
  }
}
