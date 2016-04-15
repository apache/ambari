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

import junit.framework.Assert;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.metrics2.sink.timeline.Precision;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.Function;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.MetricClusterAggregate;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.MetricHostAggregate;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineClusterMetric;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineMetricAggregator;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineMetricAggregatorFactory;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery.TimelineMetricMetadataManager;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.Condition;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.DefaultCondition;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.MetricTestHelper.createEmptyTimelineClusterMetric;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.MetricTestHelper.createEmptyTimelineMetric;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.MetricTestHelper.createMetricHostAggregate;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.MetricTestHelper.prepareSingleTimelineMetric;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixHBaseAccessor.FIFO_COMPACTION_POLICY_CLASS;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.PhoenixHBaseAccessor.HSTORE_COMPACTION_CLASS_KEY;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_AGGREGATE_MINUTE_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.METRICS_RECORD_TABLE_NAME;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query.PhoenixTransactSQL.PHOENIX_TABLES;



public class ITPhoenixHBaseAccessor extends AbstractMiniHBaseClusterTest {

  @Test
  public void testGetMetricRecordsSeconds() throws IOException, SQLException {
    // GIVEN
    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long minute = 60 * 1000;
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local1",
      "disk_free", 1));
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local2",
      "disk_free", 2));
    ctime += minute;
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local1",
      "disk_free", 2));
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local2",
      "disk_free", 1));

    // WHEN
    long endTime = ctime + minute;
    Condition condition = new DefaultCondition(
      Collections.singletonList("disk_free"), Collections.singletonList("local1"),
      null, null, startTime, endTime, Precision.SECONDS, null, true);
    TimelineMetrics timelineMetrics = hdb.getMetricRecords(condition,
      singletonValueFunctionMap("disk_free"));

    //THEN
    assertEquals(1, timelineMetrics.getMetrics().size());
    TimelineMetric metric = timelineMetrics.getMetrics().get(0);

    assertEquals("disk_free", metric.getMetricName());
    assertEquals("local1", metric.getHostName());
    assertEquals(8, metric.getMetricValues().size());
  }

  @Test
  public void testGetMetricRecordsMinutes() throws IOException, SQLException {
    // GIVEN
    TimelineMetricAggregator aggregatorMinute =
      TimelineMetricAggregatorFactory.createTimelineMetricAggregatorMinute(hdb, new Configuration());

    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long minute = 60 * 1000;
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local1",
        "disk_free", 1));
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime + minute, "local1",
        "disk_free", 2));
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local2",
        "disk_free", 2));
    long endTime = ctime + minute;
    boolean success = aggregatorMinute.doWork(startTime, endTime);
    assertTrue(success);

    // WHEN
    Condition condition = new DefaultCondition(
      Collections.singletonList("disk_free"), Collections.singletonList("local1"),
      null, null, startTime, endTime, Precision.MINUTES, null, false);
    TimelineMetrics timelineMetrics = hdb.getMetricRecords(condition,
      singletonValueFunctionMap("disk_free"));

    //THEN
    assertEquals(1, timelineMetrics.getMetrics().size());
    TimelineMetric metric = timelineMetrics.getMetrics().get(0);

    assertEquals("disk_free", metric.getMetricName());
    assertEquals("local1", metric.getHostName());
    assertEquals(1, metric.getMetricValues().size());
    Iterator<Map.Entry<Long, Double>> iterator = metric.getMetricValues().entrySet().iterator();
    assertEquals(1.5, iterator.next().getValue(), 0.00001);
  }

  @Test
  public void testGetMetricRecordsHours() throws IOException, SQLException {
    // GIVEN
    TimelineMetricAggregator aggregator =
      TimelineMetricAggregatorFactory.createTimelineMetricAggregatorHourly(hdb, new Configuration());

    MetricHostAggregate expectedAggregate =
        createMetricHostAggregate(2.0, 0.0, 20, 15.0);
    Map<TimelineMetric, MetricHostAggregate>
        aggMap = new HashMap<TimelineMetric,
        MetricHostAggregate>();

    long startTime = System.currentTimeMillis();
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
    long endTime = ctime + min_5;
    boolean success = aggregator.doWork(startTime, endTime);
    assertTrue(success);

    // WHEN
    Condition condition = new DefaultCondition(
      Collections.singletonList("disk_used"), Collections.singletonList("test_host"),
      "test_app", null, startTime, endTime, Precision.HOURS, null, true);
    TimelineMetrics timelineMetrics = hdb.getMetricRecords(condition,
      singletonValueFunctionMap("disk_used"));

    //THEN
    assertEquals(1, timelineMetrics.getMetrics().size());
    TimelineMetric metric = timelineMetrics.getMetrics().get(0);

    assertEquals("disk_used", metric.getMetricName());
    assertEquals("test_host", metric.getHostName());
    assertEquals(1, metric.getMetricValues().size());
    Iterator<Map.Entry<Long, Double>> iterator = metric.getMetricValues().entrySet().iterator();
    assertEquals(0.75, iterator.next().getValue(), 0.00001);
  }

  @Test
  public void testGetClusterMetricRecordsSeconds() throws Exception {
    // GIVEN
    TimelineMetricAggregator agg =
      TimelineMetricAggregatorFactory.createTimelineClusterAggregatorSecond(
        hdb, new Configuration(), new TimelineMetricMetadataManager(hdb, new Configuration()));

    long startTime = System.currentTimeMillis();
    long ctime = startTime + 1;
    long minute = 60 * 1000;
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local1",
        "disk_free", 1));
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local2",
        "disk_free", 2));
    ctime += minute;
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local1",
        "disk_free", 2));
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local2",
        "disk_free", 1));

    long endTime = ctime + minute + 1;
    boolean success = agg.doWork(startTime, endTime);
    assertTrue(success);

    // WHEN
    Condition condition = new DefaultCondition(
        Collections.singletonList("disk_free"), null, null, null,
        startTime, endTime, Precision.SECONDS, null, true);
    TimelineMetrics timelineMetrics = hdb.getAggregateMetricRecords(condition,
      singletonValueFunctionMap("disk_free"));

    //THEN
    assertEquals(1, timelineMetrics.getMetrics().size());
    TimelineMetric metric = timelineMetrics.getMetrics().get(0);

    assertEquals("disk_free", metric.getMetricName());
    assertEquals(5, metric.getMetricValues().size());
    assertEquals(1.5, metric.getMetricValues().values().iterator().next(), 0.00001);
  }

  @Test
  public void testGetClusterMetricRecordLatestWithFunction() throws Exception {
    // GIVEN
    TimelineMetricAggregator agg =
      TimelineMetricAggregatorFactory.createTimelineClusterAggregatorSecond
        (hdb, new Configuration(), new TimelineMetricMetadataManager(hdb, new Configuration()));

    long startTime = System.currentTimeMillis();
    long ctime = startTime + 1;
    long minute = 60 * 1000;
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local1",
      "disk_free", 1));
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local2",
      "disk_free", 2));
    ctime += minute;
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local1",
      "disk_free", 2));
    hdb.insertMetricRecords(prepareSingleTimelineMetric(ctime, "local2",
      "disk_free", 1));

    long endTime = ctime + minute + 1;
    boolean success = agg.doWork(startTime, endTime);
    assertTrue(success);

    // WHEN
    Condition condition = new DefaultCondition(
      Collections.singletonList("disk_free"), null, null, null,
      null, null, Precision.SECONDS, null, true);
    TimelineMetrics timelineMetrics = hdb.getAggregateMetricRecords(condition,
      Collections.singletonMap("disk_free",
        Collections.singletonList(new Function(Function.ReadFunction.SUM, null))));

    //THEN
    assertEquals(1, timelineMetrics.getMetrics().size());
    TimelineMetric metric = timelineMetrics.getMetrics().get(0);

    assertEquals("disk_free._sum", metric.getMetricName());
    assertEquals(1, metric.getMetricValues().size());
    assertEquals(3, metric.getMetricValues().values().iterator().next().intValue());
  }

  @Test
  public void testGetClusterMetricRecordsHours() throws Exception {
    // GIVEN
    TimelineMetricAggregator agg =
      TimelineMetricAggregatorFactory.createTimelineClusterAggregatorHourly(hdb, new Configuration());

    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long minute = 60 * 1000;

    Map<TimelineClusterMetric, MetricClusterAggregate> records =
        new HashMap<TimelineClusterMetric, MetricClusterAggregate>();

    records.put(createEmptyTimelineClusterMetric(ctime),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(createEmptyTimelineClusterMetric(ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(createEmptyTimelineClusterMetric(ctime += minute),
        new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(createEmptyTimelineClusterMetric(ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));

    hdb.saveClusterAggregateRecords(records);
    boolean success = agg.doWork(startTime, ctime + minute);
    assertTrue(success);

    // WHEN
    Condition condition = new DefaultCondition(
        Collections.singletonList("disk_used"), null, null, null,
        startTime, ctime + minute, Precision.HOURS, null, true);
    TimelineMetrics timelineMetrics = hdb.getAggregateMetricRecords(condition,
      singletonValueFunctionMap("disk_used"));

    // THEN
    assertEquals(1, timelineMetrics.getMetrics().size());
    TimelineMetric metric = timelineMetrics.getMetrics().get(0);

    assertEquals("disk_used", metric.getMetricName());
    assertEquals("test_app", metric.getAppId());
    assertEquals(1, metric.getMetricValues().size());
    assertEquals(2.0, metric.getMetricValues().values().iterator().next(), 0.00001);
  }

  @Test
  public void testInitPoliciesAndTTL() throws Exception {
    HBaseAdmin hBaseAdmin = hdb.getHBaseAdmin();
    String precisionTtl = "";
    // Verify policies are unset
    for (String tableName : PHOENIX_TABLES) {
      HTableDescriptor tableDescriptor = hBaseAdmin.getTableDescriptor(tableName.getBytes());

      Assert.assertFalse("Normalizer disabled by default.", tableDescriptor.isNormalizationEnabled());
      Assert.assertNull("Default compaction policy is null.",
        tableDescriptor.getConfigurationValue(HSTORE_COMPACTION_CLASS_KEY));

      for (HColumnDescriptor family : tableDescriptor.getColumnFamilies()) {
        if (tableName.equals(METRICS_RECORD_TABLE_NAME)) {
          precisionTtl = family.getValue("TTL");
        }
      }
      Assert.assertEquals("Precision TTL value.", "86400", precisionTtl);
    }

    Field f = PhoenixHBaseAccessor.class.getDeclaredField("tableTTL");
    f.setAccessible(true);
    Map<String, String> precisionValues = (Map<String, String>) f.get(hdb);
    precisionValues.put(METRICS_RECORD_TABLE_NAME, String.valueOf(2 * 86400));
    f.set(hdb, precisionValues);

    Field f2 = PhoenixHBaseAccessor.class.getDeclaredField("timelineMetricsTablesDurability");
    f2.setAccessible(true);
    f2.set(hdb, "ASYNC_WAL");

    hdb.initPoliciesAndTTL();

    // Verify expected policies are set
    boolean normalizerEnabled = false;
    String compactionPolicy = null;
    boolean tableDurabilitySet  = false;
    for (int i = 0; i < 10; i++) {
      LOG.warn("Policy check retry : " + i);
      for (String tableName : PHOENIX_TABLES) {
        HTableDescriptor tableDescriptor = hBaseAdmin.getTableDescriptor(tableName.getBytes());
        normalizerEnabled = tableDescriptor.isNormalizationEnabled();
        tableDurabilitySet = (Durability.ASYNC_WAL.equals(tableDescriptor.getDurability()));
        compactionPolicy = tableDescriptor.getConfigurationValue(HSTORE_COMPACTION_CLASS_KEY);
        LOG.debug("Table: " + tableName + ", normalizerEnabled = " + normalizerEnabled);
        LOG.debug("Table: " + tableName + ", compactionPolicy = " + compactionPolicy);
        // Best effort for 20 seconds
        if (!normalizerEnabled || compactionPolicy == null) {
          Thread.sleep(2000l);
        }
        if (tableName.equals(METRICS_RECORD_TABLE_NAME)) {
          for (HColumnDescriptor family : tableDescriptor.getColumnFamilies()) {
            precisionTtl = family.getValue("TTL");
          }
        }
      }
    }

    Assert.assertTrue("Normalizer enabled.", normalizerEnabled);
    Assert.assertTrue("Durability Set.", tableDurabilitySet);
    Assert.assertEquals("FIFO compaction policy is set.", FIFO_COMPACTION_POLICY_CLASS, compactionPolicy);
    Assert.assertEquals("Precision TTL value not changed.", String.valueOf(2 * 86400), precisionTtl);

    hBaseAdmin.close();
  }

  private Map<String, List<Function>> singletonValueFunctionMap(String metricName) {
    return Collections.singletonMap(metricName, Collections.singletonList(new Function()));
  }
}
