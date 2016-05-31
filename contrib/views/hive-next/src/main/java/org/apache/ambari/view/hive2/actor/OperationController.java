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
import akka.actor.Props;
import com.google.common.base.Optional;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive2.ConnectionDelegate;
import org.apache.ambari.view.hive2.actor.message.AdvanceCursor;
import org.apache.ambari.view.hive2.actor.message.AsyncJob;
import org.apache.ambari.view.hive2.actor.message.Connect;
import org.apache.ambari.view.hive2.actor.message.ExecuteJob;
import org.apache.ambari.view.hive2.actor.message.ExecuteQuery;
import org.apache.ambari.view.hive2.actor.message.FetchError;
import org.apache.ambari.view.hive2.actor.message.FetchResult;
import org.apache.ambari.view.hive2.actor.message.HiveJob;
import org.apache.ambari.view.hive2.actor.message.HiveMessage;
import org.apache.ambari.view.hive2.actor.message.JobRejected;
import org.apache.ambari.view.hive2.actor.message.RegisterActor;
import org.apache.ambari.view.hive2.actor.message.ResultReady;
import org.apache.ambari.view.hive2.actor.message.job.AsyncExecutionFailed;
import org.apache.ambari.view.hive2.actor.message.lifecycle.DestroyConnector;
import org.apache.ambari.view.hive2.actor.message.lifecycle.FreeConnector;
import org.apache.ambari.view.hive2.internal.ContextSupplier;
import org.apache.ambari.view.hive2.internal.Either;
import org.apache.ambari.view.hive2.persistence.Storage;
import org.apache.ambari.view.hive2.utils.LoggingOutputStream;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.map.HashedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * Router actor to control the operations. This delegates the operations to underlying child actors and
 * store the state for them.
 */
public class OperationController extends HiveActor {

  private final Logger LOG = LoggerFactory.getLogger(getClass());

  private final ActorSystem system;
  private final ActorRef deathWatch;
  private final ContextSupplier<ConnectionDelegate> connectionSupplier;
  private final ContextSupplier<Storage> storageSupplier;
  private final ContextSupplier<Optional<HdfsApi>> hdfsApiSupplier;

  /**
   * Store the connection per user which are currently not working
   */
  private final Map<String, Queue<ActorRef>> asyncAvailableConnections;

  /**
   * Store the connection per user which are currently not working
   */
  private final Map<String, Queue<ActorRef>> syncAvailableConnections;


  /**
   * Store the connection per user/per job which are currently working.
   */
  private final Map<String, Map<String, ActorRefResultContainer>> asyncBusyConnections;

  /**
   * Store the connection per user which will be used to execute sync jobs
   * like fetching databases, tables etc.
   */
  private final Map<String, Set<ActorRef>> syncBusyConnections;

  public OperationController(ActorSystem system,
                             ActorRef deathWatch,
                             ContextSupplier<ConnectionDelegate> connectionSupplier,
                             ContextSupplier<Storage> storageSupplier,
                             ContextSupplier<Optional<HdfsApi>> hdfsApiSupplier) {
    this.system = system;
    this.deathWatch = deathWatch;
    this.connectionSupplier = connectionSupplier;
    this.storageSupplier = storageSupplier;
    this.hdfsApiSupplier = hdfsApiSupplier;
    this.asyncAvailableConnections = new HashMap<>();
    this.syncAvailableConnections = new HashMap<>();
    this.asyncBusyConnections = new HashedMap<>();
    this.syncBusyConnections = new HashMap<>();
  }

  @Override
  public void handleMessage(HiveMessage hiveMessage) {
    Object message = hiveMessage.getMessage();

    if (message instanceof ExecuteJob) {
      ExecuteJob job = (ExecuteJob) message;
      if (job.getJob().getType() == HiveJob.Type.ASYNC) {
        sendJob(job.getConnect(), (AsyncJob) job.getJob());
      } else if (job.getJob().getType() == HiveJob.Type.SYNC) {
        sendSyncJob(job.getConnect(), job.getJob());
      }
    }

    if (message instanceof ResultReady) {
      updateResultContainer((ResultReady) message);
    }

    if(message instanceof AsyncExecutionFailed){
      updateResultContainerWithError((AsyncExecutionFailed) message);
    }

    if (message instanceof GetResultHolder) {
      getResultHolder((GetResultHolder) message);
    }

    if (message instanceof FetchResult) {
      fetchResultActorRef((FetchResult) message);
    }

    if (message instanceof FetchError) {
      fetchError((FetchError) message);
    }

    if (message instanceof FreeConnector) {
      freeConnector((FreeConnector) message);
    }

    if (message instanceof DestroyConnector) {
      destroyConnector((DestroyConnector) message);
    }
  }

  private void fetchError(FetchError message) {
    String jobId = message.getJobId();
    String username = message.getUsername();
    ActorRefResultContainer container = asyncBusyConnections.get(username).get(jobId);
    if(container.hasError){
      sender().tell(Optional.of(container.error), self());
      return;
    }
    sender().tell(Optional.absent(), self());
  }

