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
import org.apache.hadoop.metrics2.sink.timeline.MetricHostAggregate;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.AbstractMiniHBaseClusterTest;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.MetricTestHelper;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.Condition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.DefaultCondition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.MetricTestHelper.createEmptyTimelineMetric;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.GET_METRIC_AGGREGATE_ONLY_SQL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_AGGREGATE_DAILY_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_AGGREGATE_HOURLY_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_AGGREGATE_MINUTE_TABLE_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class ITMetricAggregator extends AbstractMiniHBaseClusterTest {

  @Test
  public void testShouldInsertMetrics() throws Exception {
    // GIVEN

    // WHEN
    long startTime = System.currentTimeMillis();
    TimelineMetrics metricsSent = prepareTimelineMetrics(startTime, "local");
    hdb.insertMetricRecords(metricsSent);

    Condition queryCondition = new DefaultCondition(null,
        Collections.singletonList("local"), null, null, startTime,
        startTime + (15 * 60 * 1000), null, null, false);
    TimelineMetrics recordRead = hdb.getMetricRecords(queryCondition, null);

    // THEN
    assertThat(recordRead.getMetrics()).hasSize(2)
      .extracting("metricName")
      .containsOnly("mem_free", "disk_free");

    assertThat(metricsSent.getMetrics())
      .usingElementComparator(TIME_IGNORING_COMPARATOR)
      .containsExactlyElementsOf(recordRead.getMetrics());
  }

  private Configuration getConfigurationForTest(boolean useGroupByAggregators) {
    Configuration configuration = new Configuration();
    configuration.set("timeline.metrics.service.use.groupBy.aggregators", String.valueOf(useGroupByAggregators));
    return configuration;
  }

  @Test
  public void testShouldAggregateMinuteProperly() throws Exception {
    // GIVEN
    TimelineMetricAggregator aggregatorMinute =
      TimelineMetricAggregatorFactory.createTimelineMetricAggregatorMinute(hdb,
        getConfigurationForTest(false), null, null);
    TimelineMetricReadHelper readHelper = new TimelineMetricReadHelper(false);

    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long minute = 60 * 1000;
    hdb.insertMetricRecords(prepareTimelineMetrics(startTime, "local"));
    hdb.insertMetricRecords(prepareTimelineMetrics(ctime += minute, "local"));
    hdb.insertMetricRecords(prepareTimelineMetrics(ctime += minute, "local"));
    hdb.insertMetricRecords(prepareTimelineMetrics(ctime += minute, "local"));
    hdb.insertMetricRecords(prepareTimelineMetrics(ctime += minute, "local"));

    // WHEN
    long endTime = startTime + 1000 * 60 * 4;
    boolean success = aggregatorMinute.doWork(startTime, endTime);

    //THEN
    Condition condition = new DefaultCondition(null, null, null, null, startTime,
      endTime, null, null, true);
    condition.setStatement(String.format(GET_METRIC_AGGREGATE_ONLY_SQL,
      METRICS_AGGREGATE_MINUTE_TABLE_NAME));

    PreparedStatement pstmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt(conn, condition);
    ResultSet rs = pstmt.executeQuery();
    MetricHostAggregate expectedAggregate =
      MetricTestHelper.createMetricHostAggregate(2.0, 0.0, 20, 15.0);

    int count = 0;
    while (rs.next()) {
      TimelineMetric currentMetric =
        readHelper.getTimelineMetricKeyFromResultSet(rs);
      MetricHostAggregate currentHostAggregate =
        readHelper.getMetricHostAggregateFromResultSet(rs);

      if ("disk_free".equals(currentMetric.getMetricName())) {
        assertEquals(2.0, currentHostAggregate.getMax());
        assertEquals(0.0, currentHostAggregate.getMin());
        assertEquals(20, currentHostAggregate.getNumberOfSamples());
        assertEquals(15.0, currentHostAggregate.getSum());
        assertEquals(15.0 / 20, currentHostAggregate.calculateAverage());
        count++;
      } else if ("mem_free".equals(currentMetric.getMetricName())) {
        assertEquals(2.0, currentHostAggregate.getMax());
        assertEquals(0.0, currentHostAggregate.getMin());
        assertEquals(20, currentHostAggregate.getNumberOfSamples());
        assertEquals(15.0, currentHostAggregate.getSum());
        assertEquals(15.0 / 20, currentHostAggregate.calculateAverage());
        count++;
      } else {
        fail("Unexpected entry");
      }
    }
    assertEquals("Two aggregated entries expected", 2, count);
  }

  @Test
   public void testShouldAggregateHourProperly() throws Exception {
    // GIVEN
    TimelineMetricAggregator aggregator =
      TimelineMetricAggregatorFactory.createTimelineMetricAggregatorHourly(hdb,
        getConfigurationForTest(false), null, null);
    TimelineMetricReadHelper readHelper = new TimelineMetricReadHelper(false);
    long startTime = System.currentTimeMillis();

    MetricHostAggregate expectedAggregate =
      MetricTestHelper.createMetricHostAggregate(2.0, 0.0, 20, 15.0);
    Map<TimelineMetric, MetricHostAggregate>
      aggMap = new HashMap<TimelineMetric,
      MetricHostAggregate>();

    int min_5 = 5 * 60 * 1000;
    long ctime = startTime - min_5;
    aggMap.put(createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(createEmptyTimelineMetric(ctime += min_5), expectedAggregate);

    hdb.saveHostAggregateRecords(aggMap, METRICS_AGGREGATE_MINUTE_TABLE_NAME);

    //WHEN
    long endTime = ctime + min_5;
    boolean success = aggregator.doWork(startTime, endTime);
    assertTrue(success);

    //THEN
    Condition condition = new DefaultCondition(null, null, null, null, startTime,
      endTime, null, null, true);
    condition.setStatement(String.format(GET_METRIC_AGGREGATE_ONLY_SQL, METRICS_AGGREGATE_HOURLY_TABLE_NAME));

    PreparedStatement pstmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt(conn, condition);
    ResultSet rs = pstmt.executeQuery();

    while (rs.next()) {
      TimelineMetric currentMetric =
        readHelper.getTimelineMetricKeyFromResultSet(rs);
      MetricHostAggregate currentHostAggregate =
        readHelper.getMetricHostAggregateFromResultSet(rs);

      if ("disk_used".equals(currentMetric.getMetricName())) {
        assertEquals(2.0, currentHostAggregate.getMax());
        assertEquals(0.0, currentHostAggregate.getMin());
        assertEquals(12 * 20, currentHostAggregate.getNumberOfSamples());
        assertEquals(12 * 15.0, currentHostAggregate.getSum());
        assertEquals(15.0 / 20, currentHostAggregate.calculateAverage());
      }
    }
  }

  @Test
  public void testMetricAggregateDaily() throws Exception {
    // GIVEN
    TimelineMetricAggregator aggregator =
      TimelineMetricAggregatorFactory.createTimelineMetricAggregatorDaily(hdb,
        getConfigurationForTest(false), null, null);
    TimelineMetricReadHelper readHelper = new TimelineMetricReadHelper(false);
    long startTime = System.currentTimeMillis();

    MetricHostAggregate expectedAggregate =
      MetricTestHelper.createMetricHostAggregate(2.0, 0.0, 20, 15.0);
    Map<TimelineMetric, MetricHostAggregate>
      aggMap = new HashMap<TimelineMetric, MetricHostAggregate>();

    int min_5 = 5 * 60 * 1000;
    long ctime = startTime - min_5;
    aggMap.put(createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(createEmptyTimelineMetric(ctime += min_5), expectedAggregate);

    hdb.saveHostAggregateRecords(aggMap, METRICS_AGGREGATE_HOURLY_TABLE_NAME);

    //WHEN
    long endTime = ctime + min_5;
    boolean success = aggregator.doWork(startTime, endTime);
    assertTrue(success);

    //THEN
    Condition condition = new DefaultCondition(null, null, null, null, startTime,
      endTime, null, null, true);
    condition.setStatement(String.format(GET_METRIC_AGGREGATE_ONLY_SQL, METRICS_AGGREGATE_DAILY_TABLE_NAME));

    PreparedStatement pstmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt(conn, condition);
    ResultSet rs = pstmt.executeQuery();

    while (rs.next()) {
      TimelineMetric currentMetric =
        readHelper.getTimelineMetricKeyFromResultSet(rs);
      MetricHostAggregate currentHostAggregate =
        readHelper.getMetricHostAggregateFromResultSet(rs);

      if ("disk_used".equals(currentMetric.getMetricName())) {
        assertEquals(2.0, currentHostAggregate.getMax());
        assertEquals(0.0, currentHostAggregate.getMin());
        assertEquals(12 * 20, currentHostAggregate.getNumberOfSamples());
        assertEquals(12 * 15.0, currentHostAggregate.getSum());
        assertEquals(15.0 / 20, currentHostAggregate.calculateAverage());
      }
    }
  }

  @Test
  public void testAggregationUsingGroupByQuery() throws Exception {
    // GIVEN
    TimelineMetricAggregator aggregatorMinute =
      TimelineMetricAggregatorFactory.createTimelineMetricAggregatorMinute(hdb,
        getConfigurationForTest(true), null, null);
    TimelineMetricReadHelper readHelper = new TimelineMetricReadHelper(false);

    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long minute = 60 * 1000;
    hdb.insertMetricRecords(prepareTimelineMetrics(startTime, "local"));
    hdb.insertMetricRecords(prepareTimelineMetrics(ctime += minute, "local"));
    hdb.insertMetricRecords(prepareTimelineMetrics(ctime += minute, "local"));
    hdb.insertMetricRecords(prepareTimelineMetrics(ctime += minute, "local"));
    hdb.insertMetricRecords(prepareTimelineMetrics(ctime += minute, "local"));

    long endTime = startTime + 1000 * 60 * 4;
    boolean success = aggregatorMinute.doWork(startTime, endTime);
    assertTrue(success);

    Condition condition = new DefaultCondition(null, null, null, null, startTime,
      endTime, null, null, true);
    condition.setStatement(String.format(GET_METRIC_AGGREGATE_ONLY_SQL, METRICS_AGGREGATE_MINUTE_TABLE_NAME));

    PreparedStatement pstmt = PhoenixTransactSQL.prepareGetMetricsSqlStmt(conn, condition);
    ResultSet rs = pstmt.executeQuery();
    MetricHostAggregate expectedAggregate =
      MetricTestHelper.createMetricHostAggregate(2.0, 0.0, 20, 15.0);

    int count = 0;
    while (rs.next()) {
      TimelineMetric currentMetric =
        readHelper.getTimelineMetricKeyFromResultSet(rs);
      MetricHostAggregate currentHostAggregate =
        readHelper.getMetricHostAggregateFromResultSet(rs);

      if ("disk_free".equals(currentMetric.getMetricName())) {
        assertEquals(2.0, currentHostAggregate.getMax());
        assertEquals(0.0, currentHostAggregate.getMin());
        assertEquals(20, currentHostAggregate.getNumberOfSamples());
        assertEquals(15.0, currentHostAggregate.getSum());
        assertEquals(15.0 / 20, currentHostAggregate.calculateAverage());
        count++;
      } else if ("mem_free".equals(currentMetric.getMetricName())) {
        assertEquals(2.0, currentHostAggregate.getMax());
        assertEquals(0.0, currentHostAggregate.getMin());
        assertEquals(20, currentHostAggregate.getNumberOfSamples());
        assertEquals(15.0, currentHostAggregate.getSum());
        assertEquals(15.0 / 20, currentHostAggregate.calculateAverage());
        count++;
      } else {
        fail("Unexpected entry");
      }
    }
    assertEquals("Two aggregated entries expected", 2, count);
  }

  private final static Comparator<TimelineMetric> TIME_IGNORING_COMPARATOR =
    new Comparator<TimelineMetric>() {
      @Override
      public int compare(TimelineMetric o1, TimelineMetric o2) {
        return o1.equalsExceptTime(o2) ? 0 : 1;
      }
    };

  private TimelineMetrics prepareTimelineMetrics(long startTime, String host) {
    TimelineMetrics metrics = new TimelineMetrics();
    metrics.setMetrics(Arrays.asList(
      createMetric(startTime, "disk_free", host),
      createMetric(startTime, "mem_free", host)));

    return metrics;
  }

  private TimelineMetric createMetric(long startTime, String metricName, String host) {
    TimelineMetric m = new TimelineMetric();
    m.setAppId("host");
    m.setHostName(host);
    m.setMetricName(metricName);
    m.setStartTime(startTime);
    TreeMap<Long, Double> vals = new TreeMap<Long, Double>();
    vals.put(startTime + 15000l, 0.0);
    vals.put(startTime + 30000l, 0.0);
    vals.put(startTime + 45000l, 1.0);
    vals.put(startTime + 60000l, 2.0);

    m.setMetricValues(vals);

    return m;
  }

}
