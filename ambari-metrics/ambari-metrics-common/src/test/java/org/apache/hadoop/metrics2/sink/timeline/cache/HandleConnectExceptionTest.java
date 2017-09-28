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
package org.apache.hadoop.metrics2.sink.timeline.cache;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import org.apache.hadoop.metrics2.sink.timeline.AbstractTimelineMetricsSink;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.metrics2.sink.timeline.UnableToConnectException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.createNiceMock;
import static org.powermock.api.easymock.PowerMock.expectNew;
import static org.powermock.api.easymock.PowerMock.replayAll;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AbstractTimelineMetricsSink.class, URL.class, HttpURLConnection.class})
public class HandleConnectExceptionTest {
  private static final String COLLECTOR_URL = "collector";
  private TestTimelineMetricsSink sink;

  @Before
  public void init(){
    sink = new TestTimelineMetricsSink();
    OutputStream os = createNiceMock(OutputStream.class);
    HttpURLConnection connection = createNiceMock(HttpURLConnection.class);
    URL url = createNiceMock(URL.class);
    AbstractTimelineMetricsSink.NUMBER_OF_SKIPPED_COLLECTOR_EXCEPTIONS = 2;
    try {
      expectNew(URL.class, anyString()).andReturn(url).anyTimes();
      expect(url.openConnection()).andReturn(connection).anyTimes();
      expect(connection.getOutputStream()).andReturn(os).anyTimes();
      expect(connection.getResponseCode()).andThrow(new IOException()).anyTimes();

      replayAll();
    } catch (Exception e) {
      //no-op
    }
  }

  @Test
  public void handleTest(){
    emitMetricsWithExpectedException(new TimelineMetrics());
    try {
      sink.emitMetrics(new TimelineMetrics());
    } catch (Exception e) {
      Assert.fail("There should be no exception");
    }
    emitMetricsWithExpectedException(new TimelineMetrics());
  }

  private void emitMetricsWithExpectedException(TimelineMetrics timelineMetrics) {
    try{
      sink.emitMetrics(timelineMetrics);
      Assert.fail();
    } catch (UnableToConnectException e){
      Assert.assertEquals(COLLECTOR_URL, e.getConnectUrl());
    } catch (Exception e){
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testEmitMetricsWithNullHost() {
    TestTimelineMetricsSinkWithNullHost sinkWithNullHost = new TestTimelineMetricsSinkWithNullHost();

    boolean success = sinkWithNullHost.emitMetrics(new TimelineMetrics());
    Assert.assertFalse(success);

    success = sinkWithNullHost.emitMetrics(new TimelineMetrics());
    Assert.assertTrue(success);
  }

  private class TestTimelineMetricsSink extends AbstractTimelineMetricsSink{
    @Override
    protected String getCollectorUri(String host) {
      return COLLECTOR_URL;
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
      return false;
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

    @Override
    protected synchronized String findPreferredCollectHost() {
      return "localhost";
    }

  }

  private class TestTimelineMetricsSinkWithNullHost extends AbstractTimelineMetricsSink {

    int ctr = 0;

    @Override
    protected String getCollectorUri(String host) {
      return COLLECTOR_URL;
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
      return false;
    }

    @Override
    protected int getHostInMemoryAggregationPort() {
      return 0;
    }

    @Override
    public boolean emitMetrics(TimelineMetrics metrics) {
      super.init();
      return super.emitMetrics(metrics);
    }

    @Override
    protected synchronized String findPreferredCollectHost() {
      if (ctr == 0) {
        ctr++;
        return null;
      } else {
        return "localhost";
      }
    }

    @Override
    protected boolean emitMetricsJson(String connectUrl, String jsonData) {
      return true;
    }

  }

}
