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
package org.apache.hadoop.metrics2.sink.timeline.availability;

import junit.framework.Assert;
import org.apache.hadoop.metrics2.sink.timeline.AbstractTimelineMetricsSink;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

public class AbstractTimelineMetricSinkTest {

  @Test
  public void testParseHostsStringIntoCollection() {
    AbstractTimelineMetricsSink sink = new TestTimelineMetricsSink();
    Collection<String> hosts;

    hosts = sink.parseHostsStringIntoCollection("");
    Assert.assertTrue(hosts.isEmpty());

    hosts = sink.parseHostsStringIntoCollection("test1.123.abc.def.local");
    Assert.assertTrue(hosts.size() == 1);
    Assert.assertTrue(hosts.contains("test1.123.abc.def.local"));

    hosts = sink.parseHostsStringIntoCollection("test1.123.abc.def.local ");
    Assert.assertTrue(hosts.size() == 1);
    Assert.assertTrue(hosts.contains("test1.123.abc.def.local"));

    hosts = sink.parseHostsStringIntoCollection("test1.123.abc.def.local,test1.456.abc.def.local");
    Assert.assertTrue(hosts.size() == 2);

    hosts = sink.parseHostsStringIntoCollection("test1.123.abc.def.local, test1.456.abc.def.local");
    Assert.assertTrue(hosts.size() == 2);
    Assert.assertTrue(hosts.contains("test1.123.abc.def.local"));
    Assert.assertTrue(hosts.contains("test1.456.abc.def.local"));

  }

  private class TestTimelineMetricsSink extends AbstractTimelineMetricsSink {
    @Override
    protected String getCollectorUri(String host) {
      return "";
    }

    @Override
    protected String getCollectorProtocol() {
      return "http";
    }

    @Override
    protected String getCollectorPort() {
      return "2181";
    }

    @Override
    protected int getTimeoutSeconds() {
      return 10;
    }

    @Override
    protected String getZookeeperQuorum() {
      return "localhost:2181";
    }

    @Override
    protected Collection<String> getConfiguredCollectorHosts() {
      return Arrays.asList("localhost");
    }

    @Override
    protected String getHostname() {
      return "h1";
    }

    @Override
    protected boolean isHostInMemoryAggregationEnabled() {
      return true;
    }

    @Override
    protected int getHostInMemoryAggregationPort() {
      return 61888;
    }

    @Override
    public boolean emitMetrics(TimelineMetrics metrics) {
      super.init();
      return super.emitMetrics(metrics);
    }
  }
}
