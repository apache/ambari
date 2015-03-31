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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.AbstractTimelineAggregator;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.Condition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL;
import org.apache.hadoop.yarn.util.Clock;
import org.junit.Before;
import org.junit.Test;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;
import static junit.framework.Assert.assertEquals;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.AGGREGATOR_CHECKPOINT_DELAY;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration.RESULTSET_FETCH_SIZE;

public class AbstractTimelineAggregatorTest {

  private AbstractTimelineAggregator agg;
  TestClock clock = new TestClock();

  AtomicLong startTimeInDoWork;
  AtomicLong endTimeInDoWork;
  AtomicLong checkPoint;
  int actualRuns;

  long sleepIntervalMillis;
  int checkpointCutOffMultiplier;

  @Before
  public void setUp() throws Exception {
    sleepIntervalMillis = 30000l;
    checkpointCutOffMultiplier = 2;

    Configuration metricsConf = new Configuration();
    metricsConf.setInt(AGGREGATOR_CHECKPOINT_DELAY, 0);
    metricsConf.setInt(RESULTSET_FETCH_SIZE, 2000);

    startTimeInDoWork = new AtomicLong(0);
    endTimeInDoWork = new AtomicLong(0);
    checkPoint = new AtomicLong(-1);
    actualRuns = 0;

    agg = new AbstractTimelineAggregator(
      null, metricsConf, clock) {
      @Override
      public boolean doWork(long startTime, long endTime) {
        startTimeInDoWork.set(startTime);
        endTimeInDoWork.set(endTime);
        actualRuns++;

        return true;
      }

      @Override
      protected Condition
      prepareMetricQueryCondition(long startTime, long endTime) {
        return null;
      }

      @Override
      protected void aggregate(ResultSet rs, long startTime,
                               long endTime) throws IOException, SQLException {
      }

      @Override
      protected Long getSleepIntervalMillis() {
        return sleepIntervalMillis;
      }

      @Override
      protected Integer getCheckpointCutOffMultiplier() {
        return checkpointCutOffMultiplier;
      }

      @Override
      public boolean isDisabled() {
        return false;
      }

      @Override
      protected String getCheckpointLocation() {
        return "dummy_ckptFile";
      }

      protected long readCheckPoint() {
        return checkPoint.get();
      }

      @Override
      protected void saveCheckPoint(long checkpointTime) throws IOException {
        checkPoint.set(checkpointTime);
      }
    };


  }

  @Test
  public void testDoWorkOnZeroDelay() throws Exception {

    // starting at time 0;
    clock.setTime(0);

    long sleep = agg.runOnce(sleepIntervalMillis);
    assertEquals("startTime should be zero", 0, startTimeInDoWork.get());
    assertEquals("endTime  should be zero", 0, endTimeInDoWork.get());
    assertEquals(0, checkPoint.get());
    assertEquals(sleep, sleepIntervalMillis);
    assertEquals("Do not aggregate on first run", 0, actualRuns);

    // exactly one sleepInterval
    clock.setTime(clock.getTime() + sleepIntervalMillis);
    sleep = agg.runOnce(sleepIntervalMillis);
    assertEquals("startTime", clock.getTime() -
        sleepIntervalMillis,
      startTimeInDoWork.get());
    assertEquals("endTime", clock.getTime(),
      endTimeInDoWork.get());
    assertEquals(clock.getTime(), checkPoint.get());
    assertEquals(sleep, sleepIntervalMillis);
    assertEquals(1, actualRuns);

    // exactly one sleepInterval
    clock.setTime(clock.getTime() + sleepIntervalMillis);
    sleep = agg.runOnce(sleepIntervalMillis);
    assertEquals("startTime", clock.getTime() -
        sleepIntervalMillis,
      startTimeInDoWork.get());
    assertEquals("endTime", clock.getTime(),
      endTimeInDoWork.get());
    assertEquals(clock.getTime(), checkPoint.get());
    assertEquals(sleep, sleepIntervalMillis);
    assertEquals(2, actualRuns);

    // checkpointCutOffMultiplier x sleepInterval - should pass,
    // it will aggregate only first part of the whole 2x interval
    // and sleep as usual (don't we need to skip some sleep?)
    //
    // effectively checkpoint will be one interval in the past,
    // so next run will
    clock.setTime(clock.getTime() + (checkpointCutOffMultiplier *
      sleepIntervalMillis));
    sleep = agg.runOnce(sleepIntervalMillis);
    assertEquals("startTime after 2xinterval", clock.getTime() -
        (checkpointCutOffMultiplier * sleepIntervalMillis),
      startTimeInDoWork.get());
    assertEquals("endTime after 2xinterval", clock.getTime() -
        sleepIntervalMillis,
      endTimeInDoWork.get());
    assertEquals("checkpoint after 2xinterval", clock.getTime() -
      sleepIntervalMillis, checkPoint.get());
    assertEquals(sleep, sleepIntervalMillis);
    assertEquals(3, actualRuns);

    // exactly one sleepInterval after one that lagged by one whole interval,
    // so it will do the previous one... and sleep as usual
    // no way to keep up
    clock.setTime(clock.getTime() + sleepIntervalMillis);
    sleep = agg.runOnce(sleepIntervalMillis);
    assertEquals("startTime ", clock.getTime() -
        (checkpointCutOffMultiplier * sleepIntervalMillis),
      startTimeInDoWork.get());
    assertEquals("endTime  ", clock.getTime() -
        sleepIntervalMillis,
      endTimeInDoWork.get());
    assertEquals("checkpoint ", clock.getTime() - sleepIntervalMillis,
      checkPoint.get());
    assertEquals(sleep, sleepIntervalMillis);
    assertEquals(4, actualRuns);


    // checkpointCutOffMultiplier x sleepInterval - in normal state should pass,
    // but the clock lags too much, so this will not execute aggregation
    // just update checkpoint to currentTime
    clock.setTime(clock.getTime() + (checkpointCutOffMultiplier *
      sleepIntervalMillis));
    sleep = agg.runOnce(sleepIntervalMillis);
    assertEquals(4, actualRuns);
    assertEquals("checkpoint after too much lag is reset to " +
        "current clock time",
      clock.getTime(), checkPoint.get());
    assertEquals(sleep, sleepIntervalMillis);
  }

