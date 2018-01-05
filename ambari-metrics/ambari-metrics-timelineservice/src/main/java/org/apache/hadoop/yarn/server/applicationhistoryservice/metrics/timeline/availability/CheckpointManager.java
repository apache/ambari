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

import static org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.AggregationTaskRunner.ACTUAL_AGGREGATOR_NAMES;

import org.I0Itec.zkclient.DataUpdater;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability.AggregationTaskRunner.AGGREGATOR_NAME;
import org.apache.helix.AccessOption;
import org.apache.helix.ZNRecord;
import org.apache.helix.store.zk.ZkHelixPropertyStore;
import org.apache.zookeeper.data.Stat;

public class CheckpointManager {
  private final ZkHelixPropertyStore<ZNRecord> propertyStore;
  private static final Log LOG = LogFactory.getLog(CheckpointManager.class);

  static final String ZNODE_FIELD = "checkpoint";
  static final String CHECKPOINT_PATH_PREFIX = "CHECKPOINTS";

  public CheckpointManager(ZkHelixPropertyStore<ZNRecord> propertyStore) {
    this.propertyStore = propertyStore;
  }

  /**
   * Read aggregator checkpoint from zookeeper
   *
   * @return timestamp
   */
  public long readCheckpoint(AGGREGATOR_NAME aggregatorName) {
    String path = getCheckpointZKPath(aggregatorName);
    LOG.debug("Reading checkpoint at " + path);
    Stat stat = new Stat();
    ZNRecord znRecord = propertyStore.get(path, stat, AccessOption.PERSISTENT);
    if (LOG.isTraceEnabled()) {
      LOG.trace("Stat => " + stat);
    }
    long checkpoint = znRecord != null ? znRecord.getLongField(ZNODE_FIELD, -1) : -1;
    LOG.debug("Checkpoint value = " + checkpoint);
    return checkpoint;
  }

  /**
   * Write aggregator checkpoint in zookeeper
   *
   * @param value timestamp
   * @return sucsess
   */
  public boolean writeCheckpoint(AGGREGATOR_NAME aggregatorName, long value) {
    String path = getCheckpointZKPath(aggregatorName);
    LOG.debug(String.format("Saving checkpoint at %s with value %s", path, value));
    return propertyStore.update(path, new CheckpointDataUpdater(path, value), AccessOption.PERSISTENT);
  }

  static class CheckpointDataUpdater implements DataUpdater<ZNRecord> {
    final String path;
    final Long value;

    public CheckpointDataUpdater(String path, Long value) {
      this.path = path;
      this.value = value;
    }

    @Override
    public ZNRecord update(ZNRecord currentData) {
      if (currentData == null) {
        currentData = new ZNRecord(path);
      }
      currentData.setLongField(ZNODE_FIELD, value);
      return currentData;
    }
  }

  String getCheckpointZKPath(AGGREGATOR_NAME aggregatorName) {
    StringBuilder sb = new StringBuilder("/");
    sb.append(CHECKPOINT_PATH_PREFIX);
    sb.append("/");
    sb.append(ACTUAL_AGGREGATOR_NAMES.get(aggregatorName));
    return sb.toString();
  }
}
