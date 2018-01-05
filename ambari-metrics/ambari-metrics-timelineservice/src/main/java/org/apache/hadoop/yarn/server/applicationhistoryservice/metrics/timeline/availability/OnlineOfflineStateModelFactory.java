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

import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.AggregationTaskRunner.PARTITION_AGGREGATION_TYPES;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.aggregators.TimelineMetricAggregator.AGGREGATOR_TYPE;
import org.apache.helix.NotificationContext;
import org.apache.helix.model.Message;
import org.apache.helix.participant.statemachine.StateModel;
import org.apache.helix.participant.statemachine.StateModelFactory;

public class OnlineOfflineStateModelFactory extends StateModelFactory<StateModel> {
  private static final Log LOG = LogFactory.getLog(OnlineOfflineStateModelFactory.class);
  private final String instanceName;
  private final AggregationTaskRunner taskRunner;

  public OnlineOfflineStateModelFactory(String instanceName, AggregationTaskRunner taskRunner) {
    this.instanceName = instanceName;
    this.taskRunner = taskRunner;
  }

  @Override
  public StateModel createNewStateModel(String resourceName, String partition) {
    LOG.info("Received request to process partition = " + partition + ", for " +
            "resource = " + resourceName + ", at " + instanceName);
    return new OnlineOfflineStateModel();
  }

  public class OnlineOfflineStateModel extends StateModel {
    public void onBecomeOnlineFromOffline(Message message, NotificationContext context) {
      String partitionName = message.getPartitionName();
      LOG.info("Received transition to Online from Offline for partition: " + partitionName);
      AGGREGATOR_TYPE type = PARTITION_AGGREGATION_TYPES.get(partitionName);
      taskRunner.setPartitionAggregationFunction(type);
    }

    public void onBecomeOfflineFromOnline(Message message, NotificationContext context) {
      String partitionName = message.getPartitionName();
      LOG.info("Received transition to Offline from Online for partition: " + partitionName);
      AGGREGATOR_TYPE type = PARTITION_AGGREGATION_TYPES.get(partitionName);
      taskRunner.unsetPartitionAggregationFunction(type);
    }

    public void onBecomeDroppedFromOffline(Message message, NotificationContext context) {
      String partitionName = message.getPartitionName();
      LOG.info("Received transition to Dropped from Offline for partition: " + partitionName);
      AGGREGATOR_TYPE type = PARTITION_AGGREGATION_TYPES.get(partitionName);
      taskRunner.unsetPartitionAggregationFunction(type);
    }
  }
}
