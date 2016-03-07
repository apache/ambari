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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.Condition;
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

  AtomicLong startTimeInDoWork;
  AtomicLong endTimeInDoWork;
  AtomicLong checkPoint;
  int actualRuns;

  long sleepIntervalMillis;
  int checkpointCutOffMultiplier;

  @Before
  public void setUp() throws Exception {
    sleepIntervalMillis = 5*60*1000l; //5 minutes
    checkpointCutOffMultiplier = 2;

    Configuration metricsConf = new Configuration();
    metricsConf.setInt(AGGREGATOR_CHECKPOINT_DELAY, 0);
    metricsConf.setInt(RESULTSET_FETCH_SIZE, 2000);

    startTimeInDoWork = new AtomicLong(0);
    endTimeInDoWork = new AtomicLong(0);
    checkPoint = new AtomicLong(-1);
    actualRuns = 0;

    agg = new AbstractTimelineAggregator("TimelineAggregatorTest", null, metricsConf) {
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
      public Long getSleepIntervalMillis() {
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

    long currentTime = System.currentTimeMillis();
    long roundedOffAggregatorTime = AbstractTimelineAggregator.getRoundedCheckPointTimeMillis(currentTime,
      sleepIntervalMillis);
    
    //Test first run of aggregator with no checkpoint
    checkPoint.set(-1);
    agg.setLastAggregatedEndTime(-1l);
    agg.runOnce(sleepIntervalMillis);
    assertEquals("startTime should be zero", 0, startTimeInDoWork.get());
    assertEquals("endTime  should be zero", 0, endTimeInDoWork.get());
    assertEquals(roundedOffAggregatorTime, checkPoint.get());
    assertEquals("Do not aggregate on first run", 0, actualRuns);

    //Test first run with Too Old checkpoint
    currentTime = System.currentTimeMillis();
    checkPoint.set(currentTime - 16*60*1000); //Old checkpoint
    agg.setLastAggregatedEndTime(-1l);
    agg.runOnce(sleepIntervalMillis);
    assertEquals("startTime should be zero", 0, startTimeInDoWork.get());
    assertEquals("endTime  should be zero", 0, endTimeInDoWork.get());
    assertEquals(roundedOffAggregatorTime, checkPoint.get());
    assertEquals("Do not aggregate on first run", 0, actualRuns);

    //Test first run with too "recent" checkpoint
    currentTime = System.currentTimeMillis();
    checkPoint.set(currentTime);
    agg.setLastAggregatedEndTime(-1l);
    agg.setSleepIntervalMillis(sleepIntervalMillis);
    agg.runOnce(sleepIntervalMillis);
    assertEquals("startTime should be zero", 0, startTimeInDoWork.get());
    assertEquals("endTime  should be zero", 0, endTimeInDoWork.get());
    assertEquals(agg.getLastAggregatedEndTime(), checkPoint.get());
    assertEquals("Do not aggregate on first run", 0, actualRuns);

    //Test first run with perfect checkpoint (sleepIntervalMillis back)
    currentTime = System.currentTimeMillis();
    roundedOffAggregatorTime = AbstractTimelineAggregator.getRoundedCheckPointTimeMillis(currentTime,
      sleepIntervalMillis);
    long checkPointTime = roundedOffAggregatorTime - sleepIntervalMillis;
    long expectedCheckPoint = AbstractTimelineAggregator.getRoundedCheckPointTimeMillis(checkPointTime, sleepIntervalMillis);
    checkPoint.set(checkPointTime);
    agg.setLastAggregatedEndTime(-1l);
    agg.runOnce(sleepIntervalMillis);
    assertEquals("startTime should the lower rounded time of the checkpoint time",
      expectedCheckPoint, startTimeInDoWork.get());
    assertEquals("endTime should the lower rounded time of the checkpoint time + sleepIntervalMillis",
      expectedCheckPoint + sleepIntervalMillis, endTimeInDoWork.get());
    assertEquals(expectedCheckPoint + sleepIntervalMillis,
      checkPoint.get());
    assertEquals("Aggregate on first run", 1, actualRuns);

    //Test edge case for checkpoint (2 x sleepIntervalMillis)
    checkPointTime = roundedOffAggregatorTime - 2*sleepIntervalMillis;
    expectedCheckPoint = AbstractTimelineAggregator.getRoundedCheckPointTimeMillis(checkPointTime, sleepIntervalMillis);
    checkPoint.set(checkPointTime);
    agg.runOnce(sleepIntervalMillis);
    assertEquals("startTime should the lower rounded time of the checkpoint time",
      expectedCheckPoint, startTimeInDoWork.get());
    assertEquals("startTime should the lower rounded time of the checkpoint time + sleepIntervalMillis",
      expectedCheckPoint + sleepIntervalMillis, endTimeInDoWork.get());
    assertEquals(expectedCheckPoint + sleepIntervalMillis,
      checkPoint.get());
    assertEquals("Aggregate on second run", 2, actualRuns);


 }
}