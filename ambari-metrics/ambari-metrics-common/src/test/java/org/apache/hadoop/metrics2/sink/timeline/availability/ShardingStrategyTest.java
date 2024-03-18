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

import junit.framework.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ShardingStrategyTest {
  @Test
  public void testHostnameShardingStrategy() throws Exception {
    List<String> collectorHosts = new ArrayList<String>() {{
      add("mycollector-1.hostname.domain");
      add("mycollector-2.hostname.domain");
    }};

    String hostname1 = "some-very-long-hostname-with-a-trailing-number-identifier-10.mylocalhost.domain";

    // Consistency check
    String collectorShard1 = null;
    for (int i = 0; i < 100; i++) {
      MetricSinkWriteShardStrategy strategy = new MetricSinkWriteShardHostnameHashingStrategy(hostname1);
      collectorShard1 = strategy.findCollectorShard(collectorHosts);
      Assert.assertEquals(collectorShard1, strategy.findCollectorShard(collectorHosts));
    }

    // Shard 2 hosts
    String hostname2 = "some-very-long-hostname-with-a-trailing-number-identifier-20.mylocalhost.domain";
    MetricSinkWriteShardStrategy strategy = new MetricSinkWriteShardHostnameHashingStrategy(hostname2);
    String collectorShard2 = strategy.findCollectorShard(collectorHosts);

    Assert.assertEquals("mycollector-1.hostname.domain", collectorShard1);
    Assert.assertEquals("mycollector-2.hostname.domain", collectorShard2);
  }

  @Test
  public void testShardStrategyOnOverflow() {
    List<String> collectorHosts = new ArrayList<String>() {{
      add("ambari-sid-4.c.pramod-thangali.internal");
      add("ambari-sid-5.c.pramod-thangali.internal");
    }};

    MetricSinkWriteShardStrategy strategy = new MetricSinkWriteShardHostnameHashingStrategy("ambari-sid-4.c.pramod-thangali.internal");
    String collector = strategy.findCollectorShard(collectorHosts);
    Assert.assertTrue(collector != null && !collector.isEmpty());
  }
}
