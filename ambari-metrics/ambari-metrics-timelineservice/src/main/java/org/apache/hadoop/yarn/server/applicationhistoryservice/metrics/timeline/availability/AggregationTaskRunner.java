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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineMetricAggregator.AGGREGATOR_TYPE;
import org.apache.helix.HelixManager;
import org.apache.helix.HelixManagerFactory;
import org.apache.helix.InstanceType;
import org.apache.helix.participant.StateMachineEngine;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineMetricAggregator.AGGREGATOR_TYPE.CLUSTER;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineMetricAggregator.AGGREGATOR_TYPE.HOST;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.AggregationTaskRunner.AGGREGATOR_NAME.METRIC_AGGREGATE_DAILY;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.AggregationTaskRunner.AGGREGATOR_NAME.METRIC_AGGREGATE_HOURLY;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.AggregationTaskRunner.AGGREGATOR_NAME.METRIC_AGGREGATE_MINUTE;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.AggregationTaskRunner.AGGREGATOR_NAME.METRIC_AGGREGATE_SECOND;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.AggregationTaskRunner.AGGREGATOR_NAME.METRIC_RECORD_DAILY;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.AggregationTaskRunner.AGGREGATOR_NAME.METRIC_RECORD_HOURLY;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.AggregationTaskRunner.AGGREGATOR_NAME.METRIC_RECORD_MINUTE;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.MetricCollectorHAController.DEFAULT_STATE_MODEL;
import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.MetricCollectorHAController.METRIC_AGGREGATORS;

public class AggregationTaskRunner {
  private final String instanceName;
  private final String zkAddress;
  private final String clusterName;
  private HelixManager manager;
  private static final Log LOG = LogFactory.getLog(AggregationTaskRunner.class);
  private CheckpointManager checkpointManager;
  // Map partition name to an aggregator dimension
  static final Map<String, AGGREGATOR_TYPE> PARTITION_AGGREGATION_TYPES = new HashMap<>();
  // Ownership flags to be set by the State transitions
  private final AtomicBoolean performsClusterAggregation = new AtomicBoolean(false);
  private final AtomicBoolean performsHostAggregation = new AtomicBoolean(false);

  public enum AGGREGATOR_NAME {
    METRIC_RECORD_MINUTE,
    METRIC_RECORD_HOURLY,
    METRIC_RECORD_DAILY,
    METRIC_AGGREGATE_SECOND,
    METRIC_AGGREGATE_MINUTE,
    METRIC_AGGREGATE_HOURLY,
    METRIC_AGGREGATE_DAILY,
  }

  public static final Map<AGGREGATOR_NAME, String> ACTUAL_AGGREGATOR_NAMES = new HashMap<>();

  static {
    ACTUAL_AGGREGATOR_NAMES.put(METRIC_RECORD_MINUTE, "TimelineMetricHostAggregatorMinute");
    ACTUAL_AGGREGATOR_NAMES.put(METRIC_RECORD_HOURLY, "TimelineMetricHostAggregatorHourly");
    ACTUAL_AGGREGATOR_NAMES.put(METRIC_RECORD_DAILY, "TimelineMetricHostAggregatorDaily");
    ACTUAL_AGGREGATOR_NAMES.put(METRIC_AGGREGATE_SECOND, "TimelineClusterAggregatorSecond");
    ACTUAL_AGGREGATOR_NAMES.put(METRIC_AGGREGATE_MINUTE, "TimelineClusterAggregatorMinute");
    ACTUAL_AGGREGATOR_NAMES.put(METRIC_AGGREGATE_HOURLY, "TimelineClusterAggregatorHourly");
    ACTUAL_AGGREGATOR_NAMES.put(METRIC_AGGREGATE_DAILY, "TimelineClusterAggregatorDaily");

    // Partition name to task assignment
    PARTITION_AGGREGATION_TYPES.put(METRIC_AGGREGATORS + "_0", CLUSTER);
    PARTITION_AGGREGATION_TYPES.put(METRIC_AGGREGATORS + "_1", HOST);
  }

  public AggregationTaskRunner(String instanceName, String zkAddress, String clusterName) {
    this.instanceName = instanceName;
    this.zkAddress = zkAddress;
    this.clusterName = clusterName;
  }

  public void initialize() throws Exception {
    manager = HelixManagerFactory.getZKHelixManager(clusterName, instanceName,
      InstanceType.PARTICIPANT, zkAddress);

    OnlineOfflineStateModelFactory stateModelFactory =
      new OnlineOfflineStateModelFactory(instanceName, this);

    StateMachineEngine stateMach = manager.getStateMachineEngine();
    stateMach.registerStateModelFactory(DEFAULT_STATE_MODEL, stateModelFactory);
    manager.connect();

    checkpointManager = new CheckpointManager(manager.getHelixPropertyStore());
  }

  public boolean performsClusterAggregation() {
    return performsClusterAggregation.get();
  }

  public boolean performsHostAggregation() {
    return performsHostAggregation.get();
  }

  public CheckpointManager getCheckpointManager() {
    return checkpointManager;
  }

  public void setPartitionAggregationFunction(AGGREGATOR_TYPE type) {
    switch (type) {
      case HOST:
        performsHostAggregation.set(true);
        LOG.info("Set host aggregator function for : " + instanceName);
        break;
      case CLUSTER:
        performsClusterAggregation.set(true);
        LOG.info("Set cluster aggregator function for : " + instanceName);
    }
  }

  public void unsetPartitionAggregationFunction(AGGREGATOR_TYPE type) {
    switch (type) {
      case HOST:
        performsHostAggregation.set(false);
        LOG.info("Unset host aggregator function for : " + instanceName);
        break;
      case CLUSTER:
        performsClusterAggregation.set(false);
        LOG.info("Unset cluster aggregator function for : " + instanceName);
    }
  }

  /**
   * Disconnect participant before controller shutdown
   */
  void stop() {
    manager.disconnect();
  }
}
