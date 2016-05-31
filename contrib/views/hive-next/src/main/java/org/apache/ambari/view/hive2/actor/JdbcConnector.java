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
import akka.actor.Cancellable;
import akka.actor.PoisonPill;
import com.google.common.base.Optional;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive2.ConnectionDelegate;
import org.apache.ambari.view.hive2.actor.message.Connect;
import org.apache.ambari.view.hive2.actor.message.HiveMessage;
import org.apache.ambari.view.hive2.actor.message.JobExecutionCompleted;
import org.apache.ambari.view.hive2.actor.message.lifecycle.CleanUp;
import org.apache.ambari.view.hive2.actor.message.lifecycle.DestroyConnector;
import org.apache.ambari.view.hive2.actor.message.lifecycle.FreeConnector;
import org.apache.ambari.view.hive2.actor.message.lifecycle.InactivityCheck;
import org.apache.ambari.view.hive2.actor.message.lifecycle.KeepAlive;
import org.apache.ambari.view.hive2.actor.message.lifecycle.TerminateInactivityCheck;
import org.apache.ambari.view.hive2.internal.Connectable;
import org.apache.ambari.view.hive2.internal.ConnectionException;
import org.apache.ambari.view.hive2.persistence.Storage;
import org.apache.ambari.view.hive2.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive2.resources.jobs.viewJobs.JobImpl;
import org.apache.ambari.view.hive2.utils.HiveActorConfiguration;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.hive.jdbc.HiveStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;


/**
 * Wraps one Jdbc connection per user, per instance. This is used to delegate execute the statements and
 * creates child actors to delegate the resultset extraction, YARN/ATS querying for ExecuteJob info and Log Aggregation
 */
public abstract class JdbcConnector extends HiveActor {

  private final Logger LOG = LoggerFactory.getLogger(getClass());

  /**
   * Interval for maximum inactivity allowed
   */
  private final static long MAX_INACTIVITY_INTERVAL = 5 * 60 * 1000;

  /**
   * Interval for maximum inactivity allowed before termination
   */
  private static final long MAX_TERMINATION_INACTIVITY_INTERVAL = 10 * 60 * 1000;

  protected final ViewContext viewContext;
  protected final ActorSystem system;
  protected final Storage storage;

  /**
   * Keeps track of the timestamp when the last activity has happened. This is
   * used to calculate the inactivity period and take lifecycle decisions based
   * on it.
   */
  private long lastActivityTimestamp;

  /**
   * Akka scheduler to tick at an interval to deal with inactivity of this actor
   */
  protected Cancellable inactivityScheduler;

  /**
   * Akka scheduler to tick at an interval to deal with the inactivity after which
   * the actor should be killed and connectable should be released
   */
  protected Cancellable terminateActorScheduler;

  protected Connectable connectable = null;
  protected final ActorRef deathWatch;
  protected final ConnectionDelegate connectionDelegate;
  protected final ActorRef parent;
  protected final HdfsApi hdfsApi;

  /**
   * true if the actor is currently executing any job.
   */
  protected boolean executing = false;

  /**
   * true if the currently executing job is async job.
   */
  private boolean async = true;

  /**
   * Returns the timeout configurations.
   */
  private final HiveActorConfiguration actorConfiguration;
  protected String username;
  protected String jobId;

  public JdbcConnector(ViewContext viewContext, HdfsApi hdfsApi, ActorSystem system, ActorRef parent, ActorRef deathWatch,
                       ConnectionDelegate connectionDelegate, Storage storage) {
    this.viewContext = viewContext;
    this.hdfsApi = hdfsApi;
    this.system = system;
    this.parent = parent;
    this.deathWatch = deathWatch;
    this.connectionDelegate = connectionDelegate;
    this.storage = storage;
    this.lastActivityTimestamp = System.currentTimeMillis();
    actorConfiguration = new HiveActorConfiguration(viewContext);
  }

  @Override
  public void handleMessage(HiveMessage hiveMessage) {
    Object message = hiveMessage.getMessage();
    if (message instanceof InactivityCheck) {
      checkInactivity();
    } else if (message instanceof TerminateInactivityCheck) {
      checkTerminationInactivity();
    } else if (message instanceof KeepAlive) {
      keepAlive();
    } else if (message instanceof CleanUp) {
      cleanUp();
    } else if (message instanceof JobExecutionCompleted) {
      jobExecutionCompleted();
    } else {
      handleNonLifecycleMessage(hiveMessage);
    }
  }

