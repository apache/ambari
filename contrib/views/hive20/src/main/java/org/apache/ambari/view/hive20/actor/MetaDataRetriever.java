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

import akka.actor.Props;
import com.google.common.base.Optional;
import org.apache.ambari.view.hive20.actor.message.HiveMessage;
import org.apache.ambari.view.hive20.internal.Connectable;
import org.apache.ambari.view.hive20.internal.ConnectionException;
import org.apache.ambari.view.hive20.internal.dto.DatabaseInfo;
import org.apache.ambari.view.hive20.internal.dto.TableInfo;
import org.apache.hive.jdbc.HiveConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class MetaDataRetriever extends HiveActor {

  private final Logger LOG = LoggerFactory.getLogger(getClass());

  private final Connectable connectable;

  public MetaDataRetriever(Connectable connectable) {
    this.connectable = connectable;
  }

  @Override
  public void handleMessage(HiveMessage hiveMessage) {
    Object message = hiveMessage.getMessage();
    if (message instanceof RefreshDB) {
      handleRefreshDB();
    }
  }

  private void handleRefreshDB() {
    try {
      refreshDatabaseInfos();
    } catch (ConnectionException | SQLException e) {
      LOG.error("Failed to update the complete database information. Exception: {}", e);
      getSender().tell(new DBRefreshFailed(e), getSelf());
    }
  }

  private HiveConnection getHiveConnection() throws ConnectionException {
    if (!connectable.isOpen()) {
      connectable.connect();
    }
    Optional<HiveConnection> connectionOptional = connectable.getConnection();
    return connectionOptional.get();
  }

  private void refreshDatabaseInfos() throws ConnectionException, SQLException {
    HiveConnection connection = getHiveConnection();
    Set<DatabaseInfo> infos = new HashSet<>();
    try (ResultSet schemas = connection.getMetaData().getSchemas()) {
      while (schemas.next()) {
        DatabaseInfo info = new DatabaseInfo(schemas.getString(1));
        infos.add(info);
      }
    }

    getSender().tell(new DBRefreshed(infos), getSelf());

    for (DatabaseInfo info : infos) {
      refreshTablesInfo(info.getName());
    }
  }

  private void refreshTablesInfo(String database) throws ConnectionException, SQLException {
    HiveConnection connection = getHiveConnection();
    Set<String> currentTableNames = new HashSet<>();
    try (ResultSet tables = connection.getMetaData().getTables("", database, null, null)) {
      while (tables.next()) {
        TableInfo info = new TableInfo(tables.getString(3), tables.getString(4));
        currentTableNames.add(info.getName());
        getSender().tell(new TableRefreshed(info, database), getSelf());
      }
    }
    getSender().tell(new AllTableRefreshed(database, currentTableNames), getSelf());
  }

  public static  Props props(Connectable connectable) {
    return Props.create(MetaDataRetriever.class, connectable);
  }


  public static class RefreshDB {

  }

  public static class DBRefreshed {
    private final Set<DatabaseInfo> databases;

    public DBRefreshed(Set<DatabaseInfo> databases) {
      this.databases = databases;
    }

    public Set<DatabaseInfo> getDatabases() {
      return databases;
    }
  }

  public static class DBRefreshFailed {
    private final Exception exception;

    public DBRefreshFailed(Exception exception) {
      this.exception = exception;
    }

    public Exception getException() {
      return exception;
    }
  }

  public static  class TableRefreshed {
    private final TableInfo table;
    private final String database;

    public TableRefreshed(TableInfo table, String database) {
      this.table = table;
      this.database = database;
    }

    public TableInfo getTable() {
      return table;
    }

    public String getDatabase() {
      return database;
    }
  }

  public static class AllTableRefreshed {
    private final String database;
    private final Set<String> currentTableNames;

    public AllTableRefreshed(String database, Set<String> currentTableNames) {
      this.database = database;
      this.currentTableNames = currentTableNames;
    }

    public String getDatabase() {
      return database;
    }

    public Set<String> getCurrentTableNames() {
      return currentTableNames;
    }
  }
}
