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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixHBaseAccessor;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.Condition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL;
import org.apache.hadoop.yarn.util.Clock;
import org.apache.hadoop.yarn.util.SystemClock;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.AGGREGATOR_CHECKPOINT_DELAY;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.RESULTSET_FETCH_SIZE;

/**
 * Base class for all runnable aggregators. Provides common functions like
 * check pointing and scheduling.
 */
public abstract class AbstractTimelineAggregator implements TimelineMetricAggregator {
  protected final PhoenixHBaseAccessor hBaseAccessor;
  private final Log LOG;
  private Clock clock;
  protected final long checkpointDelayMillis;
  protected final Integer resultsetFetchSize;
  protected Configuration metricsConf;

  private String checkpointLocation;
  private Long sleepIntervalMillis;
  private Integer checkpointCutOffMultiplier;
  private String aggregatorDisableParam;
  protected String tableName;
  protected String outputTableName;
  protected Long nativeTimeRangeDelay;

  public AbstractTimelineAggregator(PhoenixHBaseAccessor hBaseAccessor,
                                    Configuration metricsConf, Clock clk) {
    this.hBaseAccessor = hBaseAccessor;
    this.metricsConf = metricsConf;
    this.checkpointDelayMillis = SECONDS.toMillis(
      metricsConf.getInt(AGGREGATOR_CHECKPOINT_DELAY, 120));
    this.resultsetFetchSize = metricsConf.getInt(RESULTSET_FETCH_SIZE, 2000);
    this.LOG = LogFactory.getLog(this.getClass());
    this.clock = clk;
  }

  public AbstractTimelineAggregator(PhoenixHBaseAccessor hBaseAccessor,
                                    Configuration metricsConf) {
    this(hBaseAccessor, metricsConf, new SystemClock());
  }

  public AbstractTimelineAggregator(PhoenixHBaseAccessor hBaseAccessor,
                                    Configuration metricsConf,
                                    String checkpointLocation,
                                    Long sleepIntervalMillis,
                                    Integer checkpointCutOffMultiplier,
                                    String aggregatorDisableParam,
                                    String tableName,
                                    String outputTableName,
                                    Long nativeTimeRangeDelay) {
    this(hBaseAccessor, metricsConf);
    this.checkpointLocation = checkpointLocation;
    this.sleepIntervalMillis = sleepIntervalMillis;
    this.checkpointCutOffMultiplier = checkpointCutOffMultiplier;
    this.aggregatorDisableParam = aggregatorDisableParam;
    this.tableName = tableName;
    this.outputTableName = outputTableName;
    this.nativeTimeRangeDelay =  nativeTimeRangeDelay;
  }

  @Override
  public void run() {
    LOG.info("Started Timeline aggregator thread @ " + new Date());
    Long SLEEP_INTERVAL = getSleepIntervalMillis();

    while (true) {
      long sleepTime = runOnce(SLEEP_INTERVAL);

      try {
        Thread.sleep(sleepTime);
      } catch (InterruptedException e) {
        LOG.info("Sleep interrupted, continuing with aggregation.");
      }
    }
  }

  /**
   * Access relaxed for tests
   */
  public long runOnce(Long SLEEP_INTERVAL) {
    long currentTime = clock.getTime();
    long lastCheckPointTime = readLastCheckpointSavingOnFirstRun(currentTime);
    long sleepTime = SLEEP_INTERVAL;

    if (lastCheckPointTime != -1) {
      LOG.info("Last check point time: " + lastCheckPointTime + ", lagBy: "
        + ((clock.getTime() - lastCheckPointTime) / 1000)
        + " seconds.");

      long startTime = clock.getTime();
      boolean success = doWork(lastCheckPointTime,
        lastCheckPointTime + SLEEP_INTERVAL);
      long executionTime = clock.getTime() - startTime;
      long delta = SLEEP_INTERVAL - executionTime;

      if (delta > 0) {
        // Sleep for (configured sleep - time to execute task)
        sleepTime = delta;
      } else {
        // No sleep because last run took too long to execute
        LOG.info("Aggregator execution took too long, " +
          "cancelling sleep. executionTime = " + executionTime);
        sleepTime = 1;
      }

      LOG.debug("Aggregator sleep interval = " + sleepTime);

      if (success) {
        try {
          // Comment to bug fix:
          // cannot just save lastCheckPointTime + SLEEP_INTERVAL,
          // it has to be verified so it is not a time in the future
          // checkpoint says what was aggregated, and there is no way
          // the future metrics were aggregated!
          saveCheckPoint(Math.min(currentTime, lastCheckPointTime +
            SLEEP_INTERVAL));
        } catch (IOException io) {
          LOG.warn("Error saving checkpoint, restarting aggregation at " +
            "previous checkpoint.");
        }
      }
    }

    return sleepTime;
  }

