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

package org.apache.ambari.server.api.services;

import java.io.IOException;
import java.util.Map;

import junit.framework.Assert;

import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jettison.json.JSONException;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;

public class PersistServiceTest extends JerseyTest {
  static String PACKAGE_NAME = "org.apache.ambari.server.api.services";
  private static Log LOG = LogFactory.getLog(PersistServiceTest.class);
  Injector injector;
  protected Client client;

  public  PersistServiceTest() {
    super(new WebAppDescriptor.Builder(PACKAGE_NAME).servletClass(ServletContainer.class)
        .initParam("com.sun.jersey.api.json.POJOMappingFeature", "true")
        .build());
  }

  public class MockModule extends AbstractModule {


    @Override
    protected void configure() {
      requestStaticInjection(PersistKeyValueService.class);
    }
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    PersistKeyValueImpl impl = new PersistKeyValueImpl();
    injector = Guice.createInjector(new MockModule());
    injector.injectMembers(impl);
  }

  @Test
  public void testPersist() throws UniformInterfaceException, JSONException,
    IOException {
    ClientConfig clientConfig = new DefaultClientConfig();
    clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    client = Client.create(clientConfig);
    WebResource webResource = client.resource("http://localhost:9998/persist");
    
    webResource.post("{\"xyx\" : \"t\"}");
    LOG.info("Done posting to the server");
    String output = webResource.get(String.class);
    LOG.info("All key values " + output);
    Map<String, String> jsonOutput = StageUtils.fromJson(output, Map.class);
    String value = jsonOutput.get("xyx");
    Assert.assertEquals("t", value);
    webResource = client.resource("http://localhost:9998/persist/xyx");
    output = webResource.get(String.class);
    Assert.assertEquals("t", output);
    LOG.info("Value for xyx " + output);
  }
}
