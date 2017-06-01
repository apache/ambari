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
import akka.actor.Cancellable;
import akka.actor.PoisonPill;
import akka.actor.Props;
import com.google.common.base.Optional;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive20.AuthParams;
import org.apache.ambari.view.hive20.ConnectionDelegate;
import org.apache.ambari.view.hive20.actor.message.Connect;
import org.apache.ambari.view.hive20.actor.message.FetchError;
import org.apache.ambari.view.hive20.actor.message.FetchResult;
import org.apache.ambari.view.hive20.actor.message.GetColumnMetadataJob;
import org.apache.ambari.view.hive20.actor.message.GetDatabaseMetadataJob;
import org.apache.ambari.view.hive20.actor.message.HiveJob;
import org.apache.ambari.view.hive20.actor.message.HiveMessage;
import org.apache.ambari.view.hive20.actor.message.ResultInformation;
import org.apache.ambari.view.hive20.actor.message.ResultNotReady;
import org.apache.ambari.view.hive20.actor.message.RunStatement;
import org.apache.ambari.view.hive20.actor.message.SQLStatementJob;
import org.apache.ambari.view.hive20.actor.message.job.AuthenticationFailed;
import org.apache.ambari.view.hive20.actor.message.job.CancelJob;
import org.apache.ambari.view.hive20.actor.message.job.ExecuteNextStatement;
import org.apache.ambari.view.hive20.actor.message.job.ExecutionFailed;
import org.apache.ambari.view.hive20.actor.message.job.Failure;
import org.apache.ambari.view.hive20.actor.message.job.NoResult;
import org.apache.ambari.view.hive20.actor.message.job.ResultSetHolder;
import org.apache.ambari.view.hive20.actor.message.job.SaveDagInformation;
import org.apache.ambari.view.hive20.actor.message.job.SaveGuidToDB;
import org.apache.ambari.view.hive20.actor.message.lifecycle.CleanUp;
import org.apache.ambari.view.hive20.actor.message.lifecycle.DestroyConnector;
import org.apache.ambari.view.hive20.actor.message.lifecycle.FreeConnector;
import org.apache.ambari.view.hive20.actor.message.lifecycle.InactivityCheck;
import org.apache.ambari.view.hive20.actor.message.lifecycle.KeepAlive;
import org.apache.ambari.view.hive20.actor.message.lifecycle.TerminateInactivityCheck;
import org.apache.ambari.view.hive20.client.DatabaseMetadataWrapper;
import org.apache.ambari.view.hive20.exceptions.ServiceException;
import org.apache.ambari.view.hive20.internal.Connectable;
import org.apache.ambari.view.hive20.internal.ConnectionException;
import org.apache.ambari.view.hive20.internal.parsers.DatabaseMetadataExtractor;
import org.apache.ambari.view.hive20.persistence.Storage;
import org.apache.ambari.view.hive20.persistence.utils.ItemNotFound;
import org.apache.ambari.view.hive20.resources.jobs.viewJobs.Job;
import org.apache.ambari.view.hive20.resources.jobs.viewJobs.JobImpl;
import org.apache.ambari.view.hive20.utils.HiveActorConfiguration;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.hive.jdbc.HiveConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


/**
 * Wraps one Jdbc connection per user, per instance. This is used to delegate execute the statements and
 * creates child actors to delegate the ResultSet extraction, YARN/ATS querying for ExecuteJob info and Log Aggregation
 */
public class JdbcConnector extends HiveActor {

  private static final Logger LOG = LoggerFactory.getLogger(JdbcConnector.class);

  public static final String SUFFIX = "validating the login";

  /**
   * Interval for maximum inactivity allowed
   */
  private final static long MAX_INACTIVITY_INTERVAL = 5 * 60 * 1000;

  /**
   * Interval for maximum inactivity allowed before termination
   */
  private static final long MAX_TERMINATION_INACTIVITY_INTERVAL = 10 * 60 * 1000;

  private static final long MILLIS_IN_SECOND = 1000L;

  private final Storage storage;

  /**
   * Keeps track of the timestamp when the last activity has happened. This is
   * used to calculate the inactivity period and take lifecycle decisions based
   * on it.
   */
  private long lastActivityTimestamp;

  /**
   * Akka scheduler to tick at an interval to deal with inactivity of this actor
   */
  private Cancellable inactivityScheduler;

  /**
   * Akka scheduler to tick at an interval to deal with the inactivity after which
   * the actor should be killed and connection should be released
   */
  private Cancellable terminateActorScheduler;

