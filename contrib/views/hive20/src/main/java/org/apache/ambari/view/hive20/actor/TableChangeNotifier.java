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
import org.apache.ambari.view.hive20.actor.message.HiveMessage;
import org.apache.ambari.view.hive20.internal.dto.TableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class TableChangeNotifier extends HiveActor {
  private final Logger LOG = LoggerFactory.getLogger(getClass());

  @Override
  public void handleMessage(HiveMessage hiveMessage) {
    Object message = hiveMessage.getMessage();
    if(message instanceof TableUpdated) {
      handleTableUpdated((TableUpdated) message);
    } else if(message instanceof TableAdded) {
      handleTableAdded((TableAdded) message);
    } else if(message instanceof TableRemoved) {
      handleTableRemoved((TableRemoved) message);
    }
  }

  private void handleTableUpdated(TableUpdated message) {
    LOG.info("Tables updated for table name: {}", message.getTableInfo().getName());
  }

  private void handleTableAdded(TableAdded message) {
    LOG.info("Tables added for table name: {}", message.getTableInfo().getName());
  }

  private void handleTableRemoved(TableRemoved message) {
    LOG.info("Tables removed for table name: {}", message.getTableName());
  }

  public static Props props() {
    return Props.create(TableChangeNotifier.class);
  }


  public static class TableAdded {
    private final TableInfo tableInfo;
    public TableAdded(TableInfo tableInfo) {
      this.tableInfo = tableInfo;
    }

    public TableInfo getTableInfo() {
      return tableInfo;
    }
  }

  public static class TableRemoved {
    private final String tableName;
    public TableRemoved(String tableName) {
      this.tableName = tableName;
    }

    public String getTableName() {
      return tableName;
    }
  }


  public static class TableUpdated {
    private final TableInfo tableInfo;
    public TableUpdated(TableInfo tableInfo) {
      this.tableInfo = tableInfo;
    }

    public TableInfo getTableInfo() {
      return tableInfo;
    }
  }
}
