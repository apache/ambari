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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;

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

public abstract class AbstractTimelineAggregator implements Runnable {
  protected final PhoenixHBaseAccessor hBaseAccessor;
  private final Log LOG;
  protected final long checkpointDelayMillis;
  protected final Integer resultsetFetchSize;
  protected Configuration metricsConf;

  public AbstractTimelineAggregator(PhoenixHBaseAccessor hBaseAccessor,
                                    Configuration metricsConf) {
    this.hBaseAccessor = hBaseAccessor;
    this.metricsConf = metricsConf;
    this.checkpointDelayMillis = SECONDS.toMillis(
      metricsConf.getInt(AGGREGATOR_CHECKPOINT_DELAY, 120));
    this.resultsetFetchSize = metricsConf.getInt(RESULTSET_FETCH_SIZE, 2000);
    this.LOG = LogFactory.getLog(this.getClass());
  }

  @Override
  public void run() {
    LOG.info("Started Timeline aggregator thread @ " + new Date());
    Long SLEEP_INTERVAL = getSleepIntervalMillis();

    while (true) {
      long currentTime = System.currentTimeMillis();
      long lastCheckPointTime = -1;

      try {
        lastCheckPointTime = readCheckPoint();
        if (isLastCheckPointTooOld(lastCheckPointTime)) {
          LOG.warn("Last Checkpoint is too old, discarding last checkpoint. " +
            "lastCheckPointTime = " + lastCheckPointTime);
          lastCheckPointTime = -1;
        }
        if (lastCheckPointTime == -1) {
          // Assuming first run, save checkpoint and sleep.
          // Set checkpoint to 2 minutes in the past to allow the
          // agents/collectors to catch up
          saveCheckPoint(currentTime - checkpointDelayMillis);
        }
      } catch (IOException io) {
        LOG.warn("Unable to write last checkpoint time. Resuming sleep.", io);
      }
      long sleepTime = SLEEP_INTERVAL;

      if (lastCheckPointTime != -1) {
        LOG.info("Last check point time: " + lastCheckPointTime + ", lagBy: "
          + ((System.currentTimeMillis() - lastCheckPointTime) / 1000)
          + " seconds.");

        long startTime = System.currentTimeMillis();
        boolean success = doWork(lastCheckPointTime,
          lastCheckPointTime + SLEEP_INTERVAL);
        long executionTime = System.currentTimeMillis() - startTime;
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
            saveCheckPoint(lastCheckPointTime + SLEEP_INTERVAL);
          } catch (IOException io) {
            LOG.warn("Error saving checkpoint, restarting aggregation at " +
              "previous checkpoint.");
          }
        }
      }

      try {
        Thread.sleep(sleepTime);
      } catch (InterruptedException e) {
        LOG.info("Sleep interrupted, continuing with aggregation.");
      }
    }
  }

  private boolean isLastCheckPointTooOld(long checkpoint) {
    return checkpoint != -1 &&
      ((System.currentTimeMillis() - checkpoint) >
        getCheckpointCutOffIntervalMillis());
  }

  private long readCheckPoint() {
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

  private void saveCheckPoint(long checkpointTime) throws IOException {
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
  protected boolean doWork(long startTime, long endTime) {
    LOG.info("Start aggregation cycle @ " + new Date() + ", " +
      "startTime = " + new Date(startTime) + ", endTime = " + new Date(endTime));

    boolean success = true;
    PhoenixTransactSQL.Condition condition =
      prepareMetricQueryCondition(startTime, endTime);

    Connection conn = null;
    PreparedStatement stmt = null;

    try {
      conn = hBaseAccessor.getConnection();
      stmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt(conn, condition);

      LOG.debug("Query issued @: " + new Date());
      ResultSet rs = stmt.executeQuery();
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

  protected abstract PhoenixTransactSQL.Condition
  prepareMetricQueryCondition(long startTime, long endTime);

  protected abstract void aggregate(ResultSet rs, long startTime, long endTime)
    throws IOException, SQLException;

  protected abstract Long getSleepIntervalMillis();

  protected abstract Integer getCheckpointCutOffMultiplier();

  protected Long getCheckpointCutOffIntervalMillis() {
    return getCheckpointCutOffMultiplier() * getSleepIntervalMillis();
  }

  protected abstract boolean isDisabled();

  protected abstract String getCheckpointLocation();

}
