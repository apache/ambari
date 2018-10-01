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
package org.apache.ambari.metrics.core.timeline;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.DATE_TIERED_COMPACTION_POLICY;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.FIFO_COMPACTION_POLICY_CLASS;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.HSTORE_COMPACTION_CLASS_KEY;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.HSTORE_ENGINE_CLASS;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.CONTAINER_METRICS_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_AGGREGATE_MINUTE_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_CLUSTER_AGGREGATE_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.METRICS_RECORD_TABLE_NAME;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.PHOENIX_TABLES;
import static org.apache.ambari.metrics.core.timeline.query.PhoenixTransactSQL.PHOENIX_TABLES_REGEX_PATTERN;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.ArrayUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptor;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.metrics2.sink.timeline.ContainerMetric;
import org.apache.hadoop.metrics2.sink.timeline.MetricClusterAggregate;
import org.apache.hadoop.metrics2.sink.timeline.MetricHostAggregate;
import org.apache.hadoop.metrics2.sink.timeline.Precision;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.ambari.metrics.core.timeline.aggregators.Function;
import org.apache.ambari.metrics.core.timeline.aggregators.TimelineClusterMetric;
import org.apache.ambari.metrics.core.timeline.aggregators.TimelineMetricAggregator;
import org.apache.ambari.metrics.core.timeline.aggregators.TimelineMetricAggregatorFactory;
import org.apache.ambari.metrics.core.timeline.query.Condition;
import org.apache.ambari.metrics.core.timeline.query.DefaultCondition;
import org.junit.Test;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import junit.framework.Assert;

public class ITPhoenixHBaseAccessor extends AbstractMiniHBaseClusterTest {