  private void handleNonLifecycleMessage(HiveMessage hiveMessage) {
    Object message = hiveMessage.getMessage();
    keepAlive();
    if (message instanceof Connect) {
      connect((Connect) message);
    } else {
      handleJobMessage(hiveMessage);
    }

  }

  protected abstract void handleJobMessage(HiveMessage message);

  protected abstract boolean isAsync();

  protected abstract void notifyFailure();

  protected abstract void cleanUpChildren();

  private void keepAlive() {
    lastActivityTimestamp = System.currentTimeMillis();
  }

  private void jobExecutionCompleted() {
    // Set is executing as false so that the inactivity checks can finish cleanup
    // after timeout
    LOG.info("Job execution completed for user: {}. Results are ready to be fetched", username);
    this.executing = false;
  }

  protected Optional<String> getJobId() {
    return Optional.fromNullable(jobId);
  }

  protected Optional<String> getUsername() {
    return Optional.fromNullable(username);
  }

  private void connect(Connect message) {
    this.username = message.getUsername();
    // check the connectable
    if (connectable == null) {
      connectable = message.getConnectable();
    }
    // make the connectable to Hive
    try {
      if (!connectable.isOpen()) {
        connectable.connect();
      }
    } catch (ConnectionException e) {
      // set up job failure
      // notify parent about job failure
      this.notifyFailure();
      cleanUp();
      return;
    }

    this.terminateActorScheduler = system.scheduler().schedule(
      Duration.Zero(), Duration.create(60 * 1000, TimeUnit.MILLISECONDS),
      this.getSelf(), new TerminateInactivityCheck(), system.dispatcher(), null);

  }

  protected void updateGuidInJob(String jobId, HiveStatement statement) {
    String yarnAtsGuid = statement.getYarnATSGuid();
    try {
      JobImpl job = storage.load(JobImpl.class, jobId);
      job.setGuid(yarnAtsGuid);
      storage.store(JobImpl.class, job);
    } catch (ItemNotFound itemNotFound) {
      // Cannot do anything if the job is not present
    }
  }

  private void checkInactivity() {
    LOG.info("Inactivity check, executing status: {}", executing);
    if (executing) {
      keepAlive();
      return;
    }
    long current = System.currentTimeMillis();
    if ((current - lastActivityTimestamp) > actorConfiguration.getInactivityTimeout(MAX_INACTIVITY_INTERVAL)) {
      // Stop all the sub-actors created
      cleanUp();
    }
  }

  private void checkTerminationInactivity() {
    if (!isAsync()) {
      // Should not terminate if job is sync. Will terminate after the job is finished.
      stopTeminateInactivityScheduler();
      return;
    }

    LOG.info("Termination check, executing status: {}", executing);
    if (executing) {
      keepAlive();
      return;
    }

    long current = System.currentTimeMillis();
    if ((current - lastActivityTimestamp) > actorConfiguration.getTerminationTimeout(MAX_TERMINATION_INACTIVITY_INTERVAL)) {
      cleanUpWithTermination();
    }
  }

  protected void cleanUp() {
    if(jobId != null) {
      LOG.debug("{} :: Cleaning up resources for inactivity for jobId: {}", self().path().name(), jobId);
    } else {
      LOG.debug("{} ::Cleaning up resources with inactivity for Sync execution.", self().path().name());
    }
    this.executing = false;
    cleanUpStatementAndResultSet();
    cleanUpChildren();
    stopInactivityScheduler();
    parent.tell(new FreeConnector(username, jobId, isAsync()), self());
  }

  protected void cleanUpWithTermination() {
    LOG.debug("{} :: Cleaning up resources with inactivity for Sync execution.", self().path().name());
    cleanUpStatementAndResultSet();

    cleanUpChildren();
    stopInactivityScheduler();
    stopTeminateInactivityScheduler();
    parent.tell(new DestroyConnector(username, jobId, isAsync()), this.self());
    self().tell(PoisonPill.getInstance(), ActorRef.noSender());
  }


  private void cleanUpStatementAndResultSet() {
    connectionDelegate.closeStatement();
    connectionDelegate.closeResultSet();
  }

  private void stopTeminateInactivityScheduler() {
    if (!(terminateActorScheduler == null || terminateActorScheduler.isCancelled())) {
      terminateActorScheduler.cancel();
    }
  }

  private void stopInactivityScheduler() {
    if (!(inactivityScheduler == null || inactivityScheduler.isCancelled())) {
      inactivityScheduler.cancel();
    }
  }

  @Override
  public void postStop() throws Exception {
    stopInactivityScheduler();
    stopTeminateInactivityScheduler();

    if (connectable.isOpen()) {
      connectable.disconnect();
    }
  }


}
