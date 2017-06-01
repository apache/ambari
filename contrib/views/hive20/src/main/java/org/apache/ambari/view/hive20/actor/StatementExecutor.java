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

package org.apache.ambari.view.hive20.actor;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.google.common.base.Optional;
import org.apache.ambari.view.hive20.ConnectionDelegate;
import org.apache.ambari.view.hive20.actor.message.GetColumnMetadataJob;
import org.apache.ambari.view.hive20.actor.message.GetDatabaseMetadataJob;
import org.apache.ambari.view.hive20.actor.message.HiveMessage;
import org.apache.ambari.view.hive20.actor.message.ResultInformation;
import org.apache.ambari.view.hive20.actor.message.RunStatement;
import org.apache.ambari.view.hive20.actor.message.StartLogAggregation;
import org.apache.ambari.view.hive20.actor.message.job.Failure;
import org.apache.ambari.view.hive20.actor.message.job.UpdateYarnAtsGuid;
import org.apache.ambari.view.hive20.persistence.Storage;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.hive.jdbc.HiveConnection;
import org.apache.hive.jdbc.HiveStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Executes a single statement and returns the ResultSet if the statements generates ResultSet.
 * Also, starts logAggregation and YarnAtsGuidFetcher if they are required.
 */
public class StatementExecutor extends HiveActor {

  private final Logger LOG = LoggerFactory.getLogger(getClass());

  private final HdfsApi hdfsApi;
  private final HiveConnection connection;
  protected final Storage storage;
  private final ConnectionDelegate connectionDelegate;
  private ActorRef logAggregator;
  private ActorRef guidFetcher;


  public StatementExecutor(HdfsApi hdfsApi, Storage storage, HiveConnection connection, ConnectionDelegate connectionDelegate) {
    this.hdfsApi = hdfsApi;
    this.storage = storage;
    this.connection = connection;
    this.connectionDelegate = connectionDelegate;
  }

  @Override
  public void handleMessage(HiveMessage hiveMessage) {
    Object message = hiveMessage.getMessage();
    if (message instanceof RunStatement) {
      runStatement((RunStatement) message);
    } else if (message instanceof GetColumnMetadataJob) {
      getColumnMetaData((GetColumnMetadataJob) message);
    }else if (message instanceof GetDatabaseMetadataJob) {
      getDatabaseMetaData((GetDatabaseMetadataJob) message);
    }
  }

  private void runStatement(RunStatement message) {
    try {
      HiveStatement statement = connectionDelegate.createStatement(connection);
      if (message.shouldStartLogAggregation()) {
        startLogAggregation(statement, message.getStatement(), message.getLogFile().get());
      }

      if (message.shouldStartGUIDFetch() && message.getJobId().isPresent()) {
        startGUIDFetch(message.getId(), statement, message.getJobId().get());
      }
      LOG.info("Statement executor is executing statement: {}, Statement id: {}, JobId: {}", message.getStatement(), message.getId(), message.getJobId().or("SYNC JOB"));
      Optional<ResultSet> resultSetOptional = connectionDelegate.execute(message.getStatement());
      LOG.info("Finished executing statement: {}, Statement id: {}, JobId: {}", message.getStatement(), message.getId(), message.getJobId().or("SYNC JOB"));

      if (resultSetOptional.isPresent()) {
        sender().tell(new ResultInformation(message.getId(), resultSetOptional.get()), self());
      } else {
        sender().tell(new ResultInformation(message.getId()), self());
      }
    } catch (SQLException e) {
      LOG.error("Failed to execute statement: {}. {}", message.getStatement(), e);
      sender().tell(new ResultInformation(message.getId(), new Failure("Failed to execute statement: " + message.getStatement(), e)), self());
    } finally {
      stopGUIDFetch();
    }
  }

  private void startGUIDFetch(int statementId, HiveStatement statement, String jobId) {
    if (guidFetcher == null) {
      guidFetcher = getContext().actorOf(Props.create(YarnAtsGUIDFetcher.class, sender())
        .withDispatcher("akka.actor.misc-dispatcher"), "YarnAtsGUIDFetcher:" + UUID.randomUUID().toString());
    }
    LOG.info("Fetching guid for Job Id: {}", jobId);
    guidFetcher.tell(new UpdateYarnAtsGuid(statementId, statement, jobId), self());
  }

  private void stopGUIDFetch() {
    if (guidFetcher != null) {
      getContext().stop(guidFetcher);
    }
    guidFetcher = null;
  }

  private void startLogAggregation(HiveStatement statement, String sqlStatement, String logFile) {
    if (logAggregator == null) {
      logAggregator = getContext().actorOf(
        Props.create(LogAggregator.class, hdfsApi, logFile)
          .withDispatcher("akka.actor.misc-dispatcher"), "LogAggregator:" + UUID.randomUUID().toString());
    }
    LOG.info("Fetching query logs for statement: {}", sqlStatement);
    logAggregator.tell(new StartLogAggregation(sqlStatement, statement), getSelf());
  }

  private void stopLogAggregation() {
    if (logAggregator != null) {
      getContext().stop(logAggregator);
    }
    logAggregator = null;
  }

  @Override
  public void postStop() throws Exception {
    stopLogAggregation();
  }


  private void getColumnMetaData(GetColumnMetadataJob message) {
    try {
      ResultSet resultSet = connectionDelegate.getColumnMetadata(connection, message);
      sender().tell(new ResultInformation(-1, resultSet), self());
    } catch (SQLException e) {
      LOG.error("Failed to get column metadata for databasePattern: {}, tablePattern: {}, ColumnPattern {}. {}",
        message.getSchemaPattern(), message.getTablePattern(), message.getColumnPattern(), e);
      sender().tell(new ResultInformation(-1,
        new Failure("Failed to get column metadata for databasePattern: " + message.getSchemaPattern() +
          ", tablePattern: " + message.getTablePattern() + ", ColumnPattern: " + message.getColumnPattern(), e)), self());
    }
  }

  private void getDatabaseMetaData(GetDatabaseMetadataJob message) {
    try {
      DatabaseMetaData metaData = connectionDelegate.getDatabaseMetadata(connection);
      sender().tell(new ResultInformation(-1, metaData), self());
    } catch (SQLException e) {
      LOG.error("Failed to get database metadata.", e);
      sender().tell(new ResultInformation(-1,
        new Failure("Failed to get database metadata.", e)), self());
    }
  }
}
