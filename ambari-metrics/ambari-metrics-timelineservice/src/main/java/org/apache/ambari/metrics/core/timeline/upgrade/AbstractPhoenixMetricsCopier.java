/**
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
package org.apache.ambari.metrics.core.timeline.upgrade;

import org.apache.ambari.metrics.core.timeline.PhoenixHBaseAccessor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

public abstract class AbstractPhoenixMetricsCopier implements Runnable {
  private static final Log LOG = LogFactory.getLog(AbstractPhoenixMetricsCopier.class);
  private final Long startTime;
  protected final FileWriter processedMetricsFile;
  protected String inputTable;
  protected String outputTable;
  protected Set<String> metricNames;
  protected PhoenixHBaseAccessor hBaseAccessor;

  public AbstractPhoenixMetricsCopier(String inputTableName, String outputTableName, PhoenixHBaseAccessor hBaseAccessor,
                                      Set<String> metricNames, Long startTime, FileWriter outputStream) {
    this.inputTable = inputTableName;
    this.outputTable = outputTableName;
    this.hBaseAccessor = hBaseAccessor;
    this.metricNames = metricNames;
    this.startTime = startTime;
    this.processedMetricsFile = outputStream;
  }

  public void run(){
    long startTimer = System.currentTimeMillis();
    if (metricNames == null || metricNames.isEmpty()) {
      String query = String.format("SELECT * FROM %s WHERE SERVER_TIME > %s ORDER BY METRIC_NAME, SERVER_TIME", inputTable, startTime);
      runPhoenixQueryAndAddToResults(query);
    } else {
      for (String metricName : metricNames) {
//      TODO do batch selects
        String query = String.format("SELECT * FROM %s WHERE METRIC_NAME LIKE '%s' AND SERVER_TIME > %s ORDER BY METRIC_NAME, SERVER_TIME", inputTable, metricName, startTime);
        runPhoenixQueryAndAddToResults(query);
      }
    }

    try {
      saveMetrics();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    long estimatedTime = System.currentTimeMillis() - startTimer;
    LOG.info(String.format("Copying took %s seconds from table %s to table %s", estimatedTime/ 1000.0, inputTable, outputTable));

    saveMetricsProgress();
  }

  private void runPhoenixQueryAndAddToResults(String query) {
    LOG.debug(String.format("Running query: %s", query));
    Connection conn = null;
    PreparedStatement stmt = null;
    try {
      conn = hBaseAccessor.getConnection();
      stmt = conn.prepareStatement(query);
      ResultSet rs = stmt.executeQuery();
      while (rs.next()) {
        addToResults(rs);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      if (stmt != null) {
        try {
          stmt.close();
        } catch (SQLException e) {
          // Ignore
        }
      }
      if (conn != null) {
        try {
          conn.close();
        } catch (SQLException e) {
          // Ignore
        }
      }
    }
  }

  /**
   * Saves processed metric names info provided file in format TABLE_NAME:METRIC_NAME
   * If metric names are not defined calls saveMetricsProgressUsingResultNames()
   */
  private void saveMetricsProgress() {
    if (processedMetricsFile == null) {
      LOG.info("Skipping metrics progress save as the file is null");
      return;
    }
    if (metricNames == null || metricNames.isEmpty()) {
      saveMetricsProgressUsingResultNames();
    } else {
      for (String metricName : metricNames) {
        try {
          processedMetricsFile.append(inputTable + ":" + metricName + System.lineSeparator());
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * When metric names are not defined use this method to define metric names from the result set
   */
  protected abstract void saveMetricsProgressUsingResultNames();

  /**
   * Saves aggregated metrics to the Hbase
   * @throws SQLException
   */
  protected abstract void saveMetrics() throws SQLException;

  /**
   * Parses result set into aggregates map
   * @param rs
   * @throws SQLException
   */
  protected abstract void addToResults(ResultSet rs) throws SQLException;
}
