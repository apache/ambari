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

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import com.google.common.base.Optional;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive20.actor.message.Connect;
import org.apache.ambari.view.hive20.actor.message.CursorReset;
import org.apache.ambari.view.hive20.actor.message.ExecuteJob;
import org.apache.ambari.view.hive20.actor.message.FetchError;
import org.apache.ambari.view.hive20.actor.message.FetchResult;
import org.apache.ambari.view.hive20.actor.message.ResetCursor;
import org.apache.ambari.view.hive20.actor.message.ResultNotReady;
import org.apache.ambari.view.hive20.actor.message.SQLStatementJob;
import org.apache.ambari.view.hive20.actor.message.job.CancelJob;
import org.apache.ambari.view.hive20.actor.message.job.Failure;
import org.apache.ambari.view.hive20.actor.message.job.FetchFailed;
import org.apache.ambari.view.hive20.internal.ConnectionException;
import org.apache.ambari.view.hive20.resources.jobs.viewJobs.Job;
import org.apache.ambari.view.hive20.utils.ResultFetchFormattedException;
import org.apache.ambari.view.hive20.utils.ResultNotReadyFormattedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class AsyncJobRunnerImpl implements AsyncJobRunner {

  private final Logger LOG = LoggerFactory.getLogger(getClass());

  private final ActorRef controller;
  private final ActorSystem system;
  private final ViewContext context;

  public AsyncJobRunnerImpl(ViewContext context, ActorRef controller, ActorSystem system) {
    this.context = context;
    this.controller = controller;
    this.system = system;
  }


  @Override
  public void submitJob(ConnectionConfig config, SQLStatementJob job, Job jobp) {
    Connect connect = config.createConnectMessage(jobp.getId());
    ExecuteJob executeJob = new ExecuteJob(connect, job);
    controller.tell(executeJob, ActorRef.noSender());
  }

  @Override
  public void cancelJob(String jobId, String username) {
    controller.tell(new CancelJob(jobId, username), ActorRef.noSender());
  }

  @Override
  public Optional<NonPersistentCursor> getCursor(String jobId, String username) {
    Inbox inbox = Inbox.create(system);
    inbox.send(controller, new FetchResult(jobId, username));
    Object receive = inbox.receive(Duration.create(1, TimeUnit.MINUTES));
    if(receive instanceof ResultNotReady) {
      String errorString = "Result not ready for job: " + jobId + ", username: " + username + ". Try after sometime.";
      LOG.info(errorString);
      throw new ResultNotReadyFormattedException(errorString, new Exception(errorString));
    } else if(receive instanceof  Failure) {
      Failure failure = (Failure) receive;
      throw new ResultFetchFormattedException(failure.getMessage(), failure.getError());
    } else {
      Optional<ActorRef> iterator = (Optional<ActorRef>) receive;
      if(iterator.isPresent()) {
        return Optional.of(new NonPersistentCursor(context, system, iterator.get()));
      } else {
        return Optional.absent();
      }
    }
  }

  @Override
  public Optional<NonPersistentCursor> resetAndGetCursor(String jobId, String username) {
    Inbox inbox = Inbox.create(system);
    inbox.send(controller, new FetchResult(jobId, username));
    Object receive = inbox.receive(Duration.create(1, TimeUnit.MINUTES));
    if(receive instanceof ResultNotReady) {
      String errorString = "Result not ready for job: " + jobId + ", username: " + username + ". Try after sometime.";
      LOG.info(errorString);
      throw new ResultNotReadyFormattedException(errorString, new Exception(errorString));
    } else if(receive instanceof  Failure) {
      Failure failure = (Failure) receive;
      throw new ResultFetchFormattedException(failure.getMessage(), failure.getError());
    } else {
      Optional<ActorRef> iterator = (Optional<ActorRef>) receive;
      if(iterator.isPresent()) {
        inbox.send(iterator.get(), new ResetCursor());
        Object resetResult = inbox.receive(Duration.create(1, TimeUnit.MINUTES));
        if (resetResult instanceof CursorReset) {
          return Optional.of(new NonPersistentCursor(context, system, iterator.get()));
        } else {
          return Optional.absent();
        }
      } else {
        return Optional.absent();
      }
    }
  }

  @Override
  public Optional<Failure> getError(String jobId, String username) {
    Inbox inbox = Inbox.create(system);
    inbox.send(controller, new FetchError(jobId, username));
    Object receive = inbox.receive(Duration.create(1, TimeUnit.MINUTES));
    if(receive instanceof FetchFailed){
      FetchFailed fetchFailed = (FetchFailed) receive;
      return Optional.of(new Failure(fetchFailed.getMessage(), getExceptionForRetry()));
    }
    Optional<Failure> result = (Optional<Failure>) receive;
    return result;
  }

  private ConnectionException getExceptionForRetry() {
    return new ConnectionException(new SQLException("Cannot connect"),"Connection attempt failed, Please retry");
  }


}
