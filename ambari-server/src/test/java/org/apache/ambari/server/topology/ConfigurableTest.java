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

package org.apache.ambari.server.topology;

import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;

public class ConfigurableTest {
  public static final String JSON_LOCATION = "blueprint3.0/configurable.json";
  public static final String JSON_LOCATION2 = "blueprint3.0/configurable2.json";

  private TestConfigurable configurable;
  private ObjectMapper mapper;

  @Before
  public void setUp() throws Exception {
    mapper = new ObjectMapper();
    URL url = Resources.getResource(JSON_LOCATION);
    configurable = new ObjectMapper().readValue(url, TestConfigurable.class);
  }

  @Test
  public void testParseConfigurable() throws Exception{
    assertEquals(ImmutableMap.of("zoo.cfg", ImmutableMap.of("dataDir", "/zookeeper1")),
      configurable.getConfiguration().getProperties());
    assertEquals(
      ImmutableMap.of("zoo.cfg",
        ImmutableMap.of("final",
          ImmutableMap.of("someProp", "someValue"))),
      configurable.getConfiguration().getAttributes());
  }

  @Test
  public void testSerializaDeserialize() throws Exception {
    String persisted = mapper.writeValueAsString(configurable);
    Configurable restored = mapper.readValue(persisted, TestConfigurable.class);
    assertEquals(configurable.getConfiguration().getProperties(), restored.getConfiguration().getProperties());
    assertEquals(configurable.getConfiguration().getAttributes(), restored.getConfiguration().getAttributes());
  }

  @Test
  public void testParseConfigurableFromResoueceManager() throws Exception{
    mapper = new ObjectMapper();
    URL url = Resources.getResource(JSON_LOCATION2);
    configurable = new ObjectMapper().readValue(url, TestConfigurable.class);

    assertEquals(ImmutableMap.of("zoo.cfg", ImmutableMap.of("dataDir", "/zookeeper1")),
      configurable.getConfiguration().getProperties());
    assertEquals(
      ImmutableMap.of("zoo.cfg",
        ImmutableMap.of("final",
          ImmutableMap.of("someProp", "someValue"))),
      configurable.getConfiguration().getAttributes());
  }


}

class TestConfigurable implements Configurable {
  Configuration configuration;

  @Override
  public Configuration getConfiguration() {
    return configuration;
  }

  @Override
  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }


}