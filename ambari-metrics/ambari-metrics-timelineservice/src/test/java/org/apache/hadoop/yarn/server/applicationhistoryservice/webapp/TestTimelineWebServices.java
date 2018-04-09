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

package org.apache.hadoop.yarn.server.applicationhistoryservice.webapp;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.MediaType;

import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TestTimelineMetricStore;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricStore;
import org.apache.hadoop.yarn.webapp.GenericExceptionHandler;
import org.apache.hadoop.yarn.webapp.YarnJacksonJaxbJsonProvider;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;

import junit.framework.Assert;


public class TestTimelineWebServices extends JerseyTest {
  private static TimelineMetricStore metricStore;
  private long beforeTime;

  private Injector injector = Guice.createInjector(new ServletModule() {

    @Override
    protected void configureServlets() {
      bind(YarnJacksonJaxbJsonProvider.class);
      bind(TimelineWebServices.class);
      bind(GenericExceptionHandler.class);
      try {
        metricStore = new TestTimelineMetricStore();
      } catch (Exception e) {
        Assert.fail();
      }
      bind(TimelineMetricStore.class).toInstance(metricStore);
      serve("/*").with(GuiceContainer.class);
    }

  });

  public class GuiceServletConfig extends GuiceServletContextListener {

    @Override
    protected Injector getInjector() {
      return injector;
    }
  }

  public TestTimelineWebServices() {
    super(new WebAppDescriptor.Builder(
      "org.apache.hadoop.yarn.server.applicationhistoryservice.webapp")
      .contextListenerClass(GuiceServletConfig.class)
      .filterClass(com.google.inject.servlet.GuiceFilter.class)
      .contextPath("jersey-guice-filter")
      .servletPath("/")
      .clientConfig(new DefaultClientConfig(YarnJacksonJaxbJsonProvider.class))
      .build());
  }

  @Test
  public void testAbout() throws Exception {
    WebResource r = resource();
    ClientResponse response = r.path("ws").path("v1").path("timeline")
      .accept(MediaType.APPLICATION_JSON)
      .get(ClientResponse.class);
    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getType());
    TimelineWebServices.AboutInfo about =
      response.getEntity(TimelineWebServices.AboutInfo.class);
    Assert.assertNotNull(about);
    Assert.assertEquals("AMS API", about.getAbout());
  }
  
  private static void verifyMetrics(TimelineMetrics metrics) {
    Assert.assertNotNull(metrics);
    Assert.assertEquals("cpu_user", metrics.getMetrics().get(0).getMetricName());
    Assert.assertEquals(3, metrics.getMetrics().get(0).getMetricValues().size());
    Assert.assertEquals("mem_free", metrics.getMetrics().get(1).getMetricName());
    Assert.assertEquals(3, metrics.getMetrics().get(1).getMetricValues().size());
  }

  @Test
  public void testGetMetrics() throws Exception {
    WebResource r = resource();
    ClientResponse response = r.path("ws").path("v1").path("timeline")
      .path("metrics").queryParam("metricNames", "cpu_user").queryParam("precision", "seconds")
      .accept(MediaType.APPLICATION_JSON)
      .get(ClientResponse.class);
    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getType());
    verifyMetrics(response.getEntity(TimelineMetrics.class));
  }
}