  @Test
  public void testDoWorkOnInterruptedRuns() throws Exception {
    // start at some non-zero arbitrarily selected time;
    int startingTime = 10000;

    // 1.
    clock.setTime(startingTime);
    long timeOfFirstStep = clock.getTime();
    long sleep = agg.runOnce(sleepIntervalMillis);
    assertEquals("startTime should be zero", 0, startTimeInDoWork.get());
    assertEquals("endTime  should be zero", 0, endTimeInDoWork.get());
    assertEquals("do not aggregate on first run", 0, actualRuns);
    assertEquals("first checkpoint set on current time", timeOfFirstStep,
      checkPoint.get());
    assertEquals(sleep, sleepIntervalMillis);

    // 2.
    // the doWork was fast, and sleep was interrupted (e.g. restart)
    // Q: do we want to aggregate just part of the system? maybe we should
    // sleep up to next cycle start!!
    clock.setTime(timeOfFirstStep + 1);
    long timeOfSecondStep = clock.getTime();
    sleep = agg.runOnce(sleepIntervalMillis);
    assertEquals("startTime should be on previous checkpoint since it did not" +
        " run yet",
      timeOfFirstStep, startTimeInDoWork.get());

    assertEquals("endTime can be start + interval",
      startingTime + sleepIntervalMillis,
      endTimeInDoWork.get());
    assertEquals("should aggregate", 1, actualRuns);
    assertEquals("checkpoint here should be set to min(endTime,currentTime), " +
        "it is currentTime in our scenario",
      timeOfSecondStep, checkPoint.get());

    assertEquals(sleep, sleepIntervalMillis);

    //3.
    // and again not a full sleep passed, so only small part was aggregated
    clock.setTime(startingTime + 2);
    long timeOfThirdStep = clock.getTime();

    sleep = agg.runOnce(sleepIntervalMillis);
    // startTime and endTime are both be in the future, makes no sens,
    // query will not work!!
    assertEquals("startTime should be previous checkpoint",
      timeOfSecondStep, startTimeInDoWork.get());

    assertEquals("endTime  can be start + interval",
      timeOfSecondStep + sleepIntervalMillis,
      endTimeInDoWork.get());
    assertEquals("should aggregate", 2, actualRuns);
    assertEquals("checkpoint here should be set to min(endTime,currentTime), " +
        "it is currentTime in our scenario",
      timeOfThirdStep,
      checkPoint.get());
    assertEquals(sleep, sleepIntervalMillis);

  }

  private static class TestClock implements Clock {

    private long time;

    public void setTime(long time) {
      this.time = time;
    }

    @Override
    public long getTime() {
      return time;
    }
  }
}