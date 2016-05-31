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

package org.apache.ambari.view.hive2.client;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import com.google.common.base.Optional;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive2.actor.message.CursorReset;
import org.apache.ambari.view.hive2.actor.message.FetchError;
import org.apache.ambari.view.hive2.actor.message.ResetCursor;
import org.apache.ambari.view.hive2.resources.jobs.viewJobs.Job;
import org.apache.ambari.view.hive2.actor.message.AsyncJob;
import org.apache.ambari.view.hive2.actor.message.Connect;
import org.apache.ambari.view.hive2.actor.message.ExecuteJob;
import org.apache.ambari.view.hive2.actor.message.FetchResult;
import org.apache.ambari.view.hive2.actor.message.job.AsyncExecutionFailed;
import org.apache.ambari.view.hive2.internal.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

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
    public void submitJob(ConnectionConfig config, AsyncJob job, Job jobp) {
        Connect connect = config.createConnectMessage();
        ExecuteJob executeJob = new ExecuteJob(connect, job);
        controller.tell(executeJob,ActorRef.noSender());
    }

  @Override
  public Optional<NonPersistentCursor> getCursor(String jobId, String username) {
    Inbox inbox = Inbox.create(system);
    inbox.send(controller, new FetchResult(jobId, username));
    Object receive = inbox.receive(Duration.create(1, TimeUnit.MINUTES));
    Either<ActorRef, ActorRef> result = (Either<ActorRef, ActorRef>) receive;
    if (result.isRight()) {
      return Optional.absent();

    } else if (result.isLeft()) {
      return Optional.of(new NonPersistentCursor(context, system, result.getLeft()));
    }

    return Optional.absent();
  }

  @Override
  public Optional<NonPersistentCursor> resetAndGetCursor(String jobId, String username) {
    Inbox inbox = Inbox.create(system);
    inbox.send(controller, new FetchResult(jobId, username));
    Object receive = inbox.receive(Duration.create(1, TimeUnit.MINUTES));
    Either<ActorRef, ActorRef> result = (Either<ActorRef, ActorRef>) receive;
    if (result.isRight()) {
      return Optional.absent();

    } else if (result.isLeft()) {
      // Reset the result set cursor
      inbox.send(result.getLeft(), new ResetCursor());
      Object resetResult = inbox.receive(Duration.create(1, TimeUnit.MINUTES));
      if (resetResult instanceof CursorReset) {
        return Optional.of(new NonPersistentCursor(context, system, result.getLeft()));
      } else {
        return Optional.absent();
      }

    }

    return Optional.absent();
  }

    @Override
    public Optional<AsyncExecutionFailed> getError(String jobId, String username) {
        Inbox inbox = Inbox.create(system);
        inbox.send(controller, new FetchError(jobId, username));
        Object receive = inbox.receive(Duration.create(1, TimeUnit.MINUTES));
        Optional<AsyncExecutionFailed>  result = (Optional<AsyncExecutionFailed>) receive;
        return result;
    }


}
