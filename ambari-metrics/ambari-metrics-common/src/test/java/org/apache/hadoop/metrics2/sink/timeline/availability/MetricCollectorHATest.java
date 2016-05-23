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

import com.google.gson.Gson;
import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.metrics2.sink.timeline.AbstractTimelineMetricsSink;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.createNiceMock;
import static org.powermock.api.easymock.PowerMock.expectNew;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AbstractTimelineMetricsSink.class, URL.class, HttpURLConnection.class})
public class MetricCollectorHATest {

  @Test
  public void findCollectorUsingZKTest() throws Exception {
    InputStream is = createNiceMock(InputStream.class);
    HttpURLConnection connection = createNiceMock(HttpURLConnection.class);
    URL url = createNiceMock(URL.class);
    MetricCollectorHAHelper haHelper = createNiceMock(MetricCollectorHAHelper.class);

    expectNew(URL.class, "http://localhost:2181/ws/v1/timeline/metrics/livenodes").andReturn(url).anyTimes();
    expect(url.openConnection()).andReturn(connection).anyTimes();
    expect(connection.getInputStream()).andReturn(is).anyTimes();
    expect(connection.getResponseCode()).andThrow(new IOException()).anyTimes();
    expect(haHelper.findLiveCollectorHostsFromZNode()).andReturn(
      new ArrayList<String>() {{ add("h2"); add("h3"); }});

    replayAll();
    TestTimelineMetricsSink sink = new TestTimelineMetricsSink(haHelper);
    sink.init();

    String host = sink.findPreferredCollectHost();

    verifyAll();

    Assert.assertNotNull(host);
    Assert.assertEquals("h2", host);

  }

  @Test
  public void findCollectorUsingKnownCollectorTest() throws Exception {
    HttpURLConnection connection = createNiceMock(HttpURLConnection.class);
    URL url = createNiceMock(URL.class);
    MetricCollectorHAHelper haHelper = createNiceMock(MetricCollectorHAHelper.class);

    Gson gson = new Gson();
    ArrayList<String> output = new ArrayList<>();
    output.add("h1");
    output.add("h2");
    output.add("h3");
    InputStream is = IOUtils.toInputStream(gson.toJson(output));

    expectNew(URL.class, "http://localhost:2181/ws/v1/timeline/metrics/livenodes").andReturn(url).anyTimes();
    expect(url.openConnection()).andReturn(connection).anyTimes();
    expect(connection.getInputStream()).andReturn(is).anyTimes();
    expect(connection.getResponseCode()).andReturn(200).anyTimes();

    replayAll();
    TestTimelineMetricsSink sink = new TestTimelineMetricsSink(haHelper);
    sink.init();

    String host = sink.findPreferredCollectHost();
    Assert.assertNotNull(host);
    Assert.assertEquals("h3", host);

    verifyAll();
  }

  private class TestTimelineMetricsSink extends AbstractTimelineMetricsSink {
    MetricCollectorHAHelper testHelper;

    TestTimelineMetricsSink(MetricCollectorHAHelper haHelper) {
      testHelper = haHelper;
    }

    @Override
    protected void init() {
      super.init();
      this.collectorHAHelper = testHelper;
    }

    @Override
    protected synchronized String findPreferredCollectHost() {
      return super.findPreferredCollectHost();
    }

    @Override
    protected String getCollectorUri(String host) {
      return null;
    }

    @Override
    protected String getCollectorProtocol() {
      return "http";
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
    protected String getConfiguredCollectors() {
      return "localhost:2181";
    }

    @Override
    protected String getHostname() {
      return "h1";
    }
  }
}