  private long readLastCheckpointSavingOnFirstRun(long currentTime) {
    long lastCheckPointTime = -1;

    try {
      lastCheckPointTime = readCheckPoint();
      if (isLastCheckPointTooOld(lastCheckPointTime)) {
        LOG.warn("Last Checkpoint is too old, discarding last checkpoint. " +
          "lastCheckPointTime = " + new Date(lastCheckPointTime));
        lastCheckPointTime = -1;
      }
      if (lastCheckPointTime == -1) {
        // Assuming first run, save checkpoint and sleep.
        // Set checkpoint to 2 minutes in the past to allow the
        // agents/collectors to catch up
        LOG.info("Saving checkpoint time on first run. " +
          new Date((currentTime - checkpointDelayMillis)));
        saveCheckPoint(currentTime - checkpointDelayMillis);
      }
    } catch (IOException io) {
      LOG.warn("Unable to write last checkpoint time. Resuming sleep.", io);
    }
    return lastCheckPointTime;
  }

  private boolean isLastCheckPointTooOld(long checkpoint) {
    // first checkpoint is saved checkpointDelayMillis in the past,
    // so here we also need to take it into account
    return checkpoint != -1 &&
      ((clock.getTime() - checkpoint - checkpointDelayMillis) >
        getCheckpointCutOffIntervalMillis());
  }

  protected long readCheckPoint() {
    try {
      File checkpoint = new File(getCheckpointLocation());
      if (checkpoint.exists()) {
        String contents = FileUtils.readFileToString(checkpoint);
        if (contents != null && !contents.isEmpty()) {
          return Long.parseLong(contents);
        }
      }
    } catch (IOException io) {
      LOG.debug(io);
    }
    return -1;
  }

  protected void saveCheckPoint(long checkpointTime) throws IOException {
    File checkpoint = new File(getCheckpointLocation());
    if (!checkpoint.exists()) {
      boolean done = checkpoint.createNewFile();
      if (!done) {
        throw new IOException("Could not create checkpoint at location, " +
          getCheckpointLocation());
      }
    }
    FileUtils.writeStringToFile(checkpoint, String.valueOf(checkpointTime));
  }

  /**
   * Read metrics written during the time interval and save the sum and total
   * in the aggregate table.
   *
   * @param startTime Sample start time
   * @param endTime Sample end time
   */
  @Override
  public boolean doWork(long startTime, long endTime) {
    LOG.info("Start aggregation cycle @ " + new Date() + ", " +
      "startTime = " + new Date(startTime) + ", endTime = " + new Date(endTime));

    boolean success = true;
    Condition condition = prepareMetricQueryCondition(startTime, endTime);

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;

    try {
      conn = hBaseAccessor.getConnection();
      // FLUME 2. aggregate and ignore the instance
      stmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt(conn, condition);

      LOG.debug("Query issued @: " + new Date());
      rs = stmt.executeQuery();
      LOG.debug("Query returned @: " + new Date());

      aggregate(rs, startTime, endTime);
      LOG.info("End aggregation cycle @ " + new Date());

    } catch (SQLException e) {
      LOG.error("Exception during aggregating metrics.", e);
      success = false;
    } catch (IOException e) {
      LOG.error("Exception during aggregating metrics.", e);
      success = false;
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          // Ignore
        }
      }
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
        } catch (SQLException sql) {
          // Ignore
        }
      }
    }

    LOG.info("End aggregation cycle @ " + new Date());
    return success;
  }

  protected abstract Condition prepareMetricQueryCondition(long startTime, long endTime);

  protected abstract void aggregate(ResultSet rs, long startTime, long endTime) throws IOException, SQLException;

  protected Long getSleepIntervalMillis() {
    return sleepIntervalMillis;
  }

  protected Integer getCheckpointCutOffMultiplier() {
    return checkpointCutOffMultiplier;
  }

  protected Long getCheckpointCutOffIntervalMillis() {
    return getCheckpointCutOffMultiplier() * getSleepIntervalMillis();
  }

  public boolean isDisabled() {
    return metricsConf.getBoolean(aggregatorDisableParam, false);
  }

  protected String getCheckpointLocation() {
    return checkpointLocation;
  }
}
