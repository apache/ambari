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
package org.apache.hadoop.metrics2.sink.timeline;

import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.TreeMap;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.expectNew;
import static org.powermock.api.easymock.PowerMock.replayAll;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AbstractTimelineMetricsSink.class, HttpURLConnection.class})
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

  @Test
  @PrepareForTest({URL.class, OutputStream.class, AbstractTimelineMetricsSink.class, HttpURLConnection.class, TimelineMetric.class})
  public void testEmitMetrics() throws Exception {
    HttpURLConnection connection = PowerMock.createNiceMock(HttpURLConnection.class);
    URL url = PowerMock.createNiceMock(URL.class);
    expectNew(URL.class, anyString()).andReturn(url).anyTimes();
    expect(url.openConnection()).andReturn(connection).anyTimes();
    expect(connection.getResponseCode()).andReturn(200).anyTimes();
    OutputStream os = PowerMock.createNiceMock(OutputStream.class);
    expect(connection.getOutputStream()).andReturn(os).anyTimes();


    TestTimelineMetricsSink sink = new TestTimelineMetricsSink();
    TimelineMetrics timelineMetrics = new TimelineMetrics();
    long startTime = System.currentTimeMillis() / 60000 * 60000;

    long seconds = 1000;
    TreeMap<Long, Double> metricValues = new TreeMap<>();
    /*

    0        +30s      +60s
    |         |         |
      (1)(2)(3) (4)(5)   (6)  m1

    */
    // (6) should be cached, the rest - posted

    metricValues.put(startTime + 4*seconds, 1.0);
    metricValues.put(startTime + 14*seconds, 2.0);
    metricValues.put(startTime + 24*seconds, 3.0);
    metricValues.put(startTime + 34*seconds, 4.0);
    metricValues.put(startTime + 44*seconds, 5.0);
    metricValues.put(startTime + 64*seconds, 6.0);

    TimelineMetric timelineMetric = new TimelineMetric("metric1", "host1", "app1", "instance1");
    timelineMetric.setStartTime(metricValues.firstKey());
    timelineMetric.addMetricValues(metricValues);

    timelineMetrics.addOrMergeTimelineMetric(timelineMetric);

    replayAll();
    sink.emitMetrics(timelineMetrics);
    Assert.assertEquals(1, sink.getMetricsPostCache().size());
    metricValues = new TreeMap<>();
    metricValues.put(startTime + 64*seconds, 6.0);
    Assert.assertEquals(metricValues, sink.getMetricsPostCache().getIfPresent("metric1").getMetricValues());

    timelineMetrics = new TimelineMetrics();
    metricValues = new TreeMap<>();
    /*

    +60      +90s     +120s     +150s     +180s
    |         |         |         |         |
       (7)      (8)       (9)           (10)   (11)   m1

    */
    // (6) from previous post should be merged with current data
    // (6),(7),(8),(9),(10) - should be posted, (11) - cached
    metricValues.put(startTime + 74*seconds, 7.0);
    metricValues.put(startTime + 94*seconds, 8.0);
    metricValues.put(startTime + 124*seconds, 9.0);
    metricValues.put(startTime + 154*seconds, 10.0);
    metricValues.put(startTime + 184*seconds, 11.0);

    timelineMetric = new TimelineMetric("metric1", "host1", "app1", "instance1");
    timelineMetric.setStartTime(metricValues.firstKey());
    timelineMetric.addMetricValues(metricValues);

    timelineMetrics.addOrMergeTimelineMetric(timelineMetric);
    sink.emitMetrics(timelineMetrics);

    Assert.assertEquals(1, sink.getMetricsPostCache().size());
    metricValues = new TreeMap<>();
    metricValues.put(startTime + 184*seconds, 11.0);
    Assert.assertEquals(metricValues, sink.getMetricsPostCache().getIfPresent("metric1").getMetricValues());timelineMetrics = new TimelineMetrics();

    metricValues = new TreeMap<>();
    /*

    +180s   +210s   +240s
    |         |       |
       (12)        (13)

    */
    // (11) from previous post should be merged with current data
    // (11),(12),(13) - should be posted, cache should be empty
    metricValues.put(startTime + 194*seconds, 12.0);
    metricValues.put(startTime + 239*seconds, 13.0);

    timelineMetric = new TimelineMetric("metric1", "host1", "app1", "instance1");
    timelineMetric.setStartTime(metricValues.firstKey());
    timelineMetric.addMetricValues(metricValues);

    timelineMetrics.addOrMergeTimelineMetric(timelineMetric);
    sink.emitMetrics(timelineMetrics);

    Assert.assertEquals(0, sink.getMetricsPostCache().size());

    metricValues = new TreeMap<>();
    /*

    +240s   +270s   +300s   +330s
    |         |       |       |
       (14)        (15)   (16)

    */
    // since postAllCachedMetrics in emitMetrics call is true (14),(15),(16) - should be posted, cache should be empty
    metricValues.put(startTime + 245*seconds, 14.0);
    metricValues.put(startTime + 294*seconds, 15.0);
    metricValues.put(startTime + 315*seconds, 16.0);

    timelineMetric = new TimelineMetric("metric1", "host1", "app1", "instance1");
    timelineMetric.setStartTime(metricValues.firstKey());
    timelineMetric.addMetricValues(metricValues);

    timelineMetrics.addOrMergeTimelineMetric(timelineMetric);
    sink.emitMetrics(timelineMetrics, true);

    Assert.assertEquals(0, sink.getMetricsPostCache().size());
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
    protected String getHostInMemoryAggregationProtocol() {
      return "http";
    }
  }
}