  @Test
  public void testGetMetricRecordsSeconds() throws IOException, SQLException {
    // GIVEN
    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long minute = 60 * 1000;
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime, "local1",
      "disk_free", 1), true);
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime, "local2",
      "disk_free", 2), true);
    ctime += minute;
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime, "local1",
      "disk_free", 2), true);
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime, "local2",
      "disk_free", 1), true);

    // WHEN
    long endTime = ctime + minute;
    List<byte[]> uuids = metadataManager.getUuidsForGetMetricQuery(new ArrayList<String>() {{ add("disk_free"); }},
      Collections.singletonList("local1"),
      "host", null);

    Condition condition = new DefaultCondition(uuids,
      new ArrayList<String>() {{ add("disk_free"); }},
      Collections.singletonList("local1"),
      "host", null, startTime, endTime, Precision.SECONDS, null, true);
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
      TimelineMetricAggregatorFactory.createTimelineMetricAggregatorMinute(hdb, new Configuration(), metadataManager, null);

    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long minute = 60 * 1000;

    TimelineMetrics metrics1 = MetricTestHelper.prepareSingleTimelineMetric(ctime, "local3",
      "disk_free", 1);
    hdb.insertMetricRecords(metrics1, true);

    TimelineMetrics metrics2 = MetricTestHelper.prepareSingleTimelineMetric(ctime + minute, "local3",
      "disk_free", 2);
    hdb.insertMetricRecords(metrics2, true);

    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime, "local2",
      "disk_free", 2), true);
    long endTime = ctime + minute;
    boolean success = aggregatorMinute.doWork(startTime - 1000, endTime);
    assertTrue(success);

    // WHEN
    List<byte[]> uuids = metadataManager.getUuidsForGetMetricQuery(new ArrayList<String>() {{ add("disk_%"); }},
      Collections.singletonList("local3"),
      "host", null);
    Condition condition = new DefaultCondition(uuids,
      new ArrayList<String>() {{ add("disk_free"); }},
      Collections.singletonList("local3"),
      "host", null, startTime, endTime + 1000, Precision.MINUTES, null, false);
    TimelineMetrics timelineMetrics = hdb.getMetricRecords(condition,
      singletonValueFunctionMap("disk_free"));

    //THEN
    assertEquals(1, timelineMetrics.getMetrics().size());
    TimelineMetric metric = timelineMetrics.getMetrics().get(0);

    assertEquals("disk_free", metric.getMetricName());
    assertEquals("local3", metric.getHostName());
    assertEquals(1, metric.getMetricValues().size());
    Iterator<Map.Entry<Long, Double>> iterator = metric.getMetricValues().entrySet().iterator();
    assertEquals(1.5, iterator.next().getValue(), 0.00001);
  }

  @Test
  public void testGetMetricRecordsHours() throws IOException, SQLException {
    // GIVEN
    TimelineMetricAggregator aggregator =
      TimelineMetricAggregatorFactory.createTimelineMetricAggregatorHourly(hdb, new Configuration(), metadataManager, null);

    MetricHostAggregate expectedAggregate =
      MetricTestHelper.createMetricHostAggregate(2.0, 0.0, 20, 15.0);
    Map<TimelineMetric, MetricHostAggregate>
      aggMap = new HashMap<TimelineMetric,
      MetricHostAggregate>();

    long startTime = System.currentTimeMillis();
    int min_5 = 5 * 60 * 1000;
    long ctime = startTime - min_5;
    aggMap.put(MetricTestHelper.createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(MetricTestHelper.createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(MetricTestHelper.createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(MetricTestHelper.createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(MetricTestHelper.createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(MetricTestHelper.createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(MetricTestHelper.createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(MetricTestHelper.createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(MetricTestHelper.createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(MetricTestHelper.createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(MetricTestHelper.createEmptyTimelineMetric(ctime += min_5), expectedAggregate);
    aggMap.put(MetricTestHelper.createEmptyTimelineMetric(ctime += min_5), expectedAggregate);

    hdb.saveHostAggregateRecords(aggMap, METRICS_AGGREGATE_MINUTE_TABLE_NAME);
    long endTime = ctime + min_5;
    boolean success = aggregator.doWork(startTime - 1000, endTime);
    assertTrue(success);

    // WHEN
    List<byte[]> uuids = metadataManager.getUuidsForGetMetricQuery(new ArrayList<String>() {{ add("disk_used"); }},
      Collections.singletonList("test_host"),
      "test_app", "test_instance");

    Condition condition = new DefaultCondition(uuids,
      new ArrayList<String>() {{ add("disk_used"); }},
      Collections.singletonList("test_host"),
      "test_app", "test_instance", startTime, endTime + 1000, Precision.HOURS, null, true);
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
        hdb, new Configuration(), metadataManager, null, null);

    long startTime = System.currentTimeMillis();
    long ctime = startTime + 1;
    long minute = 60 * 1000;
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime, "local_c1",
      "disk_free", 1), true);
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime, "local_c2",
      "disk_free", 2), true);
    ctime += minute;
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime, "local_c1",
      "disk_free", 2), true);
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime, "local_c2",
      "disk_free", 1), true);

    long endTime = ctime + minute + 1;
    boolean success = agg.doWork(startTime, endTime);
    assertTrue(success);

    // WHEN
    List<byte[]> uuids = metadataManager.getUuidsForGetMetricQuery(new ArrayList<String>() {{ add("disk_free"); }},
      null,
      "host", null);

    Condition condition = new DefaultCondition(uuids,
      new ArrayList<String>() {{ add("disk_free"); }},
      null, "host", null, startTime - 90000, endTime, Precision.SECONDS, null, true);
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
      TimelineMetricAggregatorFactory.createTimelineClusterAggregatorSecond(hdb,
        new Configuration(), metadataManager, null, null);

    long startTime = System.currentTimeMillis();
    long ctime = startTime + 1;
    long minute = 60 * 1000;
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime, "local1",
      "disk_free", 1), true);
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime, "local2",
      "disk_free", 2), true);
    ctime += minute;
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime, "local1",
      "disk_free", 2), true);
    hdb.insertMetricRecords(MetricTestHelper.prepareSingleTimelineMetric(ctime, "local2",
      "disk_free", 1), true);

    long endTime = ctime + minute + 1;
    boolean success = agg.doWork(startTime, endTime);
    assertTrue(success);

    // WHEN
    List<byte[]> uuids = metadataManager.getUuidsForGetMetricQuery(new ArrayList<String>() {{ add("disk_free"); }},
      null,
      "host", null);

    Condition condition = new DefaultCondition(uuids,
      new ArrayList<String>() {{ add("disk_free"); }},
      null, "host", null, null, null, Precision.SECONDS, null, true);

    Multimap<String, List<Function>> mmap = ArrayListMultimap.create();
    mmap.put("disk_free", Collections.singletonList(new Function(Function.ReadFunction.SUM, null)));
    TimelineMetrics timelineMetrics = hdb.getAggregateMetricRecords(condition, mmap);

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
      TimelineMetricAggregatorFactory.createTimelineClusterAggregatorHourly(hdb, new Configuration(), metadataManager, null);

    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long minute = 60 * 1000;

    Map<TimelineClusterMetric, MetricClusterAggregate> records =
      new HashMap<TimelineClusterMetric, MetricClusterAggregate>();

    records.put(MetricTestHelper.createEmptyTimelineClusterMetric(ctime),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(MetricTestHelper.createEmptyTimelineClusterMetric(ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(MetricTestHelper.createEmptyTimelineClusterMetric(ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));
    records.put(MetricTestHelper.createEmptyTimelineClusterMetric(ctime += minute),
      new MetricClusterAggregate(4.0, 2, 0.0, 4.0, 0.0));

    hdb.saveClusterAggregateRecords(records);
    boolean success = agg.doWork(startTime, ctime + minute);
    assertTrue(success);

    // WHEN
    List<byte[]> uuids = metadataManager.getUuidsForGetMetricQuery(new ArrayList<String>() {{ add("disk_used"); }},
      null,
      "test_app",
      "instance_id");

    Condition condition = new DefaultCondition( uuids,
      new ArrayList<String>() {{ add("disk_used"); }},
      null,
      "test_app",
      "instance_id",
      startTime,
      ctime + minute + 1000,
      Precision.HOURS,
      null,
      true);
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
  public void testGetInvalidMetricRecords() throws IOException, SQLException {
    // GIVEN
    long startTime = System.currentTimeMillis();
    long ctime = startTime;
    long minute = 60 * 1000;

    List<TimelineMetric> timelineMetricList = new ArrayList<>();

    timelineMetricList.add(
      MetricTestHelper.createTimelineMetric(ctime, "valid_metric", "h1", "test_app",
        null, 1.0));
    timelineMetricList.add(
      MetricTestHelper.createTimelineMetric(ctime, "invalid_metric", "h1", "test_app",
        null, Double.NaN));
    TimelineMetrics timelineMetrics = new TimelineMetrics();
    timelineMetrics.setMetrics(timelineMetricList);
    hdb.insertMetricRecords(timelineMetrics, true);

    // WHEN
    long endTime = ctime + minute;
    List<byte[]> uuids = metadataManager.getUuidsForGetMetricQuery(new ArrayList<String>() {{ add("valid_metric"); }},
      Collections.singletonList("h1"),
      "test_app", null);

    Condition condition = new DefaultCondition(uuids,
      new ArrayList<String>() {{ add("valid_metric"); }},
      Collections.singletonList("h1"),
      "test_app", null, startTime - 1000, endTime, Precision.SECONDS, null, true);
    TimelineMetrics timelineMetricsFromStore = hdb.getMetricRecords(condition,
      singletonValueFunctionMap("valid_metric"));

    //THEN
    assertEquals(1, timelineMetricsFromStore.getMetrics().size());
    TimelineMetric metric = timelineMetricsFromStore.getMetrics().get(0);

    assertEquals("valid_metric", metric.getMetricName());
    assertEquals("h1", metric.getHostName());
    assertEquals(4, metric.getMetricValues().size());


    uuids = metadataManager.getUuidsForGetMetricQuery(new ArrayList<String>() {{ add("invalid_metric"); }},
      Collections.singletonList("h1"),
      "test_app", null);
    condition = new DefaultCondition(uuids,
      new ArrayList<String>() {{ add("invalid_metric"); }},
      Collections.singletonList("h1"),
      "test_app", null, startTime, endTime, Precision.SECONDS, null, true);
    timelineMetricsFromStore = hdb.getMetricRecords(condition, singletonValueFunctionMap("invalid_metric"));
    assertTrue(timelineMetricsFromStore.getMetrics().isEmpty());

  }


  @Test
  public void testInitPoliciesAndTTL() throws Exception {
    Admin hBaseAdmin = hdb.getHBaseAdmin();
    int expectedPrecisionTtl = 2 * 86400;
    int precisionTtl = 0;

    boolean modifyTables = hdb.initPoliciesAndTTL();
    Assert.assertTrue(modifyTables);
    // Verify expected policies are set
    boolean normalizerEnabled = false;
    String precisionTableCompactionPolicy = null;
    String aggregateTableCompactionPolicy = null;
    boolean precisionTableDurabilitySet  = false;
    boolean aggregateTableDurabilitySet  = false;

    boolean isComplete = false;

    for (int i = 0; i < 10 && !isComplete; i++) {
      LOG.warn("Policy check retry : " + i);
      for (String tableName : PHOENIX_TABLES) {
        TableName[] tableNames = hBaseAdmin.listTableNames(PHOENIX_TABLES_REGEX_PATTERN, false);
        TableName[] containerMetricsTableName = hBaseAdmin.listTableNames(CONTAINER_METRICS_TABLE_NAME, false);
        tableNames = (TableName[]) ArrayUtils.addAll(tableNames, containerMetricsTableName);

        Optional<TableName> tableNameOptional = Arrays.stream(tableNames)
          .filter(t -> tableName.equals(t.getNameAsString())).findFirst();

        TableDescriptor tableDescriptor = hBaseAdmin.getTableDescriptor(tableNameOptional.get());

        normalizerEnabled = tableDescriptor.isNormalizationEnabled();
        if (tableName.equals(METRICS_RECORD_TABLE_NAME)) {
          precisionTableCompactionPolicy = tableDescriptor.getValue(HSTORE_COMPACTION_CLASS_KEY);
        } else {
          aggregateTableCompactionPolicy = tableDescriptor.getValue(HSTORE_ENGINE_CLASS);
        }
        LOG.debug("Table: " + tableName + ", normalizerEnabled = " + normalizerEnabled);
        // Best effort for 20 seconds
        if (normalizerEnabled || (precisionTableCompactionPolicy == null && aggregateTableCompactionPolicy == null)) {
          Thread.sleep(2000l);
        } else {
          isComplete = true;
        }
        if (tableName.equals(METRICS_RECORD_TABLE_NAME)) {
          precisionTableDurabilitySet = (Durability.SKIP_WAL.equals(tableDescriptor.getDurability()));
          for (ColumnFamilyDescriptor family : tableDescriptor.getColumnFamilies()) {
            precisionTtl = family.getTimeToLive();
          }
        }

        if (tableName.equals(METRICS_CLUSTER_AGGREGATE_TABLE_NAME)) {
          aggregateTableDurabilitySet = (Durability.ASYNC_WAL.equals(tableDescriptor.getDurability()));
        }
      }
    }

    Assert.assertFalse("Normalizer disabled.", normalizerEnabled);
    Assert.assertTrue("METRIC_RECORD_UUID Durability Set.", precisionTableDurabilitySet);
    Assert.assertTrue("METRIC_AGGREGATE_UUID Durability Set.", aggregateTableDurabilitySet);
    Assert.assertEquals("FIFO compaction policy is set for METRIC_RECORD_UUID.", FIFO_COMPACTION_POLICY_CLASS, precisionTableCompactionPolicy);
    Assert.assertEquals("Date Tiered compaction policy is set for aggregate tables", DATE_TIERED_COMPACTION_POLICY, aggregateTableCompactionPolicy);
    Assert.assertEquals("Precision TTL value as expected.", expectedPrecisionTtl, precisionTtl);

    hBaseAdmin.close();

    //Try one more time. This time, modifyTable should be 'false'
    assertFalse(hdb.initPoliciesAndTTL());

    //Change precision durability to
    Field f = PhoenixHBaseAccessor.class.getDeclaredField("metricsConf");
    f.setAccessible(true);
    Configuration newMetricsConf = (Configuration) f.get(hdb);
    newMetricsConf.set("timeline.metrics." + METRICS_RECORD_TABLE_NAME + ".durability", "ASYNC_WAL");
    f.set(hdb, newMetricsConf);

    boolean modifyTable = hdb.initPoliciesAndTTL();
    Assert.assertTrue(modifyTable);
    TableName[] tableNames = hBaseAdmin.listTableNames(PHOENIX_TABLES_REGEX_PATTERN, false);

    Optional<TableName> tableNameOptional = Arrays.stream(tableNames)
      .filter(t -> METRICS_RECORD_TABLE_NAME.equals(t.getNameAsString())).findFirst();
    TableDescriptor tableDescriptor = hBaseAdmin.getTableDescriptor(tableNameOptional.get());
    Assert.assertEquals(tableDescriptor.getDurability().toString(),"ASYNC_WAL");

    //Try one more time. This time, modifyTable should be 'false'
    assertFalse(hdb.initPoliciesAndTTL());
  }

  private Multimap<String, List<Function>> singletonValueFunctionMap(String metricName) {
    Multimap<String, List<Function>> mmap = ArrayListMultimap.create();
    mmap.put(metricName, Collections.singletonList(new Function()));
    return mmap;
  }

  @Test
  public void testInsertContainerMetrics() throws Exception {
    ContainerMetric metric = new ContainerMetric();
    metric.setContainerId("container_1450744875949_0001_01_000001");
    metric.setHostName("host1");
    metric.setPmemLimit(2048);
    metric.setVmemLimit(2048);
    metric.setPmemUsedAvg(1024);
    metric.setPmemUsedMin(1024);
    metric.setPmemUsedMax(1024);
    metric.setLaunchDuration(2000);
    metric.setLocalizationDuration(3000);
    long startTime = System.currentTimeMillis();
    long finishTime = startTime + 5000;
    metric.setStartTime(startTime);
    metric.setFinishTime(finishTime);
    metric.setExitCode(0);
    List<ContainerMetric> list = Arrays.asList(metric);
    hdb.insertContainerMetrics(list);
    PreparedStatement stmt = conn.prepareStatement("SELECT * FROM CONTAINER_METRICS");
    ResultSet set = stmt.executeQuery();
    // check each filed is set properly when read back.
    boolean foundRecord = false;
    while (set.next()) {
      assertEquals("application_1450744875949_0001", set.getString("APP_ID"));
      assertEquals("container_1450744875949_0001_01_000001", set.getString("CONTAINER_ID"));
      assertEquals(new java.sql.Timestamp(startTime), set.getTimestamp("START_TIME"));
      assertEquals(new java.sql.Timestamp(finishTime), set.getTimestamp("FINISH_TIME"));
      assertEquals(5000, set.getLong("DURATION"));
      assertEquals("host1", set.getString("HOSTNAME"));
      assertEquals(0, set.getInt("EXIT_CODE"));
      assertEquals(3000, set.getLong("LOCALIZATION_DURATION"));
      assertEquals(2000, set.getLong("LAUNCH_DURATION"));
      assertEquals((double)2, set.getDouble("MEM_REQUESTED_GB"));
      assertEquals((double)2 * 5000, set.getDouble("MEM_REQUESTED_GB_MILLIS"));
      assertEquals((double)2, set.getDouble("MEM_VIRTUAL_GB"));
      assertEquals((double)1, set.getDouble("MEM_USED_GB_MIN"));
      assertEquals((double)1, set.getDouble("MEM_USED_GB_MAX"));
      assertEquals((double)1, set.getDouble("MEM_USED_GB_AVG"));
      assertEquals((double)(2 - 1), set.getDouble("MEM_UNUSED_GB"));
      assertEquals((double)(2-1) * 5000, set.getDouble("MEM_UNUSED_GB_MILLIS"));
      foundRecord = true;
    }
    assertTrue(foundRecord);
  }
}
