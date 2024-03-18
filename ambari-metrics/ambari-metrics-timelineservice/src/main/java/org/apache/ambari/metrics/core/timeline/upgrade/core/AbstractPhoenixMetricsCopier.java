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
import org.apache.hadoop.metrics2.sink.timeline.MetricHostAggregate;

import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

public abstract class AbstractPhoenixMetricsCopier implements Runnable {
  private static final Log LOG = LogFactory.getLog(AbstractPhoenixMetricsCopier.class);
  private static final long DEFAULT_NATIVE_TIME_RANGE_DELAY = 120000L;
  private final long startTime;
  protected final Writer processedMetricsFile;
  protected String inputTable;
  protected String outputTable;
  protected Set<String> metricNames;
  protected PhoenixHBaseAccessor hBaseAccessor;

  public AbstractPhoenixMetricsCopier(String inputTableName, String outputTableName, PhoenixHBaseAccessor hBaseAccessor,
                                      Set<String> metricNames, long startTime, Writer outputStream) {
    this.inputTable = inputTableName;
    this.outputTable = outputTableName;
    this.hBaseAccessor = hBaseAccessor;
    this.metricNames = metricNames;
    this.startTime = startTime;
    this.processedMetricsFile = outputStream;
  }

  @Override
  public void run(){
    LOG.info(String.format("Copying %s metrics from %s to %s", metricNames, inputTable, outputTable));
    long timerStart = System.currentTimeMillis();
    String query = String.format("SELECT %s %s FROM %s WHERE %s AND SERVER_TIME > %s ORDER BY METRIC_NAME, SERVER_TIME",
      getQueryHint(startTime), getColumnsClause(), inputTable, getMetricNamesLikeClause(), startTime);

    runPhoenixQueryAndAddToResults(query);

    try {
      saveMetrics();
    } catch (SQLException e) {
      LOG.error(e);
    } finally {
      long timerDelta = System.currentTimeMillis() - timerStart;
      LOG.debug(String.format("Copying took %s seconds from table %s to table %s for metric names %s", timerDelta/ 1000.0, inputTable, outputTable, metricNames));

      saveMetricsProgress();
    }
  }

  private String getMetricNamesLikeClause() {
    StringBuilder sb = new StringBuilder(256);
    sb.append('(');
    int i = 0;
    for (String metricName : metricNames) {
      sb.append("METRIC_NAME LIKE '").append(metricName).append("'");
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
    try (Connection conn = hBaseAccessor.getConnection();
         PreparedStatement stmt = conn.prepareStatement(query)) {
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          addToResults(rs);
        }
      }
    } catch (SQLException e) {
      LOG.error(String.format("Exception during running phoenix query %s", query), e);
    }
  }

  /**
   * Saves processed metric names info provided file in format TABLE_NAME:METRIC_NAME
   */
  private void saveMetricsProgress() {
    if (this.processedMetricsFile == null) {
      LOG.info("Skipping metrics progress save as the file is null");
      return;
    }

    for (String metricName : metricNames) {
      try {
        synchronized (this.processedMetricsFile) {
          this.processedMetricsFile.append(inputTable).append(":").append(metricName).append(System.lineSeparator());
        }
      } catch (IOException e) {
        LOG.error(e);
      }
    }
  }

  protected String getQueryHint(long startTime) {
    return new StringBuilder().append("/*+ NATIVE_TIME_RANGE(").append(startTime - DEFAULT_NATIVE_TIME_RANGE_DELAY).append(") */").toString();
  }

  protected MetricHostAggregate extractMetricHostAggregate(ResultSet rs) throws SQLException {
    MetricHostAggregate metricHostAggregate = new MetricHostAggregate();
    metricHostAggregate.setSum(rs.getDouble("METRIC_SUM"));
    metricHostAggregate.setNumberOfSamples(rs.getLong("METRIC_COUNT"));
    metricHostAggregate.setMax(rs.getDouble("METRIC_MAX"));
    metricHostAggregate.setMin(rs.getDouble("METRIC_MIN"));
    return metricHostAggregate;
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
