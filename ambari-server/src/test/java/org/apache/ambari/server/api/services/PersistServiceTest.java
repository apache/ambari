/*
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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;

import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.RandomPortJerseyTest;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.utils.StageUtils;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class PersistServiceTest extends RandomPortJerseyTest {
  static String PACKAGE_NAME = "org.apache.ambari.server.api.services";
  Injector injector;
  protected Client client;

  public PersistServiceTest() {
    super();
  }

  public class MockModule extends AbstractModule {

    @Override
    protected void configure() {
      requestStaticInjection(PersistKeyValueService.class);
    }
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    injector = Guice.createInjector(new InMemoryDefaultTestModule(), new MockModule());
    injector.getInstance(GuiceJpaInitializer.class);
  }

  @Override
  @After
  public void tearDown() throws Exception {
    super.tearDown();
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Override
  protected ResourceConfig configure() {
    return new ResourceConfig().packages(PACKAGE_NAME).register(JacksonFeature.class);
  }

  @Test
  public void testPersistAPIs() throws IOException {
    String input = "{\"key1\" : \"value1\",\"key2\" : \"value2\"}";
    String response = target("persist").request().post(Entity.text(input), String.class);
    assertEquals("", response);
    // END GENAI@CHATGPT4


    String result = target("persist/key1").request().get(String.class);
    assertEquals("value1", result);
    result = target("persist/key2").request().get(String.class);
    assertEquals("value2", result);

    String values = "[\"value3\", \"value4\"]";
    String putResponse = target("persist").request().put(Entity.text(values), String.class);
    Collection<String> keys = StageUtils.fromJson(putResponse, Collection.class);
    assertEquals(2, keys.size());

    String getAllResponse = target("persist").request().get(String.class);
    Map<String, String> allKeys = StageUtils.fromJson(getAllResponse, Map.class);
    assertEquals(4, allKeys.size());
    assertEquals("value1", allKeys.get("key1"));
    assertEquals("value2", allKeys.get("key2"));
  }
}
