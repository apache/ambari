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
package org.apache.ambari.metrics.core.timeline.upgrade.core;

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
  private static final Long DEFAULT_NATIVE_TIME_RANGE_DELAY = 120000L;
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
    LOG.info(String.format("Copying %s metrics from %s to %s", metricNames, inputTable, outputTable));
    long startTimer = System.currentTimeMillis();
    String query = String.format("SELECT %s %s FROM %s WHERE %s AND SERVER_TIME > %s ORDER BY METRIC_NAME, SERVER_TIME",
      getQueryHint(startTime), getColumnsClause(), inputTable, getMetricNamesLikeClause(), startTime);

    runPhoenixQueryAndAddToResults(query);

    try {
      saveMetrics();
    } catch (SQLException e) {
      LOG.error(e);
    }
    long estimatedTime = System.currentTimeMillis() - startTimer;
    LOG.debug(String.format("Copying took %s seconds from table %s to table %s for metric names %s", estimatedTime/ 1000.0, inputTable, outputTable, metricNames));

    saveMetricsProgress();
  }

  private String getMetricNamesLikeClause() {
    StringBuilder sb = new StringBuilder();
    sb.append('(');
    int i = 0;
    for (String metricName : metricNames) {
      sb.append("METRIC_NAME");
      sb.append(" LIKE ");
      sb.append("'");
      sb.append(metricName);
      sb.append("'");

      if (i < metricNames.size() - 1) {
          sb.append(" OR ");
        }
      i++;
    }

    sb.append(')');
    return sb.toString();
  }

  protected abstract String getColumnsClause();

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
      LOG.error(String.format("Exception during running phoenix query %s", query), e);
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
   */
  private void saveMetricsProgress() {
    if (processedMetricsFile == null) {
      LOG.info("Skipping metrics progress save as the file is null");
      return;
    }
    for (String metricName : metricNames) {
      try {
        processedMetricsFile.append(inputTable + ":" + metricName + System.lineSeparator());
      } catch (IOException e) {
        LOG.error(e);
      }
    }
  }

  protected String getQueryHint(Long startTime) {
    StringBuilder sb = new StringBuilder();
    sb.append("/*+ ");
    sb.append("NATIVE_TIME_RANGE(");
    sb.append(startTime - DEFAULT_NATIVE_TIME_RANGE_DELAY);
    sb.append(") ");
    sb.append("*/");
    return sb.toString();
  }

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
