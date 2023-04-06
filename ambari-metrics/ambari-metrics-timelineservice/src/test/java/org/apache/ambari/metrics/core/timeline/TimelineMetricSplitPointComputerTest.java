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

import org.apache.ambari.metrics.core.timeline.aggregators.TimelineClusterMetric;
import org.apache.ambari.metrics.core.timeline.discovery.TimelineMetricMetadataManager;
import org.apache.hadoop.conf.Configuration;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.TIMELINE_METRIC_INITIAL_CONFIGURED_MASTER_COMPONENTS;
import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.TIMELINE_METRIC_INITIAL_CONFIGURED_SLAVE_COMPONENTS;
import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;


public class TimelineMetricSplitPointComputerTest {

  @Test
  public void testSplitPointComputationForBasicCluster() {

    /**
     *  HBase Total heap = 1G.
     *  HDFS,HBASE,YARN services deployed.
     */
    Configuration metricsConfMock = EasyMock.createMock(Configuration.class);

    expect(metricsConfMock.get(TIMELINE_METRIC_INITIAL_CONFIGURED_MASTER_COMPONENTS, "")).
      andReturn("METRICS_COLLECTOR,AMBARI_SERVER,NAMENODE,RESOURCEMANAGER").once();
    expect(metricsConfMock.get(TIMELINE_METRIC_INITIAL_CONFIGURED_SLAVE_COMPONENTS, "")).
      andReturn("METRICS_MONITOR,DATANODE,NODEMANAGER,HBASE_REGIONSERVER").once();
    expect(metricsConfMock.getDouble("hbase_total_heapsize", 1024*1024*1024)).andReturn(1024 * 1024 * 1024.0).once();

    Configuration hbaseConfMock = EasyMock.createMock(Configuration.class);
    expect(hbaseConfMock.getDouble("hbase.regionserver.global.memstore.upperLimit", 0.3)).andReturn(0.3).once();
    expect(hbaseConfMock.getDouble("hbase.hregion.memstore.flush.size", 134217728)).andReturn(134217728.0).once();

    TimelineMetricMetadataManager metricMetadataManagerMock = EasyMock.createNiceMock(TimelineMetricMetadataManager.class);
    expect(metricMetadataManagerMock.getUuid(anyObject(TimelineClusterMetric.class), anyBoolean())).andReturn(new byte[16]);

    replay(metricsConfMock, hbaseConfMock, metricMetadataManagerMock);

    TimelineMetricSplitPointComputer timelineMetricSplitPointComputer = new TimelineMetricSplitPointComputer(metricsConfMock,
      hbaseConfMock,
      metricMetadataManagerMock);
    timelineMetricSplitPointComputer.computeSplitPoints();

    Assert.assertEquals(timelineMetricSplitPointComputer.getPrecisionSplitPoints().size(), 3);
    Assert.assertEquals(timelineMetricSplitPointComputer.getClusterAggregateSplitPoints().size(), 1);
    Assert.assertEquals(timelineMetricSplitPointComputer.getHostAggregateSplitPoints().size(), 1);
  }

