/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability;

import junit.framework.Assert;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.AbstractMiniHBaseClusterTest;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration;
import org.apache.helix.HelixManager;
import org.apache.helix.HelixManagerFactory;
import org.apache.helix.InstanceType;
import org.apache.helix.model.ExternalView;
import org.apache.helix.model.InstanceConfig;
import org.junit.Before;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.MetricCollectorHAController.DEFAULT_STATE_MODEL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.MetricCollectorHAController.METRIC_AGGREGATORS;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.MetricCollectorHAController.CLUSTER_NAME;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

public class MetricCollectorHAControllerTest extends AbstractMiniHBaseClusterTest {
  TimelineMetricConfiguration configuration;

  @Before
  public void setup() throws Exception {
    configuration = createNiceMock(TimelineMetricConfiguration.class);

    expect(configuration.getInstanceHostnameFromEnv()).andReturn("h1");
    expect(configuration.getInstancePort()).andReturn("12000");
    // jdbc:phoenix:localhost:52887:/hbase;test=true
    String zkUrl = getUrl();
    String port = zkUrl.split(":")[3];
    String quorum = zkUrl.split(":")[2];

    expect(configuration.getClusterZKClientPort()).andReturn(port);
    expect(configuration.getClusterZKQuorum()).andReturn(quorum);
    expect(configuration.getZkConnectionUrl(port, quorum)).andReturn(quorum + ":" + port);

    replay(configuration);
  }

  @Test(timeout = 180000)
  public void testHAControllerDistributedAggregation() throws Exception {
    MetricCollectorHAController haController = new MetricCollectorHAController(configuration);
    haController.initializeHAController();
    // Wait for task assignment
    Thread.sleep(10000);

    Assert.assertTrue(haController.isInitialized());
    Assert.assertEquals(1, haController.getLiveInstanceHostNames().size());
    Assert.assertTrue(haController.getAggregationTaskRunner().performsClusterAggregation());
    Assert.assertTrue(haController.getAggregationTaskRunner().performsHostAggregation());

    // Add new instance
    InstanceConfig instanceConfig2 = new InstanceConfig("h2_12001");
    haController.admin.addInstance(CLUSTER_NAME, instanceConfig2);
    HelixManager manager2 = HelixManagerFactory.getZKHelixManager(CLUSTER_NAME,
      instanceConfig2.getInstanceName(),
      InstanceType.PARTICIPANT, haController.zkConnectUrl);
    manager2.getStateMachineEngine().registerStateModelFactory(DEFAULT_STATE_MODEL,
      new OnlineOfflineStateModelFactory(instanceConfig2.getInstanceName(),
        new AggregationTaskRunner(instanceConfig2.getInstanceName(), "", CLUSTER_NAME)));
    manager2.connect();
    haController.admin.rebalance(CLUSTER_NAME, METRIC_AGGREGATORS, 1);

    // Wait on re-assignment of partitions
    Thread.sleep(10000);
    Assert.assertEquals(2, haController.getLiveInstanceHostNames().size());

    ExternalView view = haController.admin.getResourceExternalView(CLUSTER_NAME, METRIC_AGGREGATORS);

    Map<String, String> partitionInstanceMap = new HashMap<>();

    for (String partition : view.getPartitionSet()) {
      Map<String, String> states = view.getStateMap(partition);
      // (instance, state) pairs
      for (Map.Entry<String, String> stateEntry : states.entrySet()) {
        partitionInstanceMap.put(partition, stateEntry.getKey());
        Assert.assertEquals("ONLINE", stateEntry.getValue());
      }
    }
    // Re-assigned partitions
    Assert.assertEquals(2, partitionInstanceMap.size());

    haController.getAggregationTaskRunner().stop();
    haController.manager.disconnect();
  }
}
