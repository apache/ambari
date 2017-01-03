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
import akka.actor.PoisonPill;
import akka.actor.Props;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive20.AuthParams;
import org.apache.ambari.view.hive20.ConnectionFactory;
import org.apache.ambari.view.hive20.actor.message.HiveMessage;
import org.apache.ambari.view.hive20.client.ConnectionConfig;
import org.apache.ambari.view.hive20.internal.Connectable;
import org.apache.ambari.view.hive20.internal.HiveConnectionWrapper;
import org.apache.ambari.view.hive20.internal.dto.DatabaseInfo;
import org.apache.ambari.view.hive20.internal.dto.TableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Manages database related state, queries Hive to get the list of databases and then manages state for each database.
 * Also, periodically updates the list of databases by calling hive.
 */
public class DatabaseManager extends HiveActor {

  private final Logger LOG = LoggerFactory.getLogger(getClass());

  private final Connectable connectable;

  private final ActorRef metaDataRetriever;
  private final String username;

  private boolean refreshInProgress = false;
  private boolean selfRefreshQueued = false;

  private Map<String, DatabaseWrapper> databases = new HashMap<>();
  private Set<String> databasesToUpdate;


  public DatabaseManager(String username, Connectable connectable) {
    this.username = username;
    this.connectable = connectable;
    metaDataRetriever = getContext().actorOf(MetaDataRetriever.props(connectable));
  }

  @Override
  public void handleMessage(HiveMessage hiveMessage) {

    Object message = hiveMessage.getMessage();
    if (message instanceof Refresh) {
      handleRefresh();
    } else if (message instanceof SelfRefresh) {
      handleSelfRefresh();
    } else if (message instanceof MetaDataRetriever.DBRefreshed) {
      handleDBRefreshed((MetaDataRetriever.DBRefreshed) message);
    } else if (message instanceof MetaDataRetriever.TableRefreshed) {
      handleTableRefreshed((MetaDataRetriever.TableRefreshed) message);
    } else if (message instanceof MetaDataRetriever.AllTableRefreshed) {
      handleAllTableRefeshed((MetaDataRetriever.AllTableRefreshed) message);
    } else if (message instanceof GetDatabases) {
      handleGetDatabases((GetDatabases) message);
    }

  }

  private void handleSelfRefresh() {
    if (refreshInProgress) {
      getContext().system().scheduler().scheduleOnce(Duration.create(500, TimeUnit.MILLISECONDS),
          getSelf(), new SelfRefresh(), getContext().dispatcher(), getSelf());
    } else {
      selfRefreshQueued = false;
      refresh();
    }
  }

  private void handleRefresh() {
    if (refreshInProgress && selfRefreshQueued) {
      return; // We will not honor refresh message when a refresh is going on and another self refresh is queued in mailbox
    } else if (refreshInProgress) {
      selfRefreshQueued = true; // If refresh is in progress, we will queue up only one refresh message.
      getContext().system().scheduler().scheduleOnce(Duration.create(500, TimeUnit.MILLISECONDS),
          getSelf(), new SelfRefresh(), getContext().dispatcher(), getSelf());
    } else {
      refresh();
    }
  }

  private void handleDBRefreshed(MetaDataRetriever.DBRefreshed message) {
    Set<DatabaseInfo> databasesInfos = message.getDatabases();
    Set<String> currentDatabases = new HashSet<>(databases.keySet());
    Set<String> newDatabases = FluentIterable.from(databasesInfos).transform(new Function<DatabaseInfo, String>() {
      @Nullable
      @Override
      public String apply(@Nullable DatabaseInfo databaseInfo) {
        return databaseInfo.getName();
      }
    }).toSet();

    databasesToUpdate = new HashSet<>(newDatabases);

    Set<String> databasesAdded = Sets.difference(newDatabases, currentDatabases);
    Set<String> databasesRemoved = Sets.difference(currentDatabases, newDatabases);

    updateDatabasesAdded(databasesAdded, databasesInfos);
    updateDatabasesRemoved(databasesRemoved);
  }

  private void updateDatabasesAdded(Set<String> databasesAdded, Set<DatabaseInfo> databasesInfos) {
    for (DatabaseInfo info : databasesInfos) {
      if (databasesAdded.contains(info.getName())) {
        DatabaseWrapper wrapper = new DatabaseWrapper(info);
        databases.put(info.getName(), wrapper);
        wrapper.getDatabaseNotifier().tell(new DatabaseChangeNotifier.DatabaseAdded(info.getName()), getSelf());
      }
    }
  }

