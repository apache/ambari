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

import org.apache.hadoop.metrics2.sink.timeline.AbstractTimelineMetricsSink;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.metrics2.sink.timeline.UnableToConnectException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.createNiceMock;
import static org.powermock.api.easymock.PowerMock.expectNew;
import static org.powermock.api.easymock.PowerMock.replayAll;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AbstractTimelineMetricsSink.class, URL.class,
  HttpURLConnection.class})
public class HandleConnectExceptionTest {
  private static final String COLLECTOR_URL = "collector";
  private TestTimelineMetricsSink sink;
  
  @Before
  public void init(){
    sink = new TestTimelineMetricsSink();
    OutputStream os = createNiceMock(OutputStream.class);
    HttpURLConnection connection = createNiceMock(HttpURLConnection.class);
    URL url = createNiceMock(URL.class);

    try {
      expectNew(URL.class, "collector").andReturn(url);
      expect(url.openConnection()).andReturn(connection).once();
      expect(connection.getOutputStream()).andReturn(os).once();
      expect(connection.getResponseCode()).andThrow(new IOException());

      replayAll();
    } catch (Exception e) {
      //no-op
    }
  }

  @Test
  public void handleTest(){
    try{
      sink.emitMetrics(new TimelineMetrics());
      Assert.fail();
    }catch(UnableToConnectException e){
      Assert.assertEquals(COLLECTOR_URL, e.getConnectUrl());
    }catch(Exception e){
      Assert.fail(e.getMessage());
    }
  }

  class TestTimelineMetricsSink extends AbstractTimelineMetricsSink{
    @Override
    protected String getCollectorUri() {
      return COLLECTOR_URL;
    }

    @Override
    protected int getTimeoutSeconds() {
      return 10;
    }

    @Override
    public void emitMetrics(TimelineMetrics metrics) {
      super.emitMetrics(metrics);
    }
  }
}
