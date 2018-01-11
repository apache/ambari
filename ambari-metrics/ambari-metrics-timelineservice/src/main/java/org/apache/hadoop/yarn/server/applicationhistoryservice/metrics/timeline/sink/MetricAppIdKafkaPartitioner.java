/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.sink;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MetricAppIdKafkaPartitioner implements Partitioner {

  private ObjectMapper objectMapper = new ObjectMapper();
  private static final Log LOG = LogFactory.getLog(MetricAppIdKafkaPartitioner.class);

  @Override
  public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes,
                       Cluster cluster) {

    TimelineMetrics timelineMetrics = null;
    int partition = 0;
    try {
      timelineMetrics = objectMapper.readValue(value.toString(), TimelineMetrics.class);
      String appId = timelineMetrics.getMetrics().get(0).getAppId();

      List<String> p0 = Arrays.asList("namenode", "applicationhistoryserver", "hivemetastore");
      List<String> p1 = Arrays.asList("hbase", "kafka_broker", "datanode");
      List<String> p2 = Arrays.asList("nimbus", "ams-hbase", "nodemanager");
      List<String> p3 = Arrays.asList("hiveserver2", "resourcemanager", "HOST");

      if (p1.contains(appId)) {
        partition = 1;
      } else if (p2.contains(appId)) {
        partition = 2;
      } else if (p3.contains(appId)) {
        partition = 3;
      } else {
        partition = 0;
      }
      LOG.info("appId=" + appId + ", partition=" + partition);
    } catch (IOException e) {
      LOG.error(e.getMessage());
    }
    return partition;
  }

  @Override
  public void close() {

  }

  @Override
  public void configure(Map<String, ?> map) {

  }
}