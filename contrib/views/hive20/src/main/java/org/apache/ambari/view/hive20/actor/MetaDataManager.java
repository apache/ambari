/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive20.actor;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive20.actor.message.HiveMessage;
import org.apache.ambari.view.hive20.actor.message.Ping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Manages the Meta Information for Hive Server. Singleton actor which stores several DatabaseManagerActor in memory for
 * each user and instance name combination.
 */
public class MetaDataManager extends HiveActor {

  private final Logger LOG = LoggerFactory.getLogger(getClass());

  /**
   * Stores the sub database manager actors per user combination
   */
  private final Map<String, ActorRef> databaseManagers = new HashMap<>();
  private final Map<String, Cancellable> terminationSchedulers = new HashMap<>();
  private final ViewContext context;

  public MetaDataManager(ViewContext context) {
    this.context = context;
  }

  @Override
  public void handleMessage(HiveMessage hiveMessage) {

    Object message = hiveMessage.getMessage();
    if (message instanceof Ping) {
      handlePing((Ping) message);
    } else if (message instanceof Terminate) {
      handleTerminate((Terminate) message);
    } else if (message instanceof DatabaseManager.GetDatabases) {
      handleGetDatabases((DatabaseManager.GetDatabases) message);
    }
  }

  private void handlePing(Ping message) {
    LOG.info("Ping message received for user: {}, instance: {}", message.getUsername(), message.getInstanceName());
    ActorRef databaseManager = databaseManagers.get(message.getUsername());
    if (databaseManager == null) {
      databaseManager = createDatabaseManager(message.getUsername(), message.getInstanceName());
      databaseManagers.put(message.getUsername(), databaseManager);
      databaseManager.tell(new DatabaseManager.Refresh(message.getUsername()), getSelf());
    } else {
      if(message.isImmediate()) {
        databaseManager.tell(new DatabaseManager.Refresh(message.getUsername(), false), getSelf());
      }
      cancelTerminationScheduler(message.getUsername());
    }
    scheduleTermination(message.getUsername());
  }

  private void handleTerminate(Terminate message) {
    ActorRef databaseManager = databaseManagers.remove(message.username);
    getContext().stop(databaseManager);
    cancelTerminationScheduler(message.getUsername());
  }

  private void handleGetDatabases(DatabaseManager.GetDatabases message) {
    String username = message.getUsername();
    ActorRef databaseManager = databaseManagers.get(username);
    if(databaseManager != null) {
      databaseManager.tell(message, getSender());
    } else {
      // Not database Manager created. Start the database manager with a ping message
      // and queue up the GetDatabases call to self
      getSelf().tell(new Ping(username, context.getInstanceName()), getSender());
      getSelf().tell(message, getSender());
    }
  }

  private void cancelTerminationScheduler(String username) {
    Cancellable cancellable = terminationSchedulers.remove(username);
    if (!(cancellable == null || cancellable.isCancelled())) {
      LOG.info("Cancelling termination scheduler");
      cancellable.cancel();
    }
  }

  private void scheduleTermination(String username) {
    Cancellable cancellable = context().system().scheduler().scheduleOnce(Duration.create(2, TimeUnit.MINUTES),
        getSelf(), new Terminate(username), getContext().dispatcher(), getSelf());
    terminationSchedulers.put(username, cancellable);
  }

  private ActorRef createDatabaseManager(String username, String instanceName) {
    LOG.info("Creating database manager for username: {}, instance: {}", username, instanceName);
    return context().actorOf(DatabaseManager.props(context));
  }

  public static Props props(ViewContext viewContext) {
    return Props.create(MetaDataManager.class, viewContext);
  }

  private class Terminate {
    public final String username;

    public Terminate(String username) {
      this.username = username;
    }

    public String getUsername() {
      return username;
    }
  }

}
