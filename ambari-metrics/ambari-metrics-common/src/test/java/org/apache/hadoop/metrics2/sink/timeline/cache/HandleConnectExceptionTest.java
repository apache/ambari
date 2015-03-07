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
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.hadoop.metrics2.sink.timeline.AbstractTimelineMetricsSink;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.metrics2.sink.timeline.UnableToConnectException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HandleConnectExceptionTest {
  private static final String COLLECTOR_URL = "collector";
  @Mock private HttpClient client;
  private TestTimelineMetricsSink sink;
  
  @Before public void init(){
    sink = new TestTimelineMetricsSink();
    sink.setHttpClient(client);
    
    try {
      Mockito.when(client.executeMethod(Mockito.<HttpMethod>any())).thenThrow(new ConnectException());
    } catch (IOException e) {
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
    public void emitMetrics(TimelineMetrics metrics) throws IOException {
      super.emitMetrics(metrics);
    }
  }
}