  @Test
  public void testSplitPointComputationForMediumCluster() {

    /**
     *  HBase Total heap = 8G.
     *  All services deployed.
     */
    Configuration metricsConfMock = EasyMock.createMock(Configuration.class);

    expect(metricsConfMock.get(TIMELINE_METRIC_INITIAL_CONFIGURED_MASTER_COMPONENTS, "")).
      andReturn("METRICS_COLLECTOR,AMBARI_SERVER,NAMENODE,RESOURCEMANAGER," +
        "NIMBUS,HIVESERVER2,HIVEMETASTORE,HBASE_MASTER,KAFKA_BROKER").once();
    expect(metricsConfMock.get(TIMELINE_METRIC_INITIAL_CONFIGURED_SLAVE_COMPONENTS, "")).
      andReturn("METRICS_MONITOR,DATANODE,NODEMANAGER,HBASE_REGIONSERVER").once();
    expect(metricsConfMock.getDouble("hbase_total_heapsize", 1024*1024*1024)).andReturn(8589934592.0).once();

    Configuration hbaseConfMock = EasyMock.createMock(Configuration.class);
    expect(hbaseConfMock.getDouble("hbase.regionserver.global.memstore.upperLimit", 0.3)).andReturn(0.3).once();
    expect(hbaseConfMock.getDouble("hbase.hregion.memstore.flush.size", 134217728)).andReturn(134217728.0).once();

    TimelineMetricMetadataManager metricMetadataManagerMock = EasyMock.createNiceMock(TimelineMetricMetadataManager.class);
    expect(metricMetadataManagerMock.getUuid(anyObject(TimelineClusterMetric.class), anyBoolean())).andReturn(new byte[16]);

    replay(metricsConfMock, hbaseConfMock, metricMetadataManagerMock);

    TimelineMetricSplitPointComputer timelineMetricSplitPointComputer = new TimelineMetricSplitPointComputer(metricsConfMock,
      hbaseConfMock,
      metricMetadataManagerMock);
    timelineMetricSplitPointComputer.computeSplitPoints();

    Assert.assertEquals(timelineMetricSplitPointComputer.getPrecisionSplitPoints().size(), 6);
    Assert.assertEquals(timelineMetricSplitPointComputer.getClusterAggregateSplitPoints().size(), 1);
    Assert.assertEquals(timelineMetricSplitPointComputer.getHostAggregateSplitPoints().size(), 1);
  }

  @Test
  public void testSplitPointComputationForLargeCluster() {

    /**
     *  HBase Total heap = 24G.
     *  All services deployed.
     */
    Configuration metricsConfMock = EasyMock.createMock(Configuration.class);

    expect(metricsConfMock.get(TIMELINE_METRIC_INITIAL_CONFIGURED_MASTER_COMPONENTS, "")).
      andReturn("METRICS_COLLECTOR,AMBARI_SERVER,NAMENODE,RESOURCEMANAGER," +
        "NIMBUS,HIVESERVER2,HIVEMETASTORE,HBASE_MASTER,KAFKA_BROKER").once();
    expect(metricsConfMock.get(TIMELINE_METRIC_INITIAL_CONFIGURED_SLAVE_COMPONENTS, "")).
      andReturn("METRICS_MONITOR,DATANODE,NODEMANAGER,HBASE_REGIONSERVER").once();
    expect(metricsConfMock.getDouble("hbase_total_heapsize", 1024*1024*1024)).andReturn(24 * 1024 * 1024 * 1024.0).once();

    Configuration hbaseConfMock = EasyMock.createMock(Configuration.class);
    expect(hbaseConfMock.getDouble("hbase.regionserver.global.memstore.upperLimit", 0.3)).andReturn(0.3).once();
    expect(hbaseConfMock.getDouble("hbase.hregion.memstore.flush.size", 134217728)).andReturn(2 * 134217728.0).once();

    TimelineMetricMetadataManager metricMetadataManagerMock = EasyMock.createNiceMock(TimelineMetricMetadataManager.class);
    expect(metricMetadataManagerMock.getUuid(anyObject(TimelineClusterMetric.class), anyBoolean())).andReturn(new byte[16]);

    replay(metricsConfMock, hbaseConfMock, metricMetadataManagerMock);

    TimelineMetricSplitPointComputer timelineMetricSplitPointComputer = new TimelineMetricSplitPointComputer(metricsConfMock,
      hbaseConfMock,
      metricMetadataManagerMock);
    timelineMetricSplitPointComputer.computeSplitPoints();

    Assert.assertEquals(timelineMetricSplitPointComputer.getPrecisionSplitPoints().size(), 14);
    Assert.assertEquals(timelineMetricSplitPointComputer.getClusterAggregateSplitPoints().size(), 3);
    Assert.assertEquals(timelineMetricSplitPointComputer.getHostAggregateSplitPoints().size(), 3);
  }
}
