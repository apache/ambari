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

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import com.google.common.base.Optional;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive2.actor.message.RegisterActor;
import org.apache.ambari.view.hive2.persistence.Storage;
import org.apache.ambari.view.hive2.ConnectionDelegate;
import org.apache.ambari.view.hive2.actor.message.GetColumnMetadataJob;
import org.apache.ambari.view.hive2.actor.message.HiveMessage;
import org.apache.ambari.view.hive2.actor.message.SyncJob;
import org.apache.ambari.view.hive2.actor.message.job.ExecutionFailed;
import org.apache.ambari.view.hive2.actor.message.job.NoResult;
import org.apache.ambari.view.hive2.actor.message.job.ResultSetHolder;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.hive.jdbc.HiveConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SyncJdbcConnector extends JdbcConnector {

  private final Logger LOG = LoggerFactory.getLogger(getClass());
  private ActorRef resultSetActor = null;

  public SyncJdbcConnector(ViewContext viewContext, HdfsApi hdfsApi, ActorSystem system, ActorRef parent,ActorRef deathWatch, ConnectionDelegate connectionDelegate, Storage storage) {
    super(viewContext, hdfsApi, system, parent,deathWatch, connectionDelegate, storage);
  }

  @Override
  protected void handleJobMessage(HiveMessage message) {
    Object job = message.getMessage();
    if(job instanceof SyncJob) {
      execute((SyncJob) job);
    } else if (job instanceof GetColumnMetadataJob) {
      getColumnMetaData((GetColumnMetadataJob) job);
    }
  }

  @Override
  protected boolean isAsync() {
    return false;
  }

  @Override
  protected void cleanUpChildren() {
    if(resultSetActor != null && !resultSetActor.isTerminated()) {
      LOG.debug("Sending poison pill to log aggregator");
      resultSetActor.tell(PoisonPill.getInstance(), self());
    }
  }

  @Override
  protected void notifyFailure() {
    sender().tell(new ExecutionFailed("Cannot connect to hive"), ActorRef.noSender());
  }

  protected void execute(final SyncJob job) {
    this.executing = true;
    executeJob(new Operation<SyncJob>() {
      @Override
      SyncJob getJob() {
        return job;
      }

      @Override
      Optional<ResultSet> call(HiveConnection connection) throws SQLException {
        return connectionDelegate.executeSync(connection, job);
      }

      @Override
      String notConnectedErrorMessage() {
        return "Cannot execute sync job for user: " + job.getUsername() + ". Not connected to Hive";
      }

      @Override
      String executionFailedErrorMessage() {
        return "Failed to execute Jdbc Statement";
      }
    });
  }


  private void getColumnMetaData(final GetColumnMetadataJob job) {
    executeJob(new Operation<GetColumnMetadataJob>() {

      @Override
      GetColumnMetadataJob getJob() {
        return job;
      }

      @Override
      Optional<ResultSet> call(HiveConnection connection) throws SQLException {
        return connectionDelegate.getColumnMetadata(connection, job);
      }

      @Override
      String notConnectedErrorMessage() {
        return String.format("Cannot get column metadata for user: %s, schema: %s, table: %s, column: %s" +
            ". Not connected to Hive", job.getUsername(), job.getSchemaPattern(), job.getTablePattern(),
          job.getColumnPattern());
      }

      @Override
      String executionFailedErrorMessage() {
        return "Failed to execute Jdbc Statement";
      }
    });
  }

  private void executeJob(Operation operation) {
    ActorRef sender = this.getSender();
    String errorMessage = operation.notConnectedErrorMessage();
    if (connectable == null) {
      sender.tell(new ExecutionFailed(errorMessage), ActorRef.noSender());
      cleanUp();
      return;
    }

    Optional<HiveConnection> connectionOptional = connectable.getConnection();
    if (!connectionOptional.isPresent()) {
      sender.tell(new ExecutionFailed(errorMessage), ActorRef.noSender());
      cleanUp();
      return;
    }

    try {
      Optional<ResultSet> resultSetOptional = operation.call(connectionOptional.get());
      if(resultSetOptional.isPresent()) {
        ActorRef resultSetActor = getContext().actorOf(Props.create(ResultSetIterator.class, self(),
          resultSetOptional.get()).withDispatcher("akka.actor.result-dispatcher"));
        deathWatch.tell(new RegisterActor(resultSetActor),self());
        sender.tell(new ResultSetHolder(resultSetActor), self());
      } else {
        sender.tell(new NoResult(), self());
        cleanUp();
      }
    } catch (SQLException e) {
      LOG.error(operation.executionFailedErrorMessage(), e);
      sender.tell(new ExecutionFailed(operation.executionFailedErrorMessage(), e), self());
      cleanUp();
    }
  }

  private abstract class Operation<T> {
    abstract T getJob();
    abstract Optional<ResultSet> call(HiveConnection connection) throws SQLException;
    abstract String notConnectedErrorMessage();
    abstract String executionFailedErrorMessage();
  }
}