  private void updateResultContainerWithError(AsyncExecutionFailed message) {
    String userName = message.getUsername();
    String jobId = message.getJobId();
    ActorRefResultContainer container = asyncBusyConnections.get(userName).get(jobId);
    container.hasError = true;
    container.error = message;
  }

  private void getResultHolder(GetResultHolder message) {
    String userName = message.getUserName();
    String jobId = message.getJobId();
    if(asyncBusyConnections.containsKey(userName) && asyncBusyConnections.get(userName).containsKey(jobId))
      sender().tell(asyncBusyConnections.get(userName).get(jobId).result, self());
    else {
      Either<ActorRef, AsyncExecutionFailed> right = Either.right(new AsyncExecutionFailed(message.getJobId(),userName, "Could not find the job, maybe the pool expired"));
      sender().tell(right, self());
    }
  }

  private void updateResultContainer(ResultReady message) {
    // set up result actor in container
    String jobId = message.getJobId();
    String username = message.getUsername();
    Either<ActorRef, ActorRef> result = message.getResult();
    asyncBusyConnections.get(username).get(jobId).result = result;
    // start processing
    if(message.getResult().isRight()){
      // Query with no result sets to be returned
      // execute right away
      result.getRight().tell(new ExecuteQuery(),self());
    }
    if(result.isLeft()){
      // There is a result set to be processed
      result.getLeft().tell(new AdvanceCursor(message.getJobId()),self());
    }

  }

  private void fetchResultActorRef(FetchResult message) {
    //Gets an Either actorRef,result implementation
    // and send back to the caller
    String username = message.getUsername();
    String jobId = message.getJobId();
    ActorRefResultContainer container = asyncBusyConnections.get(username).get(jobId);
    if(container.hasError){
      sender().tell(container.error,self());
      return;
    }
    Either<ActorRef, ActorRef> result = container.result;
    sender().tell(result,self());
  }

  private void sendJob(Connect connect, AsyncJob job) {
    String username = job.getUsername();
    String jobId = job.getJobId();
    ActorRef subActor = null;
    // Check if there is available actors to process this
    subActor = getActorRefFromAsyncPool(username);
    ViewContext viewContext = job.getViewContext();
    if (subActor == null) {
      Optional<HdfsApi> hdfsApiOptional = hdfsApiSupplier.get(viewContext);
      if (!hdfsApiOptional.isPresent()) {
        sender().tell(new JobRejected(username, jobId, "Failed to connect to Hive."), self());
        return;
      }
      HdfsApi hdfsApi = hdfsApiOptional.get();

      subActor = system.actorOf(
        Props.create(AsyncJdbcConnector.class, viewContext, hdfsApi, system, self(),
          deathWatch, connectionSupplier.get(viewContext),
          storageSupplier.get(viewContext)).withDispatcher("akka.actor.jdbc-connector-dispatcher"),
        username + ":" + "jobId:" + jobId + ":" + UUID.randomUUID().toString() + ":asyncjdbcConnector");
      deathWatch.tell(new RegisterActor(subActor),self());

    }

    if (asyncBusyConnections.containsKey(username)) {
      Map<String, ActorRefResultContainer> actors = asyncBusyConnections.get(username);
      if (!actors.containsKey(jobId)) {
        actors.put(jobId, new ActorRefResultContainer(subActor));
      } else {
        // Reject this as with the same jobId one connection is already in progress.
        sender().tell(new JobRejected(username, jobId, "Existing job in progress with same jobId."), ActorRef.noSender());
      }
    } else {
      Map<String, ActorRefResultContainer> actors = new HashMap<>();
      actors.put(jobId, new ActorRefResultContainer(subActor));
      asyncBusyConnections.put(username, actors);
    }

    // set up the connect with ExecuteJob id for terminations
    subActor.tell(connect, self());
    subActor.tell(job, self());

  }

  private ActorRef getActorRefFromSyncPool(String username) {
    return getActorRefFromPool(syncAvailableConnections, username);
  }

  private ActorRef getActorRefFromAsyncPool(String username) {
    return getActorRefFromPool(asyncAvailableConnections, username);
  }

  private ActorRef getActorRefFromPool(Map<String, Queue<ActorRef>> pool, String username) {
    ActorRef subActor = null;
    if (pool.containsKey(username)) {
      Queue<ActorRef> availableActors = pool.get(username);
      if (availableActors.size() != 0) {
        subActor = availableActors.poll();
      }
    } else {
      pool.put(username, new LinkedList<ActorRef>());
    }
    return subActor;
  }

