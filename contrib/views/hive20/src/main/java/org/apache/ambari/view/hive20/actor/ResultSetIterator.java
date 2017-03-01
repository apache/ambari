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
import com.google.common.collect.Lists;
import org.apache.ambari.view.hive20.actor.message.CursorReset;
import org.apache.ambari.view.hive20.actor.message.HiveMessage;
import org.apache.ambari.view.hive20.actor.message.ResetCursor;
import org.apache.ambari.view.hive20.actor.message.job.FetchFailed;
import org.apache.ambari.view.hive20.actor.message.job.Next;
import org.apache.ambari.view.hive20.actor.message.job.NoMoreItems;
import org.apache.ambari.view.hive20.actor.message.job.Result;
import org.apache.ambari.view.hive20.actor.message.lifecycle.CleanUp;
import org.apache.ambari.view.hive20.actor.message.lifecycle.KeepAlive;
import org.apache.ambari.view.hive20.client.ColumnDescription;
import org.apache.ambari.view.hive20.client.ColumnDescriptionShort;
import org.apache.ambari.view.hive20.client.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

public class ResultSetIterator extends HiveActor {
  private final Logger LOG = LoggerFactory.getLogger(getClass());

  private static final int DEFAULT_BATCH_SIZE = 100;
  public static final String NULL = "NULL";

  private final ActorRef parent;
  private final ResultSet resultSet;
  private final int batchSize;

  private List<ColumnDescription> columnDescriptions;
  private int columnCount;
  boolean async = false;
  private boolean metaDataFetched = false;

  public ResultSetIterator(ActorRef parent, ResultSet resultSet, int batchSize, boolean isAsync) {
    this.parent = parent;
    this.resultSet = resultSet;
    this.batchSize = batchSize;
    this.async = isAsync;
  }

  public ResultSetIterator(ActorRef parent, ResultSet resultSet) {
    this(parent, resultSet, DEFAULT_BATCH_SIZE, true);
  }

  public ResultSetIterator(ActorRef parent, ResultSet resultSet, boolean isAsync) {
    this(parent, resultSet, DEFAULT_BATCH_SIZE, isAsync);
  }

  @Override
  public void handleMessage(HiveMessage hiveMessage) {
    sendKeepAlive();
    Object message = hiveMessage.getMessage();
    if (message instanceof Next) {
      getNext();
    }
    if (message instanceof ResetCursor) {
      resetResultSet();
    }

    if (message instanceof KeepAlive) {
      sendKeepAlive();
    }
  }

  private void resetResultSet() {
    try {
      resultSet.beforeFirst();
      sender().tell(new CursorReset(), self());
    } catch (SQLException e) {
      LOG.error("Failed to reset the cursor", e);
      sender().tell(new FetchFailed("Failed to reset the cursor", e), self());
      cleanUpResources();
    }
  }

  private void sendKeepAlive() {
    LOG.debug("Sending a keep alive to {}", parent);
    parent.tell(new KeepAlive(), self());
  }

  private void getNext() {
    List<Row> rows = Lists.newArrayList();
    if (!metaDataFetched) {
      try {
        initialize();
      } catch (SQLException ex) {
        LOG.error("Failed to fetch metadata for the ResultSet", ex);
        sender().tell(new FetchFailed("Failed to get metadata for ResultSet", ex), self());
        cleanUpResources();
      }
    }
    int index = 0;
    try {
      // check batchsize first becaue resultSet.next() fetches the new row as well before returning true/false.
      while (index < batchSize && resultSet.next()) {
        index++;
        rows.add(getRowFromResultSet(resultSet));
      }

      if (index == 0) {
        // We have hit end of resultSet
        sender().tell(new NoMoreItems(columnDescriptions), self());
        if(!async) {
          cleanUpResources();
        }
      } else {
        Result result = new Result(rows, columnDescriptions);
        sender().tell(result, self());
      }

    } catch (SQLException ex) {
      LOG.error("Failed to fetch next batch for the Resultset", ex);
      sender().tell(new FetchFailed("Failed to fetch next batch for the Resultset", ex), self());
      cleanUpResources();
    }
  }

  private void cleanUpResources() {
    parent.tell(new CleanUp(), self());
  }

  private Row getRowFromResultSet(ResultSet resultSet) throws SQLException {
    Object[] values = new Object[columnCount];
    for (int i = 0; i < columnCount; i++) {
      values[i] = resultSet.getObject(i + 1);
    }
    return new Row(values);
  }

  private void initialize() throws SQLException {
    metaDataFetched = true;
    ResultSetMetaData metaData = resultSet.getMetaData();
    columnCount = metaData.getColumnCount();
    columnDescriptions = Lists.newArrayList();
    for (int i = 1; i <= columnCount; i++) {
      String columnName = metaData.getColumnName(i);
      String typeName = metaData.getColumnTypeName(i);
      ColumnDescription description = new ColumnDescriptionShort(columnName, typeName, i);
      columnDescriptions.add(description);
    }
  }
}
