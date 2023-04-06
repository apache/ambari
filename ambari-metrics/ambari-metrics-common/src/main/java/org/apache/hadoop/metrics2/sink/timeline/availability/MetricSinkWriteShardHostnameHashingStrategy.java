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
package org.apache.hadoop.metrics2.sink.timeline.availability;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.util.List;

/**
 * Provides sharding based on hostname
 */
public class MetricSinkWriteShardHostnameHashingStrategy implements MetricSinkWriteShardStrategy {
  private final String hostname;
  private final long hostnameHash;
  private static final Log LOG = LogFactory.getLog(MetricSinkWriteShardHostnameHashingStrategy.class);

  public MetricSinkWriteShardHostnameHashingStrategy(String hostname) {
    this.hostname = hostname;
    this.hostnameHash = hostname != null ? computeHash(hostname) : 1000; // some constant
  }

  @Override
  public String findCollectorShard(List<String> collectorHosts) {
    long index = hostnameHash % collectorHosts.size();
    index = index < 0 ? index + collectorHosts.size() : index;
    String collectorHost = collectorHosts.get((int) index);
    LOG.info(String.format("Calculated collector shard %s based on hostname: %s", collectorHost, hostname));
    return collectorHost;
  }

  /**
   * Compute consistent hash based on hostname which should give decently
   * uniform distribution assuming hostname generally have a sequential
   * numeric suffix.
   */
  long computeHash(String hostname) {
    long h = 11987L; // prime
    int len = hostname.length();

    for (int i = 0; i < len; i++) {
      h = 31 * h + hostname.charAt(i);
    }
    return h;
  }
}