  private void sendSyncJob(Connect connect, HiveJob job) {
    String username = job.getUsername();
    ActorRef subActor = null;
    // Check if there is available actors to process this
    subActor = getActorRefFromSyncPool(username);
    ViewContext viewContext = job.getViewContext();

    if (subActor == null) {
      Optional<HdfsApi> hdfsApiOptional = hdfsApiSupplier.get(viewContext);
      if(!hdfsApiOptional.isPresent()){
          sender().tell(new JobRejected(username, ExecuteJob.SYNC_JOB_MARKER, "Failed to connect to HDFS."), ActorRef.noSender());
          return;
        }
      HdfsApi hdfsApi = hdfsApiOptional.get();

      subActor = system.actorOf(
        Props.create(SyncJdbcConnector.class, viewContext, hdfsApi, system, self(),
          deathWatch, connectionSupplier.get(viewContext),
          storageSupplier.get(viewContext)).withDispatcher("akka.actor.jdbc-connector-dispatcher"),
        username + ":" + UUID.randomUUID().toString() + ":SyncjdbcConnector" );
      deathWatch.tell(new RegisterActor(subActor),self());

    }

    if (syncBusyConnections.containsKey(username)) {
      Set<ActorRef> actors = syncBusyConnections.get(username);
      actors.add(subActor);
    } else {
      LinkedHashSet<ActorRef> actors = new LinkedHashSet<>();
      actors.add(subActor);
      syncBusyConnections.put(username, actors);
    }

    // Termination requires that the ref is known in case of sync jobs
    subActor.tell(connect, self());
    subActor.tell(job, sender());
  }


  private void destroyConnector(DestroyConnector message) {
    ActorRef sender = getSender();
    if (message.isForAsync()) {
      removeFromAsyncBusyPool(message.getUsername(), message.getJobId());
      removeFromASyncAvailable(message.getUsername(), sender);
    } else {
      removeFromSyncBusyPool(message.getUsername(), sender);
      removeFromSyncAvailable(message.getUsername(), sender);
    }
    logMaps();
  }

  private void freeConnector(FreeConnector message) {
    LOG.info("About to free connector for job {} and user {}",message.getJobId(),message.getUsername());
    ActorRef sender = getSender();
    if (message.isForAsync()) {
      Optional<ActorRef> refOptional = removeFromAsyncBusyPool(message.getUsername(), message.getJobId());
      if (refOptional.isPresent()) {
        addToAsyncAvailable(message.getUsername(), refOptional.get());
      }
      return;
    }
    // Was a sync job, remove from sync pool
    Optional<ActorRef> refOptional = removeFromSyncBusyPool(message.getUsername(), sender);
    if (refOptional.isPresent()) {
      addToSyncAvailable(message.getUsername(), refOptional.get());
    }


    logMaps();

  }

  private void logMaps() {
    LOG.info("Pool status");
    LoggingOutputStream out = new LoggingOutputStream(LOG, LoggingOutputStream.LogLevel.INFO);
    MapUtils.debugPrint(new PrintStream(out), "Busy Async connections", asyncBusyConnections);
    MapUtils.debugPrint(new PrintStream(out), "Available Async connections", asyncAvailableConnections);
    MapUtils.debugPrint(new PrintStream(out), "Busy Sync connections", syncBusyConnections);
    MapUtils.debugPrint(new PrintStream(out), "Available Sync connections", syncAvailableConnections);
    try {
      out.close();
    } catch (IOException e) {
      LOG.warn("Cannot close Logging output stream, this may lead to leaks");
    }
  }

  private Optional<ActorRef> removeFromSyncBusyPool(String userName, ActorRef refToFree) {
    if (syncBusyConnections.containsKey(userName)) {
      Set<ActorRef> actorRefs = syncBusyConnections.get(userName);
      actorRefs.remove(refToFree);
    }
    return Optional.of(refToFree);
  }

  private Optional<ActorRef> removeFromAsyncBusyPool(String username, String jobId) {
    ActorRef ref = null;
    if (asyncBusyConnections.containsKey(username)) {
      Map<String, ActorRefResultContainer> actors = asyncBusyConnections.get(username);
      if (actors.containsKey(jobId)) {
        ref = actors.get(jobId).actorRef;
        actors.remove(jobId);
      }
    }
    return Optional.fromNullable(ref);
  }

  private void addToAsyncAvailable(String username, ActorRef actor) {
    addToAvailable(asyncAvailableConnections, username, actor);
  }

  private void addToSyncAvailable(String username, ActorRef actor) {
    addToAvailable(syncAvailableConnections, username, actor);
  }

  private void addToAvailable(Map<String, Queue<ActorRef>> pool, String username, ActorRef actor) {
    if (!pool.containsKey(username)) {
      pool.put(username, new LinkedList<ActorRef>());
    }

    Queue<ActorRef> availableActors = pool.get(username);
    availableActors.add(actor);
  }

  private void removeFromASyncAvailable(String username, ActorRef sender) {
    removeFromAvailable(asyncAvailableConnections, username, sender);
  }

  private void removeFromSyncAvailable(String username, ActorRef sender) {
    removeFromAvailable(syncAvailableConnections, username, sender);
  }

  private void removeFromAvailable(Map<String, Queue<ActorRef>> pool, String username, ActorRef sender) {
    if (!pool.containsKey(username)) {
      return;
    }
    Queue<ActorRef> actors = pool.get(username);
    actors.remove(sender);
  }

  private static class ActorRefResultContainer {

    ActorRef actorRef;
    boolean hasError = false;
    Either<ActorRef, ActorRef> result = Either.none();
    AsyncExecutionFailed error;

    public ActorRefResultContainer(ActorRef actorRef) {
      this.actorRef = actorRef;
    }
  }


}