  private void updateDatabasesRemoved(Set<String> databasesRemoved) {
    for (String database : databasesRemoved) {
      DatabaseWrapper wrapper = databases.remove(database);
      ActorRef notifier = wrapper.getDatabaseNotifier();
      notifier.tell(new DatabaseChangeNotifier.DatabaseRemoved(database), getSelf());
      notifier.tell(PoisonPill.getInstance(), getSelf());
    }
  }

  private void handleTableRefreshed(MetaDataRetriever.TableRefreshed message) {
    ActorRef databaseChangeNotifier = getDatabaseChangeNotifier(message.getDatabase());
    updateTable(message.getDatabase(), message.getTable());
    databaseChangeNotifier.tell(new DatabaseChangeNotifier.TableUpdated(message.getTable()), getSelf());
  }

  private void handleAllTableRefeshed(MetaDataRetriever.AllTableRefreshed message) {
    ActorRef databaseChangeNotifier = getDatabaseChangeNotifier(message.getDatabase());
    databaseChangeNotifier.tell(new DatabaseChangeNotifier.AllTablesUpdated(message.getDatabase()), getSelf());
    if (checkIfAllTablesOfAllDatabaseRefeshed(message)) {
      refreshInProgress = false;
    }
  }

  private void handleGetDatabases(GetDatabases message) {
    if (refreshInProgress) {
      // If currently refreshing, then schedule the same message after 500 milliseconds
      getContext().system().scheduler().scheduleOnce(Duration.create(500, TimeUnit.MILLISECONDS),
          getSelf(), message, getContext().dispatcher(), getSender());
      return;
    }
    Set<DatabaseInfo> infos = new HashSet<>();
    for (DatabaseWrapper wrapper : databases.values()) {
      infos.add(wrapper.getDatabase());
    }
    getSender().tell(new DatabasesResult(infos), getSelf());
  }

  private boolean checkIfAllTablesOfAllDatabaseRefeshed(MetaDataRetriever.AllTableRefreshed message) {
    databasesToUpdate.remove(message.getDatabase());
    return databasesToUpdate.isEmpty();
  }

  private ActorRef getDatabaseChangeNotifier(String databaseName) {
    DatabaseWrapper wrapper = databases.get(databaseName);
    ActorRef databaseChangeNotifier = null;
    if (wrapper != null) {
      databaseChangeNotifier = wrapper.getDatabaseNotifier();
    }
    return databaseChangeNotifier;
  }

  private void refresh() {
    LOG.info("Received refresh for user");
    refreshInProgress = true;
    metaDataRetriever.tell(new MetaDataRetriever.RefreshDB(), getSelf());

    scheduleRefreshAfter(1, TimeUnit.MINUTES);
  }

  private void scheduleRefreshAfter(long time, TimeUnit timeUnit) {
    getContext().system().scheduler().scheduleOnce(Duration.create(time, timeUnit),
        getSelf(), new Refresh(username), getContext().dispatcher(), getSelf());
  }

  @Override
  public void postStop() throws Exception {
    LOG.info("Database Manager stopped!!!");
    connectable.disconnect();
  }

  private void updateTable(String databaseName, TableInfo table) {
    DatabaseWrapper wrapper = databases.get(databaseName);
    if (wrapper != null) {
      DatabaseInfo info = wrapper.getDatabase();
      info.getTables().add(table);
    }
  }

  public static Props props(ViewContext context) {
    ConnectionConfig config = ConnectionFactory.create(context);
    Connectable connectable = new HiveConnectionWrapper(config.getJdbcUrl(), config.getUsername(), config.getPassword(), new AuthParams(context));
    return Props.create(DatabaseManager.class, config.getUsername(), connectable);
  }

  public static class Refresh {
    private final String username;

    public Refresh(String username) {
      this.username = username;
    }

    public String getUsername() {
      return username;
    }
  }

  private static class SelfRefresh {
  }

  private class DatabaseWrapper {
    private final DatabaseInfo database;
    private final ActorRef databaseNotifier;

    private DatabaseWrapper(DatabaseInfo database) {
      this.database = database;
      databaseNotifier = getContext().actorOf(DatabaseChangeNotifier.props());
    }

    public DatabaseInfo getDatabase() {
      return database;
    }

    public ActorRef getDatabaseNotifier() {
      return databaseNotifier;
    }
  }

  public static class GetDatabases {
    private final String username;

    public GetDatabases(String username) {
      this.username = username;
    }

    public String getUsername() {
      return username;
    }
  }

  public static class DatabasesResult {
    private final Set<DatabaseInfo> databases;

    public DatabasesResult(Set<DatabaseInfo> databases) {
      this.databases = databases;
    }

    public Set<DatabaseInfo> getDatabases() {
      return databases;
    }
  }
}
