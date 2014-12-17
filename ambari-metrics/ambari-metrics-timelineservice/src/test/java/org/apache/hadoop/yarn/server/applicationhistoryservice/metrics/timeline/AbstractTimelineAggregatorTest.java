package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics
  .timeline;

import junit.framework.Assert;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.util.Clock;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics
  .timeline.TimelineMetricConfiguration.AGGREGATOR_CHECKPOINT_DELAY;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics
  .timeline.TimelineMetricConfiguration.RESULTSET_FETCH_SIZE;

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
      protected boolean doWork(long startTime, long endTime) {
        startTimeInDoWork.set(startTime);
        endTimeInDoWork.set(endTime);
        actualRuns++;

        return true;
      }

      @Override
      protected PhoenixTransactSQL.Condition
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
      protected boolean isDisabled() {
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
    Assert.assertEquals("startTime should be zero", 0, startTimeInDoWork.get());
    Assert.assertEquals("endTime  should be zero", 0, endTimeInDoWork.get());
    Assert.assertEquals(0, checkPoint.get());
    Assert.assertEquals(sleep, sleepIntervalMillis);
    Assert.assertEquals("Do not aggregate on first run", 0, actualRuns);

    // exactly one sleepInterval
    clock.setTime(clock.getTime() + sleepIntervalMillis);
    sleep = agg.runOnce(sleepIntervalMillis);
    Assert.assertEquals("startTime", clock.getTime() -
        sleepIntervalMillis,
      startTimeInDoWork.get());
    Assert.assertEquals("endTime", clock.getTime(),
      endTimeInDoWork.get());
    Assert.assertEquals(clock.getTime(), checkPoint.get());
    Assert.assertEquals(sleep, sleepIntervalMillis);
    Assert.assertEquals(1, actualRuns);

    // exactly one sleepInterval
    clock.setTime(clock.getTime() + sleepIntervalMillis);
    sleep = agg.runOnce(sleepIntervalMillis);
    Assert.assertEquals("startTime", clock.getTime() -
        sleepIntervalMillis,
      startTimeInDoWork.get());
    Assert.assertEquals("endTime", clock.getTime(),
      endTimeInDoWork.get());
    Assert.assertEquals(clock.getTime(), checkPoint.get());
    Assert.assertEquals(sleep, sleepIntervalMillis);
    Assert.assertEquals(2, actualRuns);

    // checkpointCutOffMultiplier x sleepInterval - should pass,
    // it will aggregate only first part of the whole 2x interval
    // and sleep as usual (don't we need to skip some sleep?)
    //
    // effectively checkpoint will be one interval in the past,
    // so next run will
    clock.setTime(clock.getTime() + (checkpointCutOffMultiplier *
      sleepIntervalMillis));
    sleep = agg.runOnce(sleepIntervalMillis);
    Assert.assertEquals("startTime after 2xinterval", clock.getTime() -
        (checkpointCutOffMultiplier * sleepIntervalMillis),
      startTimeInDoWork.get());
    Assert.assertEquals("endTime after 2xinterval", clock.getTime() -
        sleepIntervalMillis,
      endTimeInDoWork.get());
    Assert.assertEquals("checkpoint after 2xinterval", clock.getTime() -
      sleepIntervalMillis, checkPoint.get());
    Assert.assertEquals(sleep, sleepIntervalMillis);
    Assert.assertEquals(3, actualRuns);

    // exactly one sleepInterval after one that lagged by one whole interval,
    // so it will do the previous one... and sleep as usual
    // no way to keep up
    clock.setTime(clock.getTime() + sleepIntervalMillis);
    sleep = agg.runOnce(sleepIntervalMillis);
    Assert.assertEquals("startTime ", clock.getTime() -
        (checkpointCutOffMultiplier * sleepIntervalMillis),
      startTimeInDoWork.get());
    Assert.assertEquals("endTime  ", clock.getTime() -
        sleepIntervalMillis,
      endTimeInDoWork.get());
    Assert.assertEquals("checkpoint ", clock.getTime() - sleepIntervalMillis,
      checkPoint.get());
    Assert.assertEquals(sleep, sleepIntervalMillis);
    Assert.assertEquals(4, actualRuns);


    // checkpointCutOffMultiplier x sleepInterval - in normal state should pass,
    // but the clock lags too much, so this will not execute aggregation
    // just update checkpoint to currentTime
    clock.setTime(clock.getTime() + (checkpointCutOffMultiplier *
      sleepIntervalMillis));
    sleep = agg.runOnce(sleepIntervalMillis);
    Assert.assertEquals(4, actualRuns);
    Assert.assertEquals("checkpoint after too much lag is reset to " +
        "current clock time",
      clock.getTime(), checkPoint.get());
    Assert.assertEquals(sleep, sleepIntervalMillis);


  }

  //testDoWorkOnInterruptedruns
// 1. On interrupted it can skip some metrics
//  testOnInterruption:
//    // if sleep is interrupted.. is it ok?
//    clock.setTime(10000);
//  sleep = agg.runOnce(sleepIntervalMillis);
//  Assert.assertEquals("startTime should be zero", 0,
//    startTimeInDoWork.get());
//  Assert.assertEquals("endTime  should be zero", 0, endTimeInDoWork.get()
//    + sleepIntervalMillis);
//
//  //if it is interrupted again:
//  clock.setTime(30000);
//  sleep = agg.runOnce(sleepIntervalMillis);
  //
  // 2. if it lags it can skip??
  //

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