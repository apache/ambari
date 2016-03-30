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
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixHBaseAccessor;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.Condition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
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
  protected final Logger LOG;
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

  // Explicitly name aggregators for logging needs
  private final String aggregatorName;

  AbstractTimelineAggregator(String aggregatorName,
                             PhoenixHBaseAccessor hBaseAccessor,
                             Configuration metricsConf) {
    this.aggregatorName = aggregatorName;
    this.hBaseAccessor = hBaseAccessor;
    this.metricsConf = metricsConf;
    this.checkpointDelayMillis = SECONDS.toMillis(metricsConf.getInt(AGGREGATOR_CHECKPOINT_DELAY, 120));
    this.resultsetFetchSize = metricsConf.getInt(RESULTSET_FETCH_SIZE, 2000);
    this.LOG = LoggerFactory.getLogger(aggregatorName);
  }

  public AbstractTimelineAggregator(String aggregatorName,
                                    PhoenixHBaseAccessor hBaseAccessor,
                                    Configuration metricsConf,
                                    String checkpointLocation,
                                    Long sleepIntervalMillis,
                                    Integer checkpointCutOffMultiplier,
                                    String aggregatorDisableParam,
                                    String tableName,
                                    String outputTableName,
                                    Long nativeTimeRangeDelay) {
    this(aggregatorName, hBaseAccessor, metricsConf);
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
    runOnce(SLEEP_INTERVAL);
  }

  /**
   * Access relaxed for tests
   */
  public void runOnce(Long SLEEP_INTERVAL) {

    long currentTime = System.currentTimeMillis();
    long lastCheckPointTime = readLastCheckpointSavingOnFirstRun(currentTime);

    if (lastCheckPointTime != -1) {
      LOG.info("Last check point time: " + lastCheckPointTime + ", lagBy: "
        + ((currentTime - lastCheckPointTime) / 1000)
        + " seconds.");

      boolean success = doWork(lastCheckPointTime, lastCheckPointTime + SLEEP_INTERVAL);

      if (success) {
        try {
          saveCheckPoint(lastCheckPointTime + SLEEP_INTERVAL);
        } catch (IOException io) {
          LOG.warn("Error saving checkpoint, restarting aggregation at " +
            "previous checkpoint.");
        }
      }
    }
  }

  private long readLastCheckpointSavingOnFirstRun(long currentTime) {
    long lastCheckPointTime = -1;

    try {
      lastCheckPointTime = readCheckPoint();
      if (lastCheckPointTime != -1) {
        LOG.info("Last Checkpoint read : " + new Date(lastCheckPointTime));
        if (isLastCheckPointTooOld(currentTime, lastCheckPointTime)) {
          LOG.warn("Last Checkpoint is too old, discarding last checkpoint. " +
            "lastCheckPointTime = " + new Date(lastCheckPointTime));
          lastCheckPointTime = getRoundedAggregateTimeMillis(getSleepIntervalMillis()) - getSleepIntervalMillis();
          LOG.info("Saving checkpoint time. " + new Date((lastCheckPointTime)));
          saveCheckPoint(lastCheckPointTime);

        } else {

          if (lastCheckPointTime > 0) {
            lastCheckPointTime = getRoundedCheckPointTimeMillis(lastCheckPointTime, getSleepIntervalMillis());
            LOG.info("Rounded off checkpoint : " + new Date(lastCheckPointTime));
          }

          if (isLastCheckPointTooYoung(lastCheckPointTime)) {
            LOG.info("Last checkpoint too recent for aggregation. Sleeping for 1 cycle.");
            return -1; //Skip Aggregation this time around
          }
        }
      } else {
        /*
          No checkpoint. Save current rounded checkpoint and sleep for 1 cycle.
         */
        LOG.info("No checkpoint found");
        long firstCheckPoint = getRoundedAggregateTimeMillis(getSleepIntervalMillis());
        LOG.info("Saving checkpoint time. " + new Date((firstCheckPoint)));
        saveCheckPoint(firstCheckPoint);
      }
    } catch (IOException io) {
      LOG.warn("Unable to write last checkpoint time. Resuming sleep.", io);
    }
    return lastCheckPointTime;
  }

  private boolean isLastCheckPointTooOld(long currentTime, long checkpoint) {
    // first checkpoint is saved checkpointDelayMillis in the past,
    // so here we also need to take it into account
    return checkpoint != -1 &&
      ((currentTime - checkpoint) > getCheckpointCutOffIntervalMillis());
  }

  private boolean isLastCheckPointTooYoung(long checkpoint) {
    return checkpoint != -1 &&
      ((getRoundedAggregateTimeMillis(getSleepIntervalMillis()) <= checkpoint));
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
      LOG.debug("", io);
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
      if (condition.doUpdate()) {
        int rows = stmt.executeUpdate();
        conn.commit();
        LOG.info(rows + " row(s) updated.");
      } else {
        rs = stmt.executeQuery();
      }
      LOG.debug("Query returned @: " + new Date());

      aggregate(rs, startTime, endTime);
      LOG.info("End aggregation cycle @ " + new Date());

    } catch (SQLException | IOException e) {
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

  public Long getSleepIntervalMillis() {
    return sleepIntervalMillis;
  }

  public void setSleepIntervalMillis(Long sleepIntervalMillis) {
    this.sleepIntervalMillis = sleepIntervalMillis;
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

  public static long getRoundedCheckPointTimeMillis(long referenceTime, long aggregatorPeriod) {
    return referenceTime - (referenceTime % aggregatorPeriod);
  }

  public static long getRoundedAggregateTimeMillis(long aggregatorPeriod) {
    long currentTime = System.currentTimeMillis();
    return currentTime - (currentTime % aggregatorPeriod);
  }

}
