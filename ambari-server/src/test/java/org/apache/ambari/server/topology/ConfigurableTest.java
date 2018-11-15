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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;

public class ConfigurableTest {
  public static final String JSON_LOCATION = "add_service_api/configurable.json";
  public static final String JSON_LOCATION2 = "add_service_api/configurable2.json";
  public static final String INVALID_CONFIGS_LOCATION = "add_service_api/invalid_configurables.txt";

  private TestConfigurable configurable;
  private ObjectMapper mapper;

  Logger LOG = LoggerFactory.getLogger(ConfigurableTest.class);

  @Before
  public void setUp() throws Exception {
    mapper = new ObjectMapper();
    URL url = Resources.getResource(JSON_LOCATION);
    configurable = mapper.readValue(url, TestConfigurable.class);
  }

  /**
   * Parse normal JSON configuration
   */
  @Test
  public void testParseConfigurable() throws Exception {
    assertEquals(ImmutableMap.of("zoo.cfg", ImmutableMap.of("dataDir", "/zookeeper1")),
      configurable.getConfiguration().getProperties());
    assertEquals(
      ImmutableMap.of("zoo.cfg",
        ImmutableMap.of("final",
          ImmutableMap.of("someProp", "true"))),
      configurable.getConfiguration().getAttributes());
  }

  @Test
  public void parseInvalidConfigurables() throws Exception {
    String invalidConfigsTxt = Resources.toString(Resources.getResource(INVALID_CONFIGS_LOCATION), StandardCharsets.UTF_8);
    List<String> invalidConfigs = Splitter
      // filter block comment (Apache license) and use line comments as separators between json snippets
      .on(Pattern.compile("'''(.|[\\r\\n])*?'''|#.*$", Pattern.MULTILINE))
      .omitEmptyStrings()
      .trimResults()
      .splitToList(invalidConfigsTxt);

    for (String config: invalidConfigs) {
      LOG.info("Invalid config to parse:\n{}", config);
      try {
        mapper.readValue(config, TestConfigurable.class);
        fail("Expected " + JsonProcessingException.class.getSimpleName() + " while processing config:\n" + config);
      }
      catch (JsonProcessingException ex) {
        Throwable rootCause  = ExceptionUtils.getRootCause(ex);
        LOG.info("Error message: {}", rootCause.getMessage());
        assertTrue(
          "Expected " + IllegalArgumentException.class.getSimpleName() + " during parsing JSON:\n" + config +
            "\n found: " + rootCause,
          rootCause instanceof IllegalArgumentException);
      }
    }

  }

  /**
   * Deserialize normal JSON configuration
   */
  @Test
  public void testSerializaDeserialize() throws Exception {
    String persisted = mapper.writeValueAsString(configurable);
    Configurable restored = mapper.readValue(persisted, TestConfigurable.class);
    assertEquals(configurable.getConfiguration().getProperties(), restored.getConfiguration().getProperties());
    assertEquals(configurable.getConfiguration().getAttributes(), restored.getConfiguration().getAttributes());
  }

  /**
   * Parse flattened configuration
   */
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
          ImmutableMap.of("someProp", "true"))),
      configurable.getConfiguration().getAttributes());
  }

  static class TestConfigurable implements Configurable {
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
}
