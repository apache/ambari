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
import com.google.common.collect.Sets;
import org.apache.ambari.view.hive20.actor.message.HiveMessage;
import org.apache.ambari.view.hive20.internal.dto.TableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class DatabaseChangeNotifier extends HiveActor {
  private final Logger LOG = LoggerFactory.getLogger(getClass());

  private String currentDatabaseName;
  private Map<String, TableWrapper> tables = new HashMap<>();
  private Map<String, TableInfo> newTables = new HashMap<>();

  @Override
  public void handleMessage(HiveMessage hiveMessage) {
    Object message = hiveMessage.getMessage();
    if(message instanceof DatabaseAdded) {
      handleDatabaseAdded((DatabaseAdded) message);
    } else if ( message instanceof DatabaseRemoved) {
      handleDatabaseRemoved((DatabaseRemoved) message);
    } else if (message instanceof TableUpdated) {
      handleTableUpdated((TableUpdated) message);
    } else if (message instanceof AllTablesUpdated) {
      handleAllTableUpdated((AllTablesUpdated) message);
    }
  }

  private void handleDatabaseAdded(DatabaseAdded message) {
    LOG.info("Database Added: {}", message.name);
    currentDatabaseName = message.name;
    // TODO: Send event to eventbus
  }

  private void handleDatabaseRemoved(DatabaseRemoved message) {
    LOG.info("Database Removed: {}", message.name);
    // TODO: Send event to eventbus
  }

  private void handleTableUpdated(TableUpdated message) {
    LOG.info("XXXXX: table xxxx. Size: {}", newTables.size());
    newTables.put(message.info.getName(), message.info);
  }

  private void handleAllTableUpdated(AllTablesUpdated message) {
    Set<String> oldTableNames = new HashSet<>(tables.keySet());
    Set<String> newTableNames = new HashSet<>(newTables.keySet());

    Set<String> tablesAdded = Sets.difference(newTableNames, oldTableNames);
    Set<String> tablesRemoved = Sets.difference(oldTableNames, newTableNames);
    Set<String> tablesUpdated = Sets.intersection(oldTableNames, newTableNames);

    updateTablesAdded(tablesAdded);
    updateTablesRemoved(tablesRemoved);
    updateTablesUpdated(tablesUpdated);
    newTables.clear();
  }

  private void updateTablesAdded(Set<String> tablesAdded) {
    for (String tableName: tablesAdded) {
      TableWrapper wrapper = new TableWrapper(tableName);
      tables.put(tableName, wrapper);
      wrapper.getTableNotifier().tell(new TableChangeNotifier.TableAdded(newTables.get(tableName)), getSelf());
    }
  }

  private void updateTablesRemoved(Set<String> tablesRemoved) {
    for(String tableName: tablesRemoved) {
      TableWrapper tableWrapper = tables.remove(tableName);
      tableWrapper.getTableNotifier().tell(new TableChangeNotifier.TableRemoved(tableName), getSelf());
      tableWrapper.getTableNotifier().tell(PoisonPill.getInstance(), getSelf());
    }
  }

  private void updateTablesUpdated(Set<String> tablesUpdated) {
    for(String tableName: tablesUpdated) {
      TableWrapper tableWrapper = tables.get(tableName);
      // TODO: Check what needs to be done here.
    }
  }

  public static Props props() {
    return Props.create(DatabaseChangeNotifier.class);
  }

  public class TableWrapper {
    private final String tableName;
    private final ActorRef tableNotifier;

    private TableWrapper(String tableName) {
      this.tableName = tableName;
      this.tableNotifier = getContext().actorOf(TableChangeNotifier.props());
    }

    public String getTableName() {
      return tableName;
    }

    public ActorRef getTableNotifier() {
      return tableNotifier;
    }
  }

  public static class DatabaseAdded {
    private final String name;

    public DatabaseAdded(String name) {
      this.name = name;
    }
  }


  public static class DatabaseRemoved {
    private final String name;

    public DatabaseRemoved(String name) {
      this.name = name;
    }
  }

  public static class TableUpdated {
    private final TableInfo info;

    public TableUpdated(TableInfo info) {
      this.info = info;
    }
  }

  public static class AllTablesUpdated {
    private final String database;

    public AllTablesUpdated(String database) {
      this.database = database;
    }
  }


}
