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
import org.apache.ambari.view.hive2.ConnectionDelegate;
import org.apache.ambari.view.hive2.actor.message.AsyncJob;
import org.apache.ambari.view.hive2.actor.message.HiveMessage;
import org.apache.ambari.view.hive2.actor.message.RegisterActor;
import org.apache.ambari.view.hive2.actor.message.ResultReady;
import org.apache.ambari.view.hive2.actor.message.StartLogAggregation;
import org.apache.ambari.view.hive2.actor.message.job.AsyncExecutionFailed;
import org.apache.ambari.view.hive2.actor.message.lifecycle.InactivityCheck;
import org.apache.ambari.view.hive2.internal.Either;
import org.apache.ambari.view.hive2.persistence.Storage;
import org.apache.ambari.view.hive2.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive2.resources.jobs.viewJobs.Job;
import org.apache.ambari.view.hive2.resources.jobs.viewJobs.JobImpl;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.hive.jdbc.HiveConnection;
import org.apache.hive.jdbc.HiveStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class AsyncJdbcConnector extends JdbcConnector {

  private final Logger LOG = LoggerFactory.getLogger(getClass());

  private ActorRef logAggregator = null;
  private ActorRef asyncQueryExecutor = null;
  private ActorRef resultSetActor = null;


  public AsyncJdbcConnector(ViewContext viewContext, HdfsApi hdfsApi, ActorSystem system, ActorRef parent,ActorRef deathWatch, ConnectionDelegate connectionDelegate, Storage storage) {
    super(viewContext, hdfsApi, system, parent,deathWatch, connectionDelegate, storage);
  }

  @Override
  protected void handleJobMessage(HiveMessage message) {
    Object job = message.getMessage();
    if (job instanceof AsyncJob) {
      LOG.debug("Executing async job " + message.toString());
      execute((AsyncJob) job);
    }
  }

  @Override
  protected boolean isAsync() {
    return true;
  }

  @Override
  protected void cleanUpChildren() {
    if(logAggregator != null && !logAggregator.isTerminated()) {
      LOG.debug("Sending poison pill to log aggregator");
      logAggregator.tell(PoisonPill.getInstance(), self());
    }

    if(asyncQueryExecutor != null && !asyncQueryExecutor.isTerminated()) {
      LOG.debug("Sending poison pill to Async Query Executor");
      asyncQueryExecutor.tell(PoisonPill.getInstance(), self());
    }

    if(resultSetActor != null && !resultSetActor.isTerminated()) {
      LOG.debug("Sending poison pill to Resultset Actor");
      resultSetActor.tell(PoisonPill.getInstance(), self());
    }
  }

  @Override
  protected void notifyFailure() {
    AsyncExecutionFailed failure = new AsyncExecutionFailed(jobId,username,"Cannot connect to hive");
    parent.tell(failure, self());
  }

  private void execute(AsyncJob message) {
    this.executing = true;
    this.jobId = message.getJobId();
    updateJobStatus(jobId,Job.JOB_STATE_INITIALIZED);
    if (connectable == null) {
      notifyAndCleanUp();
      return;
    }

    Optional<HiveConnection> connectionOptional = connectable.getConnection();
    if (!connectionOptional.isPresent()) {
      notifyAndCleanUp();
      return;
    }

    try {
      Optional<ResultSet> resultSetOptional = connectionDelegate.execute(connectionOptional.get(), message);
      Optional<HiveStatement> currentStatement = connectionDelegate.getCurrentStatement();
      // There should be a result set, which either has a result set, or an empty value
      // for operations which do not return anything

      logAggregator = getContext().actorOf(
        Props.create(LogAggregator.class, system, hdfsApi, currentStatement.get(), message.getLogFile())
        .withDispatcher("akka.actor.misc-dispatcher"),   message.getJobId() + ":" +"-logAggregator"
      );
      deathWatch.tell(new RegisterActor(logAggregator),self());

      updateGuidInJob(jobId, currentStatement.get());
      updateJobStatus(jobId,Job.JOB_STATE_RUNNING);

      if (resultSetOptional.isPresent()) {
        // Start a result set aggregator on the same context, a notice to the parent will kill all these as well
        // tell the result holder to assign the result set for further operations
        resultSetActor = getContext().actorOf(Props.create(ResultSetIterator.class, self(),
          resultSetOptional.get(),storage).withDispatcher("akka.actor.result-dispatcher"),
          "ResultSetActor:ResultSetIterator:JobId:"+ jobId );
        deathWatch.tell(new RegisterActor(resultSetActor),self());
        parent.tell(new ResultReady(jobId,username, Either.<ActorRef, ActorRef>left(resultSetActor)), self());

        // Start a actor to query ATS
      } else {
        // Case when this is an Update/query with no results
        // Wait for operation to complete and add results;

        ActorRef asyncQueryExecutor = getContext().actorOf(
                Props.create(AsyncQueryExecutor.class, parent, currentStatement.get(),storage,jobId,username)
                  .withDispatcher("akka.actor.result-dispatcher"),
                 message.getJobId() + "-asyncQueryExecutor");
        deathWatch.tell(new RegisterActor(asyncQueryExecutor),self());
        parent.tell(new ResultReady(jobId,username, Either.<ActorRef, ActorRef>right(asyncQueryExecutor)), self());

      }
      // Start a actor to query log
      logAggregator.tell(new StartLogAggregation(), self());


    } catch (SQLException e) {
      // update the error on the log
      AsyncExecutionFailed failure = new AsyncExecutionFailed(message.getJobId(),username,
              e.getMessage(), e);
      updateJobStatus(jobId,Job.JOB_STATE_ERROR);
      parent.tell(failure, self());
      // Update the operation controller to write an error on the right side
      // make sure we can stop the connector
      executing = false;
      LOG.error("Caught SQL excpetion for job-"+message,e);

    }

    // Start Inactivity timer to close the statement
    this.inactivityScheduler = system.scheduler().schedule(
      Duration.Zero(), Duration.create(15 * 1000, TimeUnit.MILLISECONDS),
      this.self(), new InactivityCheck(), system.dispatcher(), null);
  }

  private void notifyAndCleanUp() {
    updateJobStatus(jobId, Job.JOB_STATE_ERROR);
    notifyFailure();
    cleanUp();
  }

  private void updateJobStatus(String jobId, String jobState) {
    JobImpl job = null;
    try {
      job = storage.load(JobImpl.class, jobId);
    } catch (ItemNotFound itemNotFound) {
      itemNotFound.printStackTrace();
    }
    job.setStatus(jobState);
    storage.store(JobImpl.class, job);
  }
}