  private Connectable connectable = null;
  private final ActorRef deathWatch;
  private final ConnectionDelegate connectionDelegate;
  private final ActorRef parent;
  private ActorRef statementExecutor = null;
  private final HdfsApi hdfsApi;
  private final AuthParams authParams;

  /**
   * true if the actor is currently executing any job.
   */
  private boolean executing = false;
  private HiveJob.Type executionType = HiveJob.Type.SYNC;

  /**
   * Returns the timeout configurations.
   */
  private final HiveActorConfiguration actorConfiguration;
  private String username;
  private String instanceName;
  private Optional<String> jobId = Optional.absent();
  private Optional<String> logFile = Optional.absent();
  private int statementsCount = 0;

  private ActorRef commandSender = null;

  private ActorRef resultSetIterator = null;
  private boolean isFailure = false;
  private Failure failure = null;
  private boolean isCancelCalled = false;

  /**
   * For every execution, this will hold the statements that are left to execute
   */
  private Queue<String> statementQueue = new ArrayDeque<>();

  public JdbcConnector(ViewContext viewContext, ActorRef parent, ActorRef deathWatch, HdfsApi hdfsApi,
                       ConnectionDelegate connectionDelegate, Storage storage) {
    this.hdfsApi = hdfsApi;
    this.parent = parent;
    this.deathWatch = deathWatch;
    this.connectionDelegate = connectionDelegate;
    this.storage = storage;
    this.lastActivityTimestamp = System.currentTimeMillis();
    resultSetIterator = null;
    this.instanceName = viewContext.getInstanceName();

    authParams = new AuthParams(viewContext);
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
    } else {
      handleNonLifecycleMessage(hiveMessage);
    }
  }

  private void handleNonLifecycleMessage(HiveMessage hiveMessage) {
    Object message = hiveMessage.getMessage();
    keepAlive();
    if (message instanceof Connect) {
      connect((Connect) message);
    } else if (message instanceof SQLStatementJob) {
      runStatementJob((SQLStatementJob) message);
    } else if (message instanceof GetColumnMetadataJob) {
      runGetMetaData((GetColumnMetadataJob) message);
    } else if (message instanceof GetDatabaseMetadataJob) {
      runGetDatabaseMetaData((GetDatabaseMetadataJob) message);
    } else if (message instanceof ExecuteNextStatement) {
      executeNextStatement();
    } else if (message instanceof ResultInformation) {
      gotResultBack((ResultInformation) message);
    } else if (message instanceof CancelJob) {
      cancelJob((CancelJob) message);
    } else if (message instanceof FetchResult) {
      fetchResult((FetchResult) message);
    } else if (message instanceof FetchError) {
      fetchError((FetchError) message);
    } else if (message instanceof SaveGuidToDB) {
      saveGuid((SaveGuidToDB) message);
    } else if (message instanceof SaveDagInformation) {
      saveDagInformation((SaveDagInformation) message);
    } else {
      unhandled(message);
    }
  }

  private void fetchError(FetchError message) {
    if (isFailure) {
      sender().tell(Optional.of(failure), self());
      return;
    }
    sender().tell(Optional.absent(), self());
  }

  private void fetchResult(FetchResult message) {
    if (isFailure) {
      sender().tell(failure, self());
      return;
    }

    if (executing) {
      sender().tell(new ResultNotReady(jobId.get(), username), self());
      return;
    }
    sender().tell(Optional.fromNullable(resultSetIterator), self());
  }

  private void cancelJob(CancelJob message) {
    if (!executing || connectionDelegate == null) {
      LOG.error("Cannot cancel job for user as currently the job is not running or started. JobId: {}", message.getJobId());
      return;
    }
    LOG.info("Cancelling job for user. JobId: {}, user: {}", message.getJobId(), username);
    try {
      isCancelCalled = true;
      connectionDelegate.cancel();
      LOG.info("Cancelled JobId:"+ jobId);
    } catch (SQLException e) {
      LOG.error("Failed to cancel job. JobId: {}. {}", message.getJobId(), e);
    }
  }

  private void gotResultBack(ResultInformation message) {
    Optional<Failure> failureOptional = message.getFailure();
    if (failureOptional.isPresent()) {
      Failure failure = failureOptional.get();
      processFailure(failure);
      return;
    }
    Optional<DatabaseMetaData> databaseMetaDataOptional = message.getDatabaseMetaData();
    if (databaseMetaDataOptional.isPresent()) {
      DatabaseMetaData databaseMetaData = databaseMetaDataOptional.get();
      processDatabaseMetadata(databaseMetaData);
      return;
    }
    if (statementQueue.size() == 0) {
      // This is the last resultSet
      processResult(message.getResultSet());
    } else {
      self().tell(new ExecuteNextStatement(), self());
    }
  }

  private void processCancel() {
    executing = false;
    if (isAsync() && jobId.isPresent()) {
      LOG.error("Job canceled by user for JobId: {}", jobId.get());
      updateJobStatus(jobId.get(), Job.JOB_STATE_CANCELED);
    }
  }

  private void processFailure(Failure failure) {
    executing = false;
    isFailure = true;
    this.failure = failure;
    if (isAsync() && jobId.isPresent()) {
      stopStatementExecutor();
      if(isCancelCalled) {
        processCancel();
        return;
      }
      updateJobStatus(jobId.get(), Job.JOB_STATE_ERROR);
    } else {
      // Send for sync execution
      commandSender.tell(new ExecutionFailed(failure.getMessage(), failure.getError()), self());
      cleanUpWithTermination();
    }
  }

  private void processDatabaseMetadata(DatabaseMetaData databaseMetaData) {
    executing = false;
    isFailure = false;
    // Send for sync execution
    try {
      DatabaseMetadataWrapper databaseMetadataWrapper = new DatabaseMetadataExtractor(databaseMetaData).extract();
      commandSender.tell(databaseMetadataWrapper, self());
    } catch (ServiceException e) {
      commandSender.tell(new ExecutionFailed(e.getMessage(), e), self());
    }
    cleanUpWithTermination();
  }

  private void stopStatementExecutor() {
    if (statementExecutor != null) {
      statementExecutor.tell(PoisonPill.getInstance(), ActorRef.noSender());
      statementExecutor = null;
    }
  }

  private void processResult(Optional<ResultSet> resultSetOptional) {
    executing = false;

    stopStatementExecutor();

    LOG.info("Finished processing SQL statements for Job id : {}", jobId.or("SYNC JOB"));
    if (isAsync() && jobId.isPresent()) {
      updateJobStatus(jobId.get(), Job.JOB_STATE_FINISHED);
    }

    if (resultSetOptional.isPresent()) {
      ActorRef resultSetActor = getContext().actorOf(Props.create(ResultSetIterator.class, self(),
        resultSetOptional.get(), isAsync()).withDispatcher("akka.actor.result-dispatcher"),
        "ResultSetIterator:" + UUID.randomUUID().toString());
      resultSetIterator = resultSetActor;
      if (!isAsync()) {
        commandSender.tell(new ResultSetHolder(resultSetActor), self());
      }
    } else {
      resultSetIterator = null;
      if (!isAsync()) {
        commandSender.tell(new NoResult(), self());
      }
    }
  }

  private void executeNextStatement() {
    if (statementQueue.isEmpty()) {
      jobExecutionCompleted();
      return;
    }

    int index = statementsCount - statementQueue.size();
    String statement = statementQueue.poll();
    if (statementExecutor == null) {
      statementExecutor = getStatementExecutor();
    }

    if (isAsync()) {
      statementExecutor.tell(new RunStatement(index, statement, jobId.get(), true, logFile.get(), true), self());
    } else {
      statementExecutor.tell(new RunStatement(index, statement), self());
    }
  }

  private void runStatementJob(SQLStatementJob message) {
    executing = true;
    jobId = message.getJobId();
    logFile = message.getLogFile();
    executionType = message.getType();
    commandSender = getSender();

    resetToInitialState();

    if (!checkConnection()) return;

    for (String statement : message.getStatements()) {
      statementQueue.add(statement);
    }
    statementsCount = statementQueue.size();

    if (isAsync() && jobId.isPresent()) {
      updateJobStatus(jobId.get(), Job.JOB_STATE_RUNNING);
      startInactivityScheduler();
    }
    self().tell(new ExecuteNextStatement(), self());
  }

  public boolean checkConnection() {
    if (connectable == null) {
      notifyConnectFailure(new SQLException("Hive connection is not created"));
      return false;
    }

    Optional<HiveConnection> connectionOptional = connectable.getConnection();
    if (!connectionOptional.isPresent()) {
      SQLException sqlException = connectable.isUnauthorized() ? new SQLException("Hive Connection not Authorized", "AUTHFAIL")
              : new SQLException("Hive connection is not created");
      notifyConnectFailure(sqlException);
      return false;
    }
    return true;
  }

  private void runGetMetaData(GetColumnMetadataJob message) {
    if (!checkConnection()) return;
    resetToInitialState();
    executing = true;
    executionType = message.getType();
    commandSender = getSender();
    statementExecutor = getStatementExecutor();
    statementExecutor.tell(message, self());
  }

  private void runGetDatabaseMetaData(GetDatabaseMetadataJob message) {
    if (!checkConnection()) return;
    resetToInitialState();
    executing = true;
    executionType = message.getType();
    commandSender = getSender();
    statementExecutor = getStatementExecutor();
    statementExecutor.tell(message, self());
  }

  private ActorRef getStatementExecutor() {
    return getContext().actorOf(Props.create(StatementExecutor.class, hdfsApi, storage, connectable.getConnection().get(), connectionDelegate)
      .withDispatcher("akka.actor.result-dispatcher"),
      "StatementExecutor:" + UUID.randomUUID().toString());
  }

  private boolean isAsync() {
    return executionType == HiveJob.Type.ASYNC;
  }

  private void notifyConnectFailure(Exception ex) {
    boolean loginError = false;
    executing = false;
    isFailure = true;
    this.failure = new Failure("Cannot connect to hive", ex);
    if(ex instanceof ConnectionException){
      ConnectionException connectionException = (ConnectionException) ex;
      Throwable cause = connectionException.getCause();
      if(cause instanceof SQLException){
        SQLException sqlException = (SQLException) cause;
        if(isLoginError(sqlException))
          loginError = true;
      }
    }

    if (isAsync()) {
      updateJobStatus(jobId.get(), Job.JOB_STATE_ERROR);

      if (loginError) {
        return;
      }

    } else {
      if (loginError) {
        sender().tell(new AuthenticationFailed("Hive authentication error", ex), ActorRef.noSender());
      } else {
        sender().tell(new ExecutionFailed("Cannot connect to hive", ex), ActorRef.noSender());
      }

    }
    // Do not clean up in case of failed authorizations
    // The failure is bubbled to the user for requesting credentials

    if (!(ex instanceof SQLException) || !((SQLException) ex).getSQLState().equals("AUTHFAIL")) {
      cleanUpWithTermination();
    }
  }

  private boolean isLoginError(SQLException ce) {
    return ce.getCause().getMessage().toLowerCase().endsWith(SUFFIX);
  }

  private void keepAlive() {
    lastActivityTimestamp = System.currentTimeMillis();
  }

  private void jobExecutionCompleted() {
    // Set is executing as false so that the inactivity checks can finish cleanup
    // after timeout
    LOG.info("Job execution completed for user: {}. Results are ready to be fetched", username);
    this.executing = false;
  }

  protected Optional<String> getUsername() {
    return Optional.fromNullable(username);
  }

  private void connect(Connect message) {
    username = message.getUsername();
    jobId = message.getJobId();
    executionType = message.getType();
    // check the connectable
    if (connectable == null) {
      connectable = message.getConnectable(authParams);
    }
    // make the connectable to Hive
    try {
      if (!connectable.isOpen()) {
        connectable.connect();
      }
    } catch (ConnectionException e) {
      LOG.error("Failed to create a hive connection. {}", e);
      // set up job failure
      // notify parent about job failure
      notifyConnectFailure(e);
      return;
    }
    startTerminateInactivityScheduler();
  }

  private void updateJobStatus(String jobid, final String status) {
    new JobSaver(jobid) {
      @Override
      protected void update(JobImpl job) {
        job.setStatus(status);
        job.setDuration(getUpdatedDuration(job.getDateSubmitted()));
      }
    }.save();
    LOG.info("Stored job status for Job id: {} as '{}'", jobid, status);
  }

  private void saveGuid(final SaveGuidToDB message) {
    new JobSaver(message.getJobId()) {
      @Override
      protected void update(JobImpl job) {
        job.setGuid(message.getGuid());
      }
    }.save();
    LOG.info("Stored GUID for Job id: {} as '{}'", message.getJobId(), message.getGuid());
  }

  private void saveDagInformation(final SaveDagInformation message) {
    if(message.getDagId() == null &&
        message.getDagName() == null &&
        message.getApplicationId() == null) {
      LOG.error("Cannot save Dag Information for job Id: {} as all the properties are null.", message.getJobId());
      return;
    }
    new JobSaver(message.getJobId()) {

      @Override
      protected void update(JobImpl job) {
        if (message.getApplicationId() != null) {
          job.setApplicationId(message.getApplicationId());
        }
        if (message.getDagId() != null) {
          job.setDagId(message.getDagId());
        }
        if(message.getDagName() != null) {
          job.setDagName(message.getDagName());
        }
      }
    }.save();
    LOG.info("Store Dag Information for job. Job id: {}, dagName: {}, dagId: {}, applicationId: {}", message.getJobId(), message.getDagName(), message.getDagId(), message.getApplicationId());
  }

  private Long getUpdatedDuration(Long dateSubmitted) {
    return (System.currentTimeMillis() / MILLIS_IN_SECOND) - (dateSubmitted / MILLIS_IN_SECOND);
  }


  private void checkInactivity() {
    LOG.debug("Inactivity check, executing status: {}", executing);
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
    LOG.debug("Termination check, executing status: {}", executing);
    if (executing) {
      keepAlive();
      return;
    }

    long current = System.currentTimeMillis();
    if ((current - lastActivityTimestamp) > actorConfiguration.getTerminationTimeout(MAX_TERMINATION_INACTIVITY_INTERVAL)) {
      cleanUpWithTermination();
    }
  }

  private void cleanUp() {
    if (jobId.isPresent()) {
      LOG.debug("{} :: Cleaning up resources for inactivity for jobId: {}", self().path().name(), jobId.get());
    } else {
      LOG.debug("{} ::Cleaning up resources with inactivity for Sync execution.", self().path().name());
    }
    this.executing = false;
    cleanUpStatementAndResultSet();
    stopInactivityScheduler();
    parent.tell(new FreeConnector(username, jobId.orNull(), isAsync()), self());
  }

  private void cleanUpWithTermination() {
    this.executing = false;
    LOG.debug("{} :: Cleaning up resources with inactivity for execution.", self().path().name());
    cleanUpStatementAndResultSet();

    stopInactivityScheduler();
    stopTerminateInactivityScheduler();
    parent.tell(new DestroyConnector(username, jobId.orNull(), isAsync()), this.self());
    self().tell(PoisonPill.getInstance(), ActorRef.noSender());
  }


  private void cleanUpStatementAndResultSet() {
    connectionDelegate.closeStatement();
    connectionDelegate.closeResultSet();
  }

  private void startTerminateInactivityScheduler() {
    this.terminateActorScheduler = getContext().system().scheduler().schedule(
      Duration.Zero(), Duration.create(60 * 1000, TimeUnit.MILLISECONDS),
      this.getSelf(), new TerminateInactivityCheck(), getContext().dispatcher(), null);
  }

  private void stopTerminateInactivityScheduler() {
    if (!(terminateActorScheduler == null || terminateActorScheduler.isCancelled())) {
      terminateActorScheduler.cancel();
    }
  }

  private void startInactivityScheduler() {
    if (inactivityScheduler != null) {
      inactivityScheduler.cancel();
    }
    inactivityScheduler = getContext().system().scheduler().schedule(
      Duration.Zero(), Duration.create(15 * 1000, TimeUnit.MILLISECONDS),
      this.self(), new InactivityCheck(), getContext().dispatcher(), null);
  }

  private void stopInactivityScheduler() {
    if (!(inactivityScheduler == null || inactivityScheduler.isCancelled())) {
      inactivityScheduler.cancel();
    }
  }

  private void resetToInitialState() {
    isFailure = false;
    failure = null;
    resultSetIterator = null;
    isCancelCalled = false;
    statementQueue = new ArrayDeque<>();
  }

  @Override
  public void postStop() throws Exception {
    stopInactivityScheduler();
    stopTerminateInactivityScheduler();

    if (connectable.isOpen()) {
      connectable.disconnect();
    }
  }

  /**
   * Saves the job to database.
   */
  private abstract class JobSaver {
    private final String jobId;

    JobSaver(String jobId) {
      this.jobId = jobId;
    }

    public void save() {
      try {
        JobImpl job = storage.load(JobImpl.class, jobId);
        update(job);
        storage.store(JobImpl.class, job);
      } catch (ItemNotFound itemNotFound) {
        itemNotFound(jobId);
      }
    }

    /**
     * Override to handle Not found exception
     */
    private void itemNotFound(String jobId) {
      // Nothing to do
    }

    protected abstract void update(JobImpl job);
  }
}